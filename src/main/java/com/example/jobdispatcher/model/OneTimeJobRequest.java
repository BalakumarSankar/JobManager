package com.example.jobdispatcher.model;

import com.example.jobdispatcher.job.RepetitiveJob;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * Request model for submitting a one-time job.
 */
public class OneTimeJobRequest {
    
    @NotBlank(message = "Job ID is required")
    private String jobId;
    
    @NotBlank(message = "Job name is required")
    private String jobName;
    
    @NotBlank(message = "Job class name is required")
    private String jobClassName;
    
    // Job grouping fields
    private String groupKey;
    private boolean canGroup = false;
    private long groupBufferMillis = 5000; // 5 seconds default buffer
    
    public OneTimeJobRequest() {}
    
    public OneTimeJobRequest(String jobId, String jobName, String jobClassName) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.jobClassName = jobClassName;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    public String getJobName() {
        return jobName;
    }
    
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
    
    public String getJobClassName() {
        return jobClassName;
    }
    
    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }
    
    public String getGroupKey() {
        return groupKey;
    }
    
    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }
    
    public boolean isCanGroup() {
        return canGroup;
    }
    
    public void setCanGroup(boolean canGroup) {
        this.canGroup = canGroup;
    }
    
    public long getGroupBufferMillis() {
        return groupBufferMillis;
    }
    
    public void setGroupBufferMillis(long groupBufferMillis) {
        this.groupBufferMillis = groupBufferMillis;
    }
}
