package com.example.jobdispatcher.service;

import com.example.jobdispatcher.model.OneTimeJobRequest;
import com.example.jobdispatcher.model.RepetitiveJobRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service responsible for managing job grouping and buffering.
 * Groups jobs with the same key and executes only one job per group within a buffer window.
 */
@Service
public class JobGroupingService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobGroupingService.class);
    
    @Autowired
    @Qualifier("repetitiveJobScheduler")
    private ThreadPoolTaskScheduler scheduler;
    
    @Autowired
    @Lazy
    private JobDispatcherService jobDispatcherService;
    
    // Store job groups and their metadata
    private final ConcurrentHashMap<String, JobGroup> jobGroups = new ConcurrentHashMap<>();
    
    /**
     * Process a one-time job request with grouping logic.
     * If grouping is enabled and a group exists, the job is added to the group.
     * Otherwise, it's dispatched immediately.
     */
    public void processOneTimeJob(OneTimeJobRequest request) {
        if (!request.isCanGroup() || request.getGroupKey() == null || request.getGroupKey().trim().isEmpty()) {
            // No grouping, dispatch immediately
            logger.info("Job {} dispatched immediately (no grouping)", request.getJobId());
            return;
        }
        
        String groupKey = request.getGroupKey();
        JobGroup group = jobGroups.computeIfAbsent(groupKey, k -> new JobGroup(groupKey, request.getGroupBufferMillis()));
        
        synchronized (group) {
            group.addJob(request);
            
            if (group.getJobCount() == 1) {
                // First job in group, schedule the buffer timer
                logger.info("First job {} added to group {}, scheduling buffer timer for {}ms", 
                           request.getJobId(), groupKey, request.getGroupBufferMillis());
                
                ScheduledFuture<?> timer = scheduler.schedule(() -> {
                    dispatchGroupedJobs(groupKey);
                }, new java.util.Date(System.currentTimeMillis() + request.getGroupBufferMillis()));
                
                group.setTimer(timer);
            } else {
                logger.info("Job {} added to group {} (total jobs: {})", 
                           request.getJobId(), groupKey, group.getJobCount());
            }
        }
    }
    
    /**
     * Process a repetitive job request with grouping logic.
     * For repetitive jobs, grouping is typically not used, but we support it for consistency.
     */
    public void processRepetitiveJob(RepetitiveJobRequest request) {
        if (!request.isCanGroup() || request.getGroupKey() == null || request.getGroupKey().trim().isEmpty()) {
            // No grouping, dispatch immediately
            logger.info("Repetitive job {} dispatched immediately (no grouping)", request.getJobId());
            return;
        }
        
        // For repetitive jobs, we might want different grouping logic
        // For now, we'll treat them similar to one-time jobs
        String groupKey = request.getGroupKey();
        JobGroup group = jobGroups.computeIfAbsent(groupKey, k -> new JobGroup(groupKey, request.getGroupBufferMillis()));
        
        synchronized (group) {
            group.addRepetitiveJob(request);
            
            if (group.getRepetitiveJobCount() == 1) {
                logger.info("First repetitive job {} added to group {}, scheduling buffer timer for {}ms", 
                           request.getJobId(), groupKey, request.getGroupBufferMillis());
                
                ScheduledFuture<?> timer = scheduler.schedule(() -> {
                    dispatchGroupedRepetitiveJobs(groupKey);
                }, new java.util.Date(System.currentTimeMillis() + request.getGroupBufferMillis()));
                
                group.setRepetitiveTimer(timer);
            } else {
                logger.info("Repetitive job {} added to group {} (total repetitive jobs: {})", 
                           request.getJobId(), groupKey, group.getRepetitiveJobCount());
            }
        }
    }
    
    /**
     * Dispatch all jobs in a group (one-time jobs).
     */
    private void dispatchGroupedJobs(String groupKey) {
        JobGroup group = jobGroups.remove(groupKey);
        if (group == null) {
            logger.warn("Group {} not found for dispatch", groupKey);
            return;
        }
        
        synchronized (group) {
            logger.info("Dispatching {} jobs from group {}", group.getJobCount(), groupKey);
            
            // Dispatch only the first job from the group
            OneTimeJobRequest firstJob = group.getFirstJob();
            if (firstJob != null) {
                logger.info("Dispatching representative job {} for group {} (representing {} jobs)", 
                           firstJob.getJobId(), groupKey, group.getJobCount());
                
                // Call the actual job dispatcher
                jobDispatcherService.dispatchGroupedOneTimeJob(firstJob, group.getJobCount());
            }
        }
    }
    
    /**
     * Dispatch all repetitive jobs in a group.
     */
    private void dispatchGroupedRepetitiveJobs(String groupKey) {
        JobGroup group = jobGroups.remove(groupKey);
        if (group == null) {
            logger.warn("Group {} not found for repetitive dispatch", groupKey);
            return;
        }
        
        synchronized (group) {
            logger.info("Dispatching {} repetitive jobs from group {}", group.getRepetitiveJobCount(), groupKey);
            
            // Dispatch only the first repetitive job from the group
            RepetitiveJobRequest firstJob = group.getFirstRepetitiveJob();
            if (firstJob != null) {
                logger.info("Dispatching representative repetitive job {} for group {} (representing {} jobs)", 
                           firstJob.getJobId(), groupKey, group.getRepetitiveJobCount());
                
                // Call the actual job dispatcher
                jobDispatcherService.dispatchGroupedRepetitiveJob(firstJob, group.getRepetitiveJobCount());
            }
        }
    }
    
    /**
     * Get statistics about current job groups.
     */
    public java.util.Map<String, Object> getGroupingStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalGroups", jobGroups.size());
        
        java.util.Map<String, Object> groupDetails = new java.util.HashMap<>();
        jobGroups.forEach((key, group) -> {
            java.util.Map<String, Object> groupInfo = new java.util.HashMap<>();
            groupInfo.put("jobCount", group.getJobCount());
            groupInfo.put("repetitiveJobCount", group.getRepetitiveJobCount());
            groupInfo.put("bufferMillis", group.getBufferMillis());
            groupInfo.put("createdAt", group.getCreatedAt());
            groupDetails.put(key, groupInfo);
        });
        
        stats.put("groups", groupDetails);
        return stats;
    }
    
    /**
     * Inner class to represent a job group.
     */
    private static class JobGroup {
        private final String groupKey;
        private final long bufferMillis;
        private final long createdAt;
        private final java.util.List<OneTimeJobRequest> jobs = new java.util.ArrayList<>();
        private final java.util.List<RepetitiveJobRequest> repetitiveJobs = new java.util.ArrayList<>();
        private ScheduledFuture<?> timer;
        private ScheduledFuture<?> repetitiveTimer;
        
        public JobGroup(String groupKey, long bufferMillis) {
            this.groupKey = groupKey;
            this.bufferMillis = bufferMillis;
            this.createdAt = System.currentTimeMillis();
        }
        
        public void addJob(OneTimeJobRequest job) {
            jobs.add(job);
        }
        
        public void addRepetitiveJob(RepetitiveJobRequest job) {
            repetitiveJobs.add(job);
        }
        
        public int getJobCount() {
            return jobs.size();
        }
        
        public int getRepetitiveJobCount() {
            return repetitiveJobs.size();
        }
        
        public OneTimeJobRequest getFirstJob() {
            return jobs.isEmpty() ? null : jobs.get(0);
        }
        
        public RepetitiveJobRequest getFirstRepetitiveJob() {
            return repetitiveJobs.isEmpty() ? null : repetitiveJobs.get(0);
        }
        
        public void setTimer(ScheduledFuture<?> timer) {
            this.timer = timer;
        }
        
        public void setRepetitiveTimer(ScheduledFuture<?> timer) {
            this.repetitiveTimer = timer;
        }
        
        public String getGroupKey() {
            return groupKey;
        }
        
        public long getBufferMillis() {
            return bufferMillis;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
    }
}
