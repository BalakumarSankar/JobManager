package com.example.jobdispatcher.service;

import com.example.jobdispatcher.job.Job;
import com.example.jobdispatcher.job.OneTimeJob;
import com.example.jobdispatcher.job.RepetitiveJob;
import com.example.jobdispatcher.model.OneTimeJobRequest;
import com.example.jobdispatcher.model.RepetitiveJobRequest;
import com.example.jobdispatcher.entity.ScheduledJob;
import com.example.jobdispatcher.entity.ThreadPool;
import com.example.jobdispatcher.entity.AppServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.support.CronTrigger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.Date;

/**
 * Service responsible for dispatching jobs to appropriate thread pools.
 */
@Service
public class JobDispatcherService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobDispatcherService.class);
    
    @Autowired
    @Qualifier("oneTimeJobExecutor")
    private Executor oneTimeJobExecutor;
    
    @Autowired
    @Qualifier("repetitiveJobScheduler")
    private ThreadPoolTaskScheduler repetitiveJobScheduler;
    
    @Autowired
    private JobGroupingService jobGroupingService;
    
    @Autowired
    private DatabasePersistenceService databasePersistenceService;
    
    @Autowired
    private JobRetryService jobRetryService;
    
    // Store scheduled tasks for management
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    /**
     * Dispatch a one-time job to the thread pool.
     * If grouping is enabled, the job will be processed by the grouping service.
     */
    public void dispatchOneTimeJob(OneTimeJobRequest request) {
        try {
            // Create and save scheduled job record
            ScheduledJob scheduledJob = createScheduledJobFromRequest(request, "ONE_TIME");
            databasePersistenceService.saveScheduledJob(scheduledJob);
            
            // Check if job should be grouped
            if (request.isCanGroup() && request.getGroupKey() != null && !request.getGroupKey().trim().isEmpty()) {
                logger.info("Job {} submitted for grouping with key: {}", request.getJobId(), request.getGroupKey());
                jobGroupingService.processOneTimeJob(request);
                return;
            }
            
            // No grouping, dispatch immediately
            Job job = createJobInstance(request.getJobClassName());
            if (job instanceof OneTimeJob) {
                // Set the external job ID and name from the request
                job.setJobId(request.getJobId());
                job.setJobName(request.getJobName());
                
                logger.info("Dispatching one-time job: {} with ID: {}", request.getJobName(), request.getJobId());
                
                oneTimeJobExecutor.execute(() -> {
                    try {
                        databasePersistenceService.markJobAsStarted(request.getJobId());
                        logger.info("Executing one-time job: {} with ID: {}", job.getJobName(), job.getJobId());
                        job.process();
                        databasePersistenceService.markJobAsCompleted(request.getJobId());
                        logger.info("Completed one-time job: {} with ID: {}", job.getJobName(), job.getJobId());
                    } catch (Exception e) {
                        databasePersistenceService.markJobAsFailed(request.getJobId(), e.getMessage());
                        logger.error("Error executing one-time job: {} with ID: {}", job.getJobName(), job.getJobId(), e);
                        
                        // Schedule retry if applicable
                        try {
                            ScheduledJob retryJob = databasePersistenceService.findScheduledJobByJobId(request.getJobId()).orElse(null);
                            if (retryJob != null) {
                                jobRetryService.scheduleRetry(retryJob, e);
                            }
                        } catch (Exception retryException) {
                            logger.error("Error scheduling retry for job: {}", request.getJobId(), retryException);
                        }
                    }
                });
            } else {
                throw new IllegalArgumentException("Job class must implement OneTimeJob interface");
            }
        } catch (Exception e) {
            logger.error("Error dispatching one-time job: {}", request.getJobId(), e);
            throw new RuntimeException("Failed to dispatch one-time job", e);
        }
    }
    
    /**
     * Dispatch a repetitive job to the scheduler.
     * If grouping is enabled, the job will be processed by the grouping service.
     */
    public void dispatchRepetitiveJob(RepetitiveJobRequest request) {
        try {
            // Check if job should be grouped
            if (request.isCanGroup() && request.getGroupKey() != null && !request.getGroupKey().trim().isEmpty()) {
                logger.info("Repetitive job {} submitted for grouping with key: {}", request.getJobId(), request.getGroupKey());
                jobGroupingService.processRepetitiveJob(request);
                return;
            }
            
            // No grouping, dispatch immediately
            Job job = createJobInstance(request.getJobClassName());
            if (job instanceof RepetitiveJob) {
                RepetitiveJob repetitiveJob = (RepetitiveJob) job;
                
                // Set the external job ID and name from the request
                job.setJobId(request.getJobId());
                job.setJobName(request.getJobName());
                
                logger.info("Dispatching repetitive job: {} with ID: {}", request.getJobName(), request.getJobId());
                
        // Persist the scheduled job with cron if present
        ScheduledJob scheduledJob = createScheduledJobFromRequest(request, "REPETITIVE");
        databasePersistenceService.saveScheduledJob(scheduledJob);
        
        ScheduledFuture<?> scheduledTask = scheduleRepetitiveJob(repetitiveJob, request);
                scheduledTasks.put(request.getJobId(), scheduledTask);
                
                logger.info("Scheduled repetitive job: {} with ID: {}", request.getJobName(), request.getJobId());
            } else {
                throw new IllegalArgumentException("Job class must implement RepetitiveJob interface");
            }
        } catch (Exception e) {
            logger.error("Error dispatching repetitive job: {}", request.getJobId(), e);
            throw new RuntimeException("Failed to dispatch repetitive job", e);
        }
    }
    
    /**
     * Dispatch a grouped one-time job (called by JobGroupingService).
     */
    public void dispatchGroupedOneTimeJob(OneTimeJobRequest request, int totalJobsInGroup) {
        try {
            Job job = createJobInstance(request.getJobClassName());
            if (job instanceof OneTimeJob) {
                // Set the external job ID and name from the request
                job.setJobId(request.getJobId());
                job.setJobName(request.getJobName());
                
                logger.info("Dispatching grouped one-time job: {} with ID: {} (representing {} jobs)", 
                           request.getJobName(), request.getJobId(), totalJobsInGroup);
                
                oneTimeJobExecutor.execute(() -> {
                    try {
                        logger.info("Executing grouped one-time job: {} with ID: {} (representing {} jobs)", 
                                   job.getJobName(), job.getJobId(), totalJobsInGroup);
                        job.process();
                        logger.info("Completed grouped one-time job: {} with ID: {} (representing {} jobs)", 
                                   job.getJobName(), job.getJobId(), totalJobsInGroup);
                    } catch (Exception e) {
                        logger.error("Error executing grouped one-time job: {} with ID: {} (representing {} jobs)", 
                                   job.getJobName(), job.getJobId(), totalJobsInGroup, e);
                    }
                });
            } else {
                throw new IllegalArgumentException("Job class must implement OneTimeJob interface");
            }
        } catch (Exception e) {
            logger.error("Error dispatching grouped one-time job: {}", request.getJobId(), e);
            throw new RuntimeException("Failed to dispatch grouped one-time job", e);
        }
    }
    
    /**
     * Dispatch a grouped repetitive job (called by JobGroupingService).
     */
    public void dispatchGroupedRepetitiveJob(RepetitiveJobRequest request, int totalJobsInGroup) {
        try {
            Job job = createJobInstance(request.getJobClassName());
            if (job instanceof RepetitiveJob) {
                RepetitiveJob repetitiveJob = (RepetitiveJob) job;
                
                // Set the external job ID and name from the request
                job.setJobId(request.getJobId());
                job.setJobName(request.getJobName());
                
                logger.info("Dispatching grouped repetitive job: {} with ID: {} (representing {} jobs)", 
                           request.getJobName(), request.getJobId(), totalJobsInGroup);
                
        ScheduledJob scheduledJob = createScheduledJobFromRequest(request, "REPETITIVE");
        databasePersistenceService.saveScheduledJob(scheduledJob);
        
        ScheduledFuture<?> scheduledTask = scheduleRepetitiveJob(repetitiveJob, request);
                scheduledTasks.put(request.getJobId(), scheduledTask);
                
                logger.info("Scheduled grouped repetitive job: {} with ID: {} (representing {} jobs)", 
                           request.getJobName(), request.getJobId(), totalJobsInGroup);
            } else {
                throw new IllegalArgumentException("Job class must implement RepetitiveJob interface");
            }
        } catch (Exception e) {
            logger.error("Error dispatching grouped repetitive job: {}", request.getJobId(), e);
            throw new RuntimeException("Failed to dispatch grouped repetitive job", e);
        }
    }
    
    /**
     * Cancel a scheduled repetitive job.
     */
    public boolean cancelRepetitiveJob(String jobId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.remove(jobId);
        if (scheduledTask != null) {
            boolean cancelled = scheduledTask.cancel(false);
            logger.info("Cancelled repetitive job with ID: {}, success: {}", jobId, cancelled);
            return cancelled;
        }
        logger.warn("No scheduled task found for job ID: {}", jobId);
        return false;
    }
    
    /**
     * Check if a repetitive job is currently scheduled.
     */
    public boolean isJobScheduled(String jobId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(jobId);
        return scheduledTask != null && !scheduledTask.isDone();
    }
    
    /**
     * Get the number of active scheduled tasks.
     */
    public int getActiveScheduledTasksCount() {
        return (int) scheduledTasks.values().stream()
                .filter(task -> !task.isDone())
                .count();
    }
    
    /**
     * Create a job instance from the class name.
     */
    private Job createJobInstance(String className) throws Exception {
        Class<?> jobClass = Class.forName(className);
        return (Job) jobClass.getDeclaredConstructor().newInstance();
    }
    
    /**
     * Schedule a repetitive job based on its repetition type.
     */
    private ScheduledFuture<?> scheduleRepetitiveJob(RepetitiveJob job, RepetitiveJobRequest request) {
        long initialDelay = request.getInitialDelayMillis();
        long interval = request.getIntervalMillis();
        
        Runnable task = () -> {
            try {
                logger.info("Executing repetitive job: {} with ID: {}", job.getJobName(), job.getJobId());
                job.process();
                logger.info("Completed repetitive job: {} with ID: {}", job.getJobName(), job.getJobId());
            } catch (Exception e) {
                logger.error("Error executing repetitive job: {} with ID: {}", job.getJobName(), job.getJobId(), e);
            }
        };
        
        switch (request.getRepetitionType()) {
            case FIXED_DELAY:
                return repetitiveJobScheduler.scheduleWithFixedDelay(task, new Date(System.currentTimeMillis() + initialDelay), interval);
            case FIXED_RATE:
                return repetitiveJobScheduler.scheduleAtFixedRate(task, new Date(System.currentTimeMillis() + initialDelay), interval);
            case CRON:
                if (request.getCronExpression() == null || request.getCronExpression().trim().isEmpty()) {
                    throw new IllegalArgumentException("cronExpression is required when repetitionType is CRON");
                }
                return repetitiveJobScheduler.schedule(task, new CronTrigger(request.getCronExpression()));
            default:
                throw new IllegalArgumentException("Unsupported repetition type: " + request.getRepetitionType());
        }
    }
    
    /**
     * Create a ScheduledJob entity from a OneTimeJobRequest.
     */
    private ScheduledJob createScheduledJobFromRequest(OneTimeJobRequest request, String jobType) {
        ScheduledJob scheduledJob = new ScheduledJob(request.getJobId(), request.getJobName(), request.getJobClassName(), jobType);
        scheduledJob.setGroupKey(request.getGroupKey());
        scheduledJob.setCanGroup(request.isCanGroup());
        scheduledJob.setGroupBufferMillis(request.getGroupBufferMillis());
        
        // All jobs have equal priority (NORMAL)
        scheduledJob.setJobPriority(com.example.jobdispatcher.enums.JobPriority.NORMAL);
        
        // Set default thread pool (one-time job executor)
        databasePersistenceService.findThreadPoolByName("one-time-job-executor")
            .ifPresent(scheduledJob::setThreadPool);
        
        return scheduledJob;
    }
    
    /**
     * Create a ScheduledJob entity from a RepetitiveJobRequest.
     */
    private ScheduledJob createScheduledJobFromRequest(RepetitiveJobRequest request, String jobType) {
        ScheduledJob scheduledJob = new ScheduledJob(request.getJobId(), request.getJobName(), request.getJobClassName(), jobType);
        scheduledJob.setGroupKey(request.getGroupKey());
        scheduledJob.setCanGroup(request.isCanGroup());
        scheduledJob.setGroupBufferMillis(request.getGroupBufferMillis());
        scheduledJob.setIntervalMillis(request.getIntervalMillis());
        scheduledJob.setInitialDelayMillis(request.getInitialDelayMillis());
        scheduledJob.setRepetitionType(request.getRepetitionType().toString());
        scheduledJob.setCronExpression(request.getCronExpression());
        
        // All jobs have equal priority (NORMAL)
        scheduledJob.setJobPriority(com.example.jobdispatcher.enums.JobPriority.NORMAL);
        
        // Set default thread pool (repetitive job scheduler)
        databasePersistenceService.findThreadPoolByName("repetitive-job-scheduler")
            .ifPresent(scheduledJob::setThreadPool);
        
        return scheduledJob;
    }
    
    /**
     * Re-dispatch a one-time job for retry.
     */
    public void redispatchOneTimeJob(ScheduledJob scheduledJob) {
        try {
            Job job = createJobInstance(scheduledJob.getJobClassName());
            if (job instanceof OneTimeJob) {
                job.setJobId(scheduledJob.getJobId());
                job.setJobName(scheduledJob.getJobName());
                
                logger.info("Re-dispatching one-time job for retry: {} with ID: {}", 
                           scheduledJob.getJobName(), scheduledJob.getJobId());
                
                oneTimeJobExecutor.execute(() -> {
                    try {
                        databasePersistenceService.markJobAsStarted(scheduledJob.getJobId());
                        logger.info("Executing retry for one-time job: {} with ID: {}", 
                                   job.getJobName(), job.getJobId());
                        job.process();
                        databasePersistenceService.markJobAsCompleted(scheduledJob.getJobId());
                        logger.info("Completed retry for one-time job: {} with ID: {}", 
                                   job.getJobName(), job.getJobId());
                    } catch (Exception e) {
                        databasePersistenceService.markJobAsFailed(scheduledJob.getJobId(), e.getMessage());
                        logger.error("Error executing retry for one-time job: {} with ID: {}", 
                                    job.getJobName(), job.getJobId(), e);
                        
                        // Schedule another retry if applicable
                        try {
                            ScheduledJob updatedJob = databasePersistenceService.findScheduledJobByJobId(scheduledJob.getJobId()).orElse(null);
                            if (updatedJob != null) {
                                jobRetryService.scheduleRetry(updatedJob, e);
                            }
                        } catch (Exception retryException) {
                            logger.error("Error scheduling retry for job: {}", scheduledJob.getJobId(), retryException);
                        }
                    }
                });
            } else {
                throw new IllegalArgumentException("Job class must implement OneTimeJob interface");
            }
        } catch (Exception e) {
            logger.error("Error re-dispatching one-time job: {}", scheduledJob.getJobId(), e);
            throw new RuntimeException("Failed to re-dispatch one-time job", e);
        }
    }
    
    /**
     * Re-dispatch a repetitive job for retry.
     */
    public void redispatchRepetitiveJob(ScheduledJob scheduledJob) {
        try {
            Job job = createJobInstance(scheduledJob.getJobClassName());
            if (job instanceof RepetitiveJob) {
                job.setJobId(scheduledJob.getJobId());
                job.setJobName(scheduledJob.getJobName());
                
                logger.info("Re-dispatching repetitive job for retry: {} with ID: {}", 
                           scheduledJob.getJobName(), scheduledJob.getJobId());
                
                // Re-schedule the repetitive job
                ScheduledFuture<?> scheduledTask = scheduleRepetitiveJob((RepetitiveJob) job, scheduledJob);
                scheduledTasks.put(scheduledJob.getJobId(), scheduledTask);
                
            } else {
                throw new IllegalArgumentException("Job class must implement RepetitiveJob interface");
            }
        } catch (Exception e) {
            logger.error("Error re-dispatching repetitive job: {}", scheduledJob.getJobId(), e);
            throw new RuntimeException("Failed to re-dispatch repetitive job", e);
        }
    }
    
    /**
     * Schedule a repetitive job from ScheduledJob entity.
     */
    private ScheduledFuture<?> scheduleRepetitiveJob(RepetitiveJob job, ScheduledJob scheduledJob) {
        long initialDelay = scheduledJob.getInitialDelayMillis() != null ? 
                           scheduledJob.getInitialDelayMillis() : 0;
        long interval = scheduledJob.getIntervalMillis() != null ? 
                       scheduledJob.getIntervalMillis() : 60000; // Default 1 minute
        
        Runnable task = () -> {
            try {
                databasePersistenceService.markJobAsStarted(scheduledJob.getJobId());
                logger.info("Executing retry for repetitive job: {} with ID: {}", 
                           job.getJobName(), job.getJobId());
                job.process();
                databasePersistenceService.markJobAsCompleted(scheduledJob.getJobId());
                logger.info("Completed retry for repetitive job: {} with ID: {}", 
                           job.getJobName(), job.getJobId());
            } catch (Exception e) {
                databasePersistenceService.markJobAsFailed(scheduledJob.getJobId(), e.getMessage());
                logger.error("Error executing retry for repetitive job: {} with ID: {}", 
                            job.getJobName(), job.getJobId(), e);
                
                // Schedule another retry if applicable
                try {
                    ScheduledJob updatedJob = databasePersistenceService.findScheduledJobByJobId(scheduledJob.getJobId()).orElse(null);
                    if (updatedJob != null) {
                        jobRetryService.scheduleRetry(updatedJob, e);
                    }
                } catch (Exception retryException) {
                    logger.error("Error scheduling retry for job: {}", scheduledJob.getJobId(), retryException);
                }
            }
        };
        
        // Use the repetition type from the scheduled job
        String repetitionType = scheduledJob.getRepetitionType();
        if ("FIXED_DELAY".equals(repetitionType)) {
            return repetitiveJobScheduler.scheduleWithFixedDelay(task, new Date(System.currentTimeMillis() + initialDelay), interval);
        } else if ("FIXED_RATE".equals(repetitionType)) {
            return repetitiveJobScheduler.scheduleAtFixedRate(task, new Date(System.currentTimeMillis() + initialDelay), interval);
        } else if ("CRON".equals(repetitionType)) {
            if (scheduledJob.getCronExpression() == null || scheduledJob.getCronExpression().trim().isEmpty()) {
                throw new IllegalArgumentException("cronExpression is required for CRON repetition type");
            }
            return repetitiveJobScheduler.schedule(task, new CronTrigger(scheduledJob.getCronExpression()));
        } else {
            // Default to fixed delay
            return repetitiveJobScheduler.scheduleWithFixedDelay(task, new Date(System.currentTimeMillis() + initialDelay), interval);
        }
    }
}
