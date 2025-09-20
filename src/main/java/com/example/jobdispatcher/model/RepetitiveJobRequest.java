package com.example.jobdispatcher.model;

import com.example.jobdispatcher.job.RepetitiveJob;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * Request model for submitting a repetitive job.
 */
public class RepetitiveJobRequest {
    
    @NotBlank(message = "Job ID is required")
    private String jobId;
    
    @NotBlank(message = "Job name is required")
    private String jobName;
    
    @NotBlank(message = "Job class name is required")
    private String jobClassName;
    
    @Positive(message = "Interval must be positive")
    private long intervalMillis;
    
    @Positive(message = "Initial delay must be positive")
    private long initialDelayMillis = 0;
    
    @NotNull(message = "Repetition type is required")
    private RepetitiveJob.RepetitionType repetitionType = RepetitiveJob.RepetitionType.FIXED_DELAY;
    
    // Optional cron expression when repetitionType is CRON
    private String cronExpression;
    
    // Job grouping fields
    private String groupKey;
    private boolean canGroup = false;
    private long groupBufferMillis = 5000; // 5 seconds default buffer
    
    public RepetitiveJobRequest() {}
    
    public RepetitiveJobRequest(String jobId, String jobName, String jobClassName, 
                               long intervalMillis, long initialDelayMillis, 
                               RepetitiveJob.RepetitionType repetitionType) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.jobClassName = jobClassName;
        this.intervalMillis = intervalMillis;
        this.initialDelayMillis = initialDelayMillis;
        this.repetitionType = repetitionType;
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
    
    public long getIntervalMillis() {
        return intervalMillis;
    }
    
    public void setIntervalMillis(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }
    
    public long getInitialDelayMillis() {
        return initialDelayMillis;
    }
    
    public void setInitialDelayMillis(long initialDelayMillis) {
        this.initialDelayMillis = initialDelayMillis;
    }
    
    public RepetitiveJob.RepetitionType getRepetitionType() {
        return repetitionType;
    }
    
    public void setRepetitionType(RepetitiveJob.RepetitionType repetitionType) {
        this.repetitionType = repetitionType;
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

    public String getCronExpression() {
        return cronExpression;
    }
    
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}
