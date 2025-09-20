package com.example.jobdispatcher.model;

/**
 * Response model for job submission operations.
 */
public class JobSubmissionResponse {
    
    private String jobId;
    private String status;
    private String message;
    private long timestamp;
    
    public JobSubmissionResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public JobSubmissionResponse(String jobId, String status, String message) {
        this.jobId = jobId;
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
