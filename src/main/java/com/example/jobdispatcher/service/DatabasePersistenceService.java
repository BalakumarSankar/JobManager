package com.example.jobdispatcher.service;

import com.example.jobdispatcher.entity.AppServer;
import com.example.jobdispatcher.entity.ScheduledJob;
import com.example.jobdispatcher.entity.ThreadPool;
import com.example.jobdispatcher.repository.AppServerRepository;
import com.example.jobdispatcher.repository.ScheduledJobRepository;
import com.example.jobdispatcher.repository.ThreadPoolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing database persistence operations.
 */
@Service
@Transactional
public class DatabasePersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabasePersistenceService.class);
    
    @Autowired
    private ThreadPoolRepository threadPoolRepository;
    
    @Autowired
    private AppServerRepository appServerRepository;
    
    @Autowired
    private ScheduledJobRepository scheduledJobRepository;
    
    // ThreadPool Operations
    
    public ThreadPool saveThreadPool(ThreadPool threadPool) {
        logger.info("Saving thread pool: {}", threadPool.getName());
        return threadPoolRepository.save(threadPool);
    }
    
    public Optional<ThreadPool> findThreadPoolByName(String name) {
        return threadPoolRepository.findByName(name);
    }
    
    public List<ThreadPool> findAllActiveThreadPools() {
        return threadPoolRepository.findByIsActiveTrue();
    }
    
    public List<ThreadPool> findThreadPoolsByType(String type) {
        return threadPoolRepository.findByTypeAndIsActiveTrue(type);
    }
    
    public boolean threadPoolExists(String name) {
        return threadPoolRepository.existsByName(name);
    }
    
    // AppServer Operations
    
    public AppServer saveAppServer(AppServer appServer) {
        logger.info("Saving app server: {}", appServer.getName());
        return appServerRepository.save(appServer);
    }
    
    public Optional<AppServer> findAppServerByName(String name) {
        return appServerRepository.findByName(name);
    }
    
    public List<AppServer> findAllActiveAppServers() {
        return appServerRepository.findByIsActiveTrue();
    }
    
    public List<AppServer> findAppServersByHealthStatus(String healthStatus) {
        return appServerRepository.findByIsActiveTrueAndHealthStatus(healthStatus);
    }
    
    public Optional<AppServer> findAppServerByHostAndPort(String host, Integer port) {
        return appServerRepository.findByHostAndPort(host, port);
    }
    
    public AppServer findAppServerByAppServerId(String appServerId) {
        return appServerRepository.findByAppServerId(appServerId).orElse(null);
    }
    
    public boolean appServerExists(String name) {
        return appServerRepository.existsByName(name);
    }
    
    public void updateAppServerHealthStatus(Long appServerId, String healthStatus) {
        Optional<AppServer> appServerOpt = appServerRepository.findById(appServerId);
        if (appServerOpt.isPresent()) {
            AppServer appServer = appServerOpt.get();
            appServer.setHealthStatus(healthStatus);
            appServer.setLastHealthCheck(LocalDateTime.now());
            appServerRepository.save(appServer);
            logger.info("Updated health status for app server {}: {}", appServer.getName(), healthStatus);
        }
    }
    
    // ScheduledJob Operations
    
    public ScheduledJob saveScheduledJob(ScheduledJob scheduledJob) {
        logger.info("Saving scheduled job: {}", scheduledJob.getJobId());
        return scheduledJobRepository.save(scheduledJob);
    }
    
    public Optional<ScheduledJob> findScheduledJobByJobId(String jobId) {
        return scheduledJobRepository.findByJobId(jobId);
    }
    
    public List<ScheduledJob> findScheduledJobsByStatus(String status) {
        return scheduledJobRepository.findByStatus(status);
    }
    
    public List<ScheduledJob> findScheduledJobsByJobType(String jobType) {
        return scheduledJobRepository.findByJobType(jobType);
    }
    
    public List<ScheduledJob> findScheduledJobsByGroupKey(String groupKey) {
        return scheduledJobRepository.findByGroupKey(groupKey);
    }
    
    public List<ScheduledJob> findScheduledJobsByThreadPool(Long threadPoolId) {
        return scheduledJobRepository.findByThreadPoolId(threadPoolId);
    }
    
    public List<ScheduledJob> findScheduledJobsByAppServer(Long appServerId) {
        return scheduledJobRepository.findByAppServerId(appServerId);
    }
    
    public List<ScheduledJob> findJobsForRetry() {
        return scheduledJobRepository.findJobsForRetry();
    }
    
    public List<ScheduledJob> findLongRunningJobs(LocalDateTime cutoffTime) {
        return scheduledJobRepository.findLongRunningJobs(cutoffTime);
    }
    
    public Page<ScheduledJob> findScheduledJobsWithPagination(String status, Pageable pageable) {
        return scheduledJobRepository.findByStatus(status, pageable);
    }
    
    public Page<ScheduledJob> findScheduledJobsByStatusAndTypeWithPagination(String status, String jobType, Pageable pageable) {
        return scheduledJobRepository.findByStatusAndJobType(status, jobType, pageable);
    }
    
    public long countScheduledJobsByStatus(String status) {
        return scheduledJobRepository.countByStatus(status);
    }
    
    public long countScheduledJobsByJobType(String jobType) {
        return scheduledJobRepository.countByJobType(jobType);
    }
    
    public long countScheduledJobsByGroupKey(String groupKey) {
        return scheduledJobRepository.countByGroupKey(groupKey);
    }
    
    public boolean scheduledJobExists(String jobId) {
        return scheduledJobRepository.existsByJobId(jobId);
    }
    
    public void deleteScheduledJobsByStatus(String status) {
        logger.info("Deleting scheduled jobs with status: {}", status);
        scheduledJobRepository.deleteByStatus(status);
    }
    
    public void deleteOldScheduledJobs(LocalDateTime cutoffTime) {
        logger.info("Deleting scheduled jobs older than: {}", cutoffTime);
        scheduledJobRepository.deleteBySubmittedAtBefore(cutoffTime);
    }
    
    // Job Status Management
    
    public void markJobAsStarted(String jobId) {
        Optional<ScheduledJob> jobOpt = scheduledJobRepository.findByJobId(jobId);
        if (jobOpt.isPresent()) {
            ScheduledJob job = jobOpt.get();
            job.markAsStarted();
            scheduledJobRepository.save(job);
            logger.info("Marked job {} as started", jobId);
        }
    }
    
    public void markJobAsCompleted(String jobId) {
        Optional<ScheduledJob> jobOpt = scheduledJobRepository.findByJobId(jobId);
        if (jobOpt.isPresent()) {
            ScheduledJob job = jobOpt.get();
            job.markAsCompleted();
            scheduledJobRepository.save(job);
            logger.info("Marked job {} as completed", jobId);
        }
    }
    
    public void markJobAsFailed(String jobId, String errorMessage) {
        Optional<ScheduledJob> jobOpt = scheduledJobRepository.findByJobId(jobId);
        if (jobOpt.isPresent()) {
            ScheduledJob job = jobOpt.get();
            job.markAsFailed(errorMessage);
            scheduledJobRepository.save(job);
            logger.info("Marked job {} as failed: {}", jobId, errorMessage);
        }
    }
    
    public void markJobAsCancelled(String jobId) {
        Optional<ScheduledJob> jobOpt = scheduledJobRepository.findByJobId(jobId);
        if (jobOpt.isPresent()) {
            ScheduledJob job = jobOpt.get();
            job.markAsCancelled();
            scheduledJobRepository.save(job);
            logger.info("Marked job {} as cancelled", jobId);
        }
    }
    
    public void incrementJobRetryCount(String jobId) {
        Optional<ScheduledJob> jobOpt = scheduledJobRepository.findByJobId(jobId);
        if (jobOpt.isPresent()) {
            ScheduledJob job = jobOpt.get();
            job.incrementRetryCount();
            scheduledJobRepository.save(job);
            logger.info("Incremented retry count for job {}: {}", jobId, job.getRetryCount());
        }
    }
    
    // Statistics and Reporting
    
    public java.util.Map<String, Object> getJobStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        stats.put("totalJobs", scheduledJobRepository.count());
        stats.put("pendingJobs", scheduledJobRepository.countByStatus("PENDING"));
        stats.put("runningJobs", scheduledJobRepository.countByStatus("RUNNING"));
        stats.put("completedJobs", scheduledJobRepository.countByStatus("COMPLETED"));
        stats.put("failedJobs", scheduledJobRepository.countByStatus("FAILED"));
        stats.put("cancelledJobs", scheduledJobRepository.countByStatus("CANCELLED"));
        
        stats.put("oneTimeJobs", scheduledJobRepository.countByJobType("ONE_TIME"));
        stats.put("repetitiveJobs", scheduledJobRepository.countByJobType("REPETITIVE"));
        
        stats.put("totalThreadPools", threadPoolRepository.count());
        stats.put("activeThreadPools", threadPoolRepository.findByIsActiveTrue().size());
        
        stats.put("totalAppServers", appServerRepository.count());
        stats.put("activeAppServers", appServerRepository.findByIsActiveTrue().size());
        stats.put("upAppServers", appServerRepository.countActiveByHealthStatus("UP"));
        stats.put("downAppServers", appServerRepository.countActiveByHealthStatus("DOWN"));
        
        return stats;
    }
}
