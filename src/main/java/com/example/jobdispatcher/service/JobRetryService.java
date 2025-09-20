package com.example.jobdispatcher.service;

import com.example.jobdispatcher.config.RetryConfig;
import com.example.jobdispatcher.entity.ScheduledJob;
import com.example.jobdispatcher.repository.ScheduledJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling job retry logic with exponential backoff.
 */
@Service
@Transactional
public class JobRetryService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobRetryService.class);
    
    @Autowired
    private RetryConfig retryConfig;
    
    @Autowired
    private ScheduledJobRepository scheduledJobRepository;
    
    @Autowired
    private JobDispatcherService jobDispatcherService;
    
    /**
     * Schedule a job for retry with exponential backoff.
     */
    public void scheduleRetry(ScheduledJob job, Throwable exception) {
        if (!retryConfig.isEnabled() || !job.getRetryEnabled()) {
            logger.info("Retry disabled for job: {}", job.getJobId());
            return;
        }
        
        if (!retryConfig.isRetryableException(exception)) {
            logger.info("Exception {} is not retryable for job: {}", 
                       exception.getClass().getSimpleName(), job.getJobId());
            return;
        }
        
        if (!job.canRetry()) {
            logger.info("Job {} cannot be retried (attempts: {}/{})", 
                       job.getJobId(), job.getRetryCount(), job.getMaxRetryAttempts());
            return;
        }
        
        // Calculate retry delay with exponential backoff
        long delayMillis = calculateRetryDelay(job);
        
        // Update job with retry information
        job.incrementRetryCount();
        job.scheduleNextRetry(delayMillis);
        job.setRetryReason(exception.getMessage());
        
        scheduledJobRepository.save(job);
        
        logger.info("Scheduled retry for job {} in {}ms (attempt {}/{})", 
                   job.getJobId(), delayMillis, job.getRetryCount(), job.getMaxRetryAttempts());
    }
    
    /**
     * Execute a job retry asynchronously.
     */
    @Async
    public CompletableFuture<Void> executeRetry(String jobId) {
        try {
            ScheduledJob job = scheduledJobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
            
            if (!job.isRetryDue()) {
                logger.warn("Retry not due yet for job: {}", jobId);
                return CompletableFuture.completedFuture(null);
            }
            
            logger.info("Executing retry for job: {} (attempt {}/{})", 
                       jobId, job.getRetryCount(), job.getMaxRetryAttempts());
            
            // Clear retry schedule
            job.setNextRetryAt(null);
            scheduledJobRepository.save(job);
            
            // Re-dispatch the job
            if ("ONE_TIME".equals(job.getJobType())) {
                // For one-time jobs, we need to recreate the request and dispatch
                jobDispatcherService.redispatchOneTimeJob(job);
            } else if ("REPETITIVE".equals(job.getJobType())) {
                // For repetitive jobs, restart the scheduling
                jobDispatcherService.redispatchRepetitiveJob(job);
            }
            
        } catch (Exception e) {
            logger.error("Error executing retry for job: {}", jobId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Process all due retries (called by scheduler).
     */
    @Scheduled(fixedDelay = 5000) // Check every 5 seconds
    public void processDueRetries() {
        if (!retryConfig.isEnabled()) {
            return;
        }
        
        try {
            List<ScheduledJob> dueRetries = scheduledJobRepository.findJobsForRetry();
            
            for (ScheduledJob job : dueRetries) {
                if (job.isRetryDue()) {
                    executeRetry(job.getJobId());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing due retries", e);
        }
    }
    
    /**
     * Calculate retry delay with exponential backoff and jitter.
     */
    private long calculateRetryDelay(ScheduledJob job) {
        // Use job-specific settings if available, otherwise use defaults
        long initialDelay = job.getRetryDelayMillis() != null ? 
                           job.getRetryDelayMillis() : 
                           retryConfig.getDefaultInitialDelay();
        
        double multiplier = job.getRetryMultiplier() != null ? 
                           job.getRetryMultiplier() : 
                           retryConfig.getDefaultMultiplier();
        
        long maxDelay = job.getRetryMaxDelayMillis() != null ? 
                       job.getRetryMaxDelayMillis() : 
                       retryConfig.getDefaultMaxDelay();
        
        // Calculate exponential backoff: initialDelay * (multiplier ^ (attempt - 1))
        long delay = (long) (initialDelay * Math.pow(multiplier, job.getRetryCount() - 1));
        
        // Apply maximum delay limit
        delay = Math.min(delay, maxDelay);
        
        // Apply jitter to prevent thundering herd
        double jitter = retryConfig.getDefaultJitter();
        if (jitter > 0) {
            double jitterFactor = 1.0 + (Math.random() - 0.5) * 2 * jitter;
            delay = (long) (delay * jitterFactor);
        }
        
        return Math.max(delay, 0);
    }
    
    /**
     * Get retry statistics.
     */
    public java.util.Map<String, Object> getRetryStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        // Count jobs by retry status
        long totalJobs = scheduledJobRepository.count();
        long failedJobs = scheduledJobRepository.countByStatus("FAILED");
        long retryableJobs = scheduledJobRepository.findJobsForRetry().size();
        long retryScheduledJobs = scheduledJobRepository.findByStatus("PENDING").stream()
                .filter(job -> job.getNextRetryAt() != null)
                .count();
        
        stats.put("totalJobs", totalJobs);
        stats.put("failedJobs", failedJobs);
        stats.put("retryableJobs", retryableJobs);
        stats.put("retryScheduledJobs", retryScheduledJobs);
        stats.put("retryEnabled", retryConfig.isEnabled());
        
        // Calculate retry success rate
        long completedJobs = scheduledJobRepository.countByStatus("COMPLETED");
        long totalAttempts = completedJobs + failedJobs;
        double successRate = totalAttempts > 0 ? (double) completedJobs / totalAttempts * 100 : 0;
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
        
        return stats;
    }
    
    /**
     * Cancel all retries for a specific job.
     */
    public void cancelRetries(String jobId) {
        try {
            ScheduledJob job = scheduledJobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
            
            job.setNextRetryAt(null);
            job.setRetryEnabled(false);
            scheduledJobRepository.save(job);
            
            logger.info("Cancelled retries for job: {}", jobId);
            
        } catch (Exception e) {
            logger.error("Error cancelling retries for job: {}", jobId, e);
        }
    }
    
    /**
     * Reset retry count for a job (useful for manual intervention).
     */
    public void resetRetryCount(String jobId) {
        try {
            ScheduledJob job = scheduledJobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
            
            job.setRetryCount(0);
            job.setNextRetryAt(null);
            job.setLastRetryAt(null);
            job.setRetryReason(null);
            scheduledJobRepository.save(job);
            
            logger.info("Reset retry count for job: {}", jobId);
            
        } catch (Exception e) {
            logger.error("Error resetting retry count for job: {}", jobId, e);
        }
    }
}

