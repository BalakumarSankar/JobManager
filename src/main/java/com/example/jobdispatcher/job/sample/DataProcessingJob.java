package com.example.jobdispatcher.job.sample;

import com.example.jobdispatcher.job.OneTimeJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample data processing one-time job implementation.
 */
public class DataProcessingJob implements OneTimeJob {
    
    private static final Logger logger = LoggerFactory.getLogger(DataProcessingJob.class);
    
    private String jobId;
    private String jobName;
    
    public DataProcessingJob() {
        // Default values - will be overridden by external values
        this.jobId = "data-processing-" + System.currentTimeMillis();
        this.jobName = "Data Processing Job";
    }
    
    @Override
    public void process() {
        logger.info("Starting data processing job...");
        
        try {
            // Simulate data processing work
            for (int i = 1; i <= 5; i++) {
                logger.info("Processing batch {} of 5", i);
                Thread.sleep(500); // Simulate processing time
            }
            
            logger.info("Data processing job completed successfully!");
            logger.info("Processed 5 batches of data");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Data processing job was interrupted", e);
        } catch (Exception e) {
            logger.error("Error in data processing job", e);
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
