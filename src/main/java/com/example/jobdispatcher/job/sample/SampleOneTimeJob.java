package com.example.jobdispatcher.job.sample;

import com.example.jobdispatcher.job.OneTimeJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample one-time job implementation for testing purposes.
 */
public class SampleOneTimeJob implements OneTimeJob {
    
    private static final Logger logger = LoggerFactory.getLogger(SampleOneTimeJob.class);
    
    private String jobId;
    private String jobName;
    
    public SampleOneTimeJob() {
        // Default values - will be overridden by external values
        this.jobId = "sample-onetime-" + System.currentTimeMillis();
        this.jobName = "Sample One-Time Job";
    }
    
    @Override
    public void process() {
        logger.info("Starting sample one-time job processing...");
        
        try {
            // Simulate some work
            Thread.sleep(2000);
            
            logger.info("Sample one-time job completed successfully!");
            logger.info("Job ID: {}, Job Name: {}", jobId, jobName);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Sample one-time job was interrupted", e);
        } catch (Exception e) {
            logger.error("Error in sample one-time job", e);
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
}
