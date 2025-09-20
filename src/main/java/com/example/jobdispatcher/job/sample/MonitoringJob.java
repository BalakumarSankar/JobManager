package com.example.jobdispatcher.job.sample;

import com.example.jobdispatcher.job.RepetitiveJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample monitoring repetitive job implementation.
 */
public class MonitoringJob implements RepetitiveJob {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringJob.class);
    
    private String jobId;
    private String jobName;
    private final long intervalMillis;
    private final long initialDelayMillis;
    private final RepetitionType repetitionType;
    
    public MonitoringJob() {
        // Default values - will be overridden by external values
        this.jobId = "monitoring-" + System.currentTimeMillis();
        this.jobName = "System Monitoring Job";
        this.intervalMillis = 10000; // 10 seconds
        this.initialDelayMillis = 2000; // 2 seconds initial delay
        this.repetitionType = RepetitionType.FIXED_RATE;
    }
    
    @Override
    public void process() {
        logger.info("Starting system monitoring check...");
        
        try {
            // Simulate monitoring checks
            long memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long maxMemory = Runtime.getRuntime().maxMemory();
            double memoryUsagePercent = (double) memoryUsage / maxMemory * 100;
            
            logger.info("System Status Check:");
            logger.info("- Memory Usage: {} MB ({}%)", memoryUsage / 1024 / 1024, String.format("%.2f", memoryUsagePercent));
            logger.info("- Available Processors: {}", Runtime.getRuntime().availableProcessors());
            logger.info("- Uptime: {} ms", System.currentTimeMillis());
            
            if (memoryUsagePercent > 80) {
                logger.warn("High memory usage detected: {}%", String.format("%.2f", memoryUsagePercent));
            }
            
        } catch (Exception e) {
            logger.error("Error in monitoring job", e);
        }
    }
    
    @Override
    public String getJobId() {
        return jobId;
    }
    
    @Override
    public String getJobName() {
        return jobName;
    }
    
    @Override
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
    
    @Override
    public long getIntervalMillis() {
        return intervalMillis;
    }
    
    @Override
    public long getInitialDelayMillis() {
        return initialDelayMillis;
    }
    
    @Override
    public RepetitionType getRepetitionType() {
        return repetitionType;
    }
}
