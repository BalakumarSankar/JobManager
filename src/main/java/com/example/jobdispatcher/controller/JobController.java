package com.example.jobdispatcher.controller;

import com.example.jobdispatcher.model.JobSubmissionResponse;
import com.example.jobdispatcher.model.OneTimeJobRequest;
import com.example.jobdispatcher.model.RepetitiveJobRequest;
import com.example.jobdispatcher.service.JobDispatcherService;
import com.example.jobdispatcher.service.ThreadPoolMonitorService;
import com.example.jobdispatcher.service.JobGroupingService;
import com.example.jobdispatcher.service.DatabasePersistenceService;
import com.example.jobdispatcher.annotation.RateLimited;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for job submission and management.
 */
@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {
    
    private static final Logger logger = LoggerFactory.getLogger(JobController.class);
    
    @Autowired
    private JobDispatcherService jobDispatcherService;
    
    @Autowired
    private ThreadPoolMonitorService threadPoolMonitorService;
    
    @Autowired
    private JobGroupingService jobGroupingService;
    
    @Autowired
    private DatabasePersistenceService databasePersistenceService;
    
    @Autowired
    private com.example.jobdispatcher.service.RateLimitingService rateLimitingService;
    
    @Autowired
    private com.example.jobdispatcher.service.JobRetryService jobRetryService;
    
    @Autowired
    private com.example.jobdispatcher.service.ApiKeyService apiKeyService;
    
    /**
     * Submit a one-time job for execution.
     */
    @PostMapping("/onetime")
    @RateLimited(value = RateLimited.RateLimitType.COMBINED, jobType = "ONE_TIME", 
                 message = "Rate limit exceeded for one-time job submissions. Please try again later.")
    public ResponseEntity<JobSubmissionResponse> submitOneTimeJob(
            @Valid @RequestBody OneTimeJobRequest request,
            HttpServletRequest httpRequest) {
        try {
            String appServerId = (String) httpRequest.getAttribute("appServerId");
            String apiKeyId = (String) httpRequest.getAttribute("apiKeyId");
            
            logger.info("Received one-time job submission request for job ID: {} from app server: {} (API key: {})", 
                       request.getJobId(), appServerId, apiKeyId);
            
            jobDispatcherService.dispatchOneTimeJob(request);
            
            JobSubmissionResponse response = new JobSubmissionResponse(
                request.getJobId(),
                "SUBMITTED",
                "One-time job submitted successfully"
            );
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (Exception e) {
            logger.error("Error submitting one-time job: {}", request.getJobId(), e);
            
            JobSubmissionResponse response = new JobSubmissionResponse(
                request.getJobId(),
                "FAILED",
                "Failed to submit one-time job: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Submit a repetitive job for execution.
     */
    @PostMapping("/repetitive")
    @RateLimited(value = RateLimited.RateLimitType.COMBINED, jobType = "REPETITIVE", 
                 message = "Rate limit exceeded for repetitive job submissions. Please try again later.")
    public ResponseEntity<JobSubmissionResponse> submitRepetitiveJob(
            @Valid @RequestBody RepetitiveJobRequest request,
            HttpServletRequest httpRequest) {
        try {
            String appServerId = (String) httpRequest.getAttribute("appServerId");
            String apiKeyId = (String) httpRequest.getAttribute("apiKeyId");
            
            logger.info("Received repetitive job submission request for job ID: {} from app server: {} (API key: {})", 
                       request.getJobId(), appServerId, apiKeyId);
            
            jobDispatcherService.dispatchRepetitiveJob(request);
            
            JobSubmissionResponse response = new JobSubmissionResponse(
                request.getJobId(),
                "SCHEDULED",
                "Repetitive job scheduled successfully"
            );
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (Exception e) {
            logger.error("Error submitting repetitive job: {}", request.getJobId(), e);
            
            JobSubmissionResponse response = new JobSubmissionResponse(
                request.getJobId(),
                "FAILED",
                "Failed to submit repetitive job: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Cancel a scheduled repetitive job.
     */
    @DeleteMapping("/repetitive/{jobId}")
    public ResponseEntity<JobSubmissionResponse> cancelRepetitiveJob(@PathVariable String jobId) {
        try {
            logger.info("Received request to cancel repetitive job: {}", jobId);
            
            boolean cancelled = jobDispatcherService.cancelRepetitiveJob(jobId);
            
            JobSubmissionResponse response = new JobSubmissionResponse(
                jobId,
                cancelled ? "CANCELLED" : "NOT_FOUND",
                cancelled ? "Job cancelled successfully" : "Job not found or already completed"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error cancelling repetitive job: {}", jobId, e);
            
            JobSubmissionResponse response = new JobSubmissionResponse(
                jobId,
                "ERROR",
                "Failed to cancel job: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Check the status of a repetitive job.
     */
    @GetMapping("/repetitive/{jobId}/status")
    public ResponseEntity<JobSubmissionResponse> getJobStatus(@PathVariable String jobId) {
        try {
            boolean isScheduled = jobDispatcherService.isJobScheduled(jobId);
            
            JobSubmissionResponse response = new JobSubmissionResponse(
                jobId,
                isScheduled ? "SCHEDULED" : "NOT_SCHEDULED",
                isScheduled ? "Job is currently scheduled" : "Job is not scheduled or completed"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking job status: {}", jobId, e);
            
            JobSubmissionResponse response = new JobSubmissionResponse(
                jobId,
                "ERROR",
                "Failed to check job status: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get statistics about active scheduled tasks.
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getJobStats() {
        try {
            int activeTasks = jobDispatcherService.getActiveScheduledTasksCount();
            
            return ResponseEntity.ok(new Object() {
                public final int activeScheduledTasks = activeTasks;
                public final long timestamp = System.currentTimeMillis();
            });
            
        } catch (Exception e) {
            logger.error("Error getting job statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving job statistics: " + e.getMessage());
        }
    }
    
    /**
     * Get detailed thread pool statistics.
     */
    @GetMapping("/thread-pool-stats")
    public ResponseEntity<Object> getThreadPoolStats() {
        try {
            return ResponseEntity.ok(threadPoolMonitorService.getThreadPoolStats());
        } catch (Exception e) {
            logger.error("Error getting thread pool statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving thread pool statistics: " + e.getMessage());
        }
    }
    
    /**
     * Get queue utilization information.
     */
    @GetMapping("/queue-utilization")
    public ResponseEntity<Object> getQueueUtilization() {
        try {
            return ResponseEntity.ok(threadPoolMonitorService.getQueueUtilization());
        } catch (Exception e) {
            logger.error("Error getting queue utilization", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving queue utilization: " + e.getMessage());
        }
    }
    
    /**
     * Get job grouping statistics.
     */
    @GetMapping("/grouping-stats")
    public ResponseEntity<Object> getGroupingStats() {
        try {
            return ResponseEntity.ok(jobGroupingService.getGroupingStats());
        } catch (Exception e) {
            logger.error("Error getting grouping statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving grouping statistics: " + e.getMessage());
        }
    }
    
    /**
     * Get database statistics.
     */
    @GetMapping("/database-stats")
    public ResponseEntity<Object> getDatabaseStats() {
        try {
            return ResponseEntity.ok(databasePersistenceService.getJobStatistics());
        } catch (Exception e) {
            logger.error("Error getting database statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving database statistics: " + e.getMessage());
        }
    }
    
    /**
     * Get scheduled jobs by status.
     */
    @GetMapping("/jobs/status/{status}")
    public ResponseEntity<Object> getJobsByStatus(@PathVariable String status) {
        try {
            return ResponseEntity.ok(databasePersistenceService.findScheduledJobsByStatus(status));
        } catch (Exception e) {
            logger.error("Error getting jobs by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving jobs by status: " + e.getMessage());
        }
    }
    
    /**
     * Get scheduled jobs by job type.
     */
    @GetMapping("/jobs/type/{jobType}")
    public ResponseEntity<Object> getJobsByType(@PathVariable String jobType) {
        try {
            return ResponseEntity.ok(databasePersistenceService.findScheduledJobsByJobType(jobType));
        } catch (Exception e) {
            logger.error("Error getting jobs by type: {}", jobType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving jobs by type: " + e.getMessage());
        }
    }
    
    /**
     * Get scheduled job by job ID.
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Object> getJobById(@PathVariable String jobId) {
        try {
            return databasePersistenceService.findScheduledJobByJobId(jobId)
                    .map(job -> ResponseEntity.ok(job))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error getting job by ID: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving job: " + e.getMessage());
        }
    }
    
    /**
     * Get all thread pools.
     */
    @GetMapping("/thread-pools")
    public ResponseEntity<Object> getThreadPools() {
        try {
            return ResponseEntity.ok(databasePersistenceService.findAllActiveThreadPools());
        } catch (Exception e) {
            logger.error("Error getting thread pools", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving thread pools: " + e.getMessage());
        }
    }
    
    /**
     * Get all app servers.
     */
    @GetMapping("/app-servers")
    public ResponseEntity<Object> getAppServers() {
        try {
            return ResponseEntity.ok(databasePersistenceService.findAllActiveAppServers());
        } catch (Exception e) {
            logger.error("Error getting app servers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving app servers: " + e.getMessage());
        }
    }
    
    /**
     * Get rate limiting statistics.
     */
    @GetMapping("/rate-limiting-stats")
    public ResponseEntity<Object> getRateLimitingStats() {
        try {
            return ResponseEntity.ok(rateLimitingService.getRateLimitingStats());
        } catch (Exception e) {
            logger.error("Error getting rate limiting statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving rate limiting statistics: " + e.getMessage());
        }
    }
    
    /**
     * Get retry statistics.
     */
    @GetMapping("/retry-stats")
    public ResponseEntity<Object> getRetryStats() {
        try {
            return ResponseEntity.ok(jobRetryService.getRetryStatistics());
        } catch (Exception e) {
            logger.error("Error getting retry statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving retry statistics: " + e.getMessage());
        }
    }
    
    /**
     * Cancel retries for a specific job.
     */
    @PostMapping("/jobs/{jobId}/cancel-retries")
    public ResponseEntity<Object> cancelRetries(@PathVariable String jobId) {
        try {
            jobRetryService.cancelRetries(jobId);
            final String finalJobId = jobId;
            return ResponseEntity.ok(new Object() {
                public final String jobId = finalJobId;
                public final String message = "Retries cancelled successfully";
                public final long timestamp = System.currentTimeMillis();
            });
        } catch (Exception e) {
            logger.error("Error cancelling retries for job: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error cancelling retries: " + e.getMessage());
        }
    }
    
    /**
     * Reset retry count for a specific job.
     */
    @PostMapping("/jobs/{jobId}/reset-retries")
    public ResponseEntity<Object> resetRetries(@PathVariable String jobId) {
        try {
            jobRetryService.resetRetryCount(jobId);
            return ResponseEntity.ok(new Object() {
                public final String jobId = jobId;
                public final String message = "Retry count reset successfully";
                public final long timestamp = System.currentTimeMillis();
            });
        } catch (Exception e) {
            logger.error("Error resetting retries for job: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error resetting retries: " + e.getMessage());
        }
    }
    
    /**
     * Manually trigger retry for a specific job.
     */
    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<Object> triggerRetry(@PathVariable String jobId) {
        try {
            jobRetryService.executeRetry(jobId);
            return ResponseEntity.ok(new Object() {
                public final String jobId = jobId;
                public final String message = "Retry triggered successfully";
                public final long timestamp = System.currentTimeMillis();
            });
        } catch (Exception e) {
            logger.error("Error triggering retry for job: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error triggering retry: " + e.getMessage());
        }
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Object> healthCheck() {
        return ResponseEntity.ok(new Object() {
            public final String status = "UP";
            public final String service = "Job Dispatcher";
            public final long timestamp = System.currentTimeMillis();
        });
    }
}
