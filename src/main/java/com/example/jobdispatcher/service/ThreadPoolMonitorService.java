package com.example.jobdispatcher.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for monitoring thread pool and queue status.
 */
@Service
public class ThreadPoolMonitorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolMonitorService.class);
    
    @Autowired
    @Qualifier("oneTimeJobExecutor")
    private ThreadPoolTaskExecutor oneTimeJobExecutor;
    
    @Autowired
    @Qualifier("repetitiveJobScheduler")
    private ThreadPoolTaskScheduler repetitiveJobScheduler;
    
    /**
     * Get comprehensive thread pool statistics.
     */
    public Map<String, Object> getThreadPoolStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // One-time job executor stats
        Map<String, Object> oneTimeStats = getExecutorStats(oneTimeJobExecutor, "One-Time Jobs");
        stats.put("oneTimeJobExecutor", oneTimeStats);
        
        // Repetitive job scheduler stats
        Map<String, Object> repetitiveStats = getSchedulerStats(repetitiveJobScheduler, "Repetitive Jobs");
        stats.put("repetitiveJobScheduler", repetitiveStats);
        
        return stats;
    }
    
    /**
     * Get statistics for a ThreadPoolTaskExecutor.
     */
    private Map<String, Object> getExecutorStats(ThreadPoolTaskExecutor executor, String poolName) {
        Map<String, Object> stats = new HashMap<>();
        
        if (executor.getThreadPoolExecutor() != null) {
            ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
            
            stats.put("poolName", poolName);
            stats.put("corePoolSize", threadPoolExecutor.getCorePoolSize());
            stats.put("maximumPoolSize", threadPoolExecutor.getMaximumPoolSize());
            stats.put("activeThreads", threadPoolExecutor.getActiveCount());
            stats.put("poolSize", threadPoolExecutor.getPoolSize());
            stats.put("largestPoolSize", threadPoolExecutor.getLargestPoolSize());
            stats.put("completedTaskCount", threadPoolExecutor.getCompletedTaskCount());
            stats.put("totalTaskCount", threadPoolExecutor.getTaskCount());
            stats.put("queueSize", threadPoolExecutor.getQueue().size());
            stats.put("queueRemainingCapacity", threadPoolExecutor.getQueue().remainingCapacity());
            stats.put("isShutdown", threadPoolExecutor.isShutdown());
            stats.put("isTerminated", threadPoolExecutor.isTerminated());
        }
        
        return stats;
    }
    
    /**
     * Get statistics for a ThreadPoolTaskScheduler.
     */
    private Map<String, Object> getSchedulerStats(ThreadPoolTaskScheduler scheduler, String poolName) {
        Map<String, Object> stats = new HashMap<>();
        
        if (scheduler.getScheduledThreadPoolExecutor() != null) {
            ThreadPoolExecutor threadPoolExecutor = scheduler.getScheduledThreadPoolExecutor();
            
            stats.put("poolName", poolName);
            stats.put("corePoolSize", threadPoolExecutor.getCorePoolSize());
            stats.put("maximumPoolSize", threadPoolExecutor.getMaximumPoolSize());
            stats.put("activeThreads", threadPoolExecutor.getActiveCount());
            stats.put("poolSize", threadPoolExecutor.getPoolSize());
            stats.put("largestPoolSize", threadPoolExecutor.getLargestPoolSize());
            stats.put("completedTaskCount", threadPoolExecutor.getCompletedTaskCount());
            stats.put("totalTaskCount", threadPoolExecutor.getTaskCount());
            stats.put("queueSize", threadPoolExecutor.getQueue().size());
            stats.put("queueRemainingCapacity", threadPoolExecutor.getQueue().remainingCapacity());
            stats.put("isShutdown", threadPoolExecutor.isShutdown());
            stats.put("isTerminated", threadPoolExecutor.isTerminated());
        }
        
        return stats;
    }
    
    /**
     * Get queue utilization percentage for each thread pool.
     */
    public Map<String, Object> getQueueUtilization() {
        Map<String, Object> utilization = new HashMap<>();
        
        // One-time job queue utilization
        if (oneTimeJobExecutor.getThreadPoolExecutor() != null) {
            ThreadPoolExecutor executor = oneTimeJobExecutor.getThreadPoolExecutor();
            int queueSize = executor.getQueue().size();
            int queueCapacity = executor.getQueue().size() + executor.getQueue().remainingCapacity();
            double utilizationPercent = queueCapacity > 0 ? (double) queueSize / queueCapacity * 100 : 0;
            
            Map<String, Object> oneTimeUtil = new HashMap<>();
            oneTimeUtil.put("queueSize", queueSize);
            oneTimeUtil.put("queueCapacity", queueCapacity);
            oneTimeUtil.put("utilizationPercent", String.format("%.2f", utilizationPercent));
            oneTimeUtil.put("status", utilizationPercent > 80 ? "HIGH" : utilizationPercent > 50 ? "MEDIUM" : "LOW");
            
            utilization.put("oneTimeJobQueue", oneTimeUtil);
        }
        
        // Repetitive job queue utilization
        if (repetitiveJobScheduler.getScheduledThreadPoolExecutor() != null) {
            ThreadPoolExecutor executor = repetitiveJobScheduler.getScheduledThreadPoolExecutor();
            int queueSize = executor.getQueue().size();
            int queueCapacity = executor.getQueue().size() + executor.getQueue().remainingCapacity();
            double utilizationPercent = queueCapacity > 0 ? (double) queueSize / queueCapacity * 100 : 0;
            
            Map<String, Object> repetitiveUtil = new HashMap<>();
            repetitiveUtil.put("queueSize", queueSize);
            repetitiveUtil.put("queueCapacity", queueCapacity);
            repetitiveUtil.put("utilizationPercent", String.format("%.2f", utilizationPercent));
            repetitiveUtil.put("status", utilizationPercent > 80 ? "HIGH" : utilizationPercent > 50 ? "MEDIUM" : "LOW");
            
            utilization.put("repetitiveJobQueue", repetitiveUtil);
        }
        
        return utilization;
    }
}

