package com.example.jobdispatcher.job;

/**
 * Base interface for all jobs that can be executed by the job dispatcher.
 * All job implementations must implement this interface and provide a process method.
 */
public interface Job {
    
    /**
     * The main method that will be executed when the job is dispatched.
     * This method should contain the actual business logic for the job.
     */
    void process();
    
    /**
     * Get the unique identifier for this job.
     * @return the job ID
     */
    String getJobId();
    
    /**
     * Get the name/description of this job.
     * @return the job name
     */
    String getJobName();
    
    /**
     * Set the external job ID from the request.
     * @param jobId the external job ID
     */
    void setJobId(String jobId);
    
    /**
     * Set the external job name from the request.
     * @param jobName the external job name
     */
    void setJobName(String jobName);
}
