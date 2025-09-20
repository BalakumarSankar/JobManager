package com.example.jobdispatcher.job.sample;

import com.example.jobdispatcher.job.RepetitiveJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample repetitive job implementation for testing purposes.
 */
public class SampleRepetitiveJob implements RepetitiveJob {
    
    private static final Logger logger = LoggerFactory.getLogger(SampleRepetitiveJob.class);
    
    private String jobId;
    private String jobName;
    private final long intervalMillis;
    private final long initialDelayMillis;
    private final RepetitionType repetitionType;
    
    public SampleRepetitiveJob() {
        // Default values - will be overridden by external values
        this.jobId = "sample-repetitive-" + System.currentTimeMillis();
        this.jobName = "Sample Repetitive Job";
        this.intervalMillis = 5000; // 5 seconds
        this.initialDelayMillis = 1000; // 1 second initial delay
        this.repetitionType = RepetitionType.FIXED_DELAY;
    }
    
    @Override
    public void process() {
        logger.info("Starting sample repetitive job processing...");
        
        try {
            // Simulate some work
            Thread.sleep(1000);
            
            logger.info("Sample repetitive job execution completed!");
            logger.info("Job ID: {}, Job Name: {}, Execution Time: {}", 
                       jobId, jobName, System.currentTimeMillis());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Sample repetitive job was interrupted", e);
        } catch (Exception e) {
            logger.error("Error in sample repetitive job", e);
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
