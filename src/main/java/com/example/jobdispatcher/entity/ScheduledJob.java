package com.example.jobdispatcher.entity;

import com.example.jobdispatcher.enums.JobPriority;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Entity representing a scheduled job with tracking details.
 */
@Entity
@Table(name = "scheduled_jobs")
public class ScheduledJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "job_id", nullable = false, unique = true)
    private String jobId;
    
    @NotBlank
    @Column(name = "job_name", nullable = false)
    private String jobName;
    
    @NotBlank
    @Column(name = "job_class_name", nullable = false)
    private String jobClassName;
    
    @NotBlank
    @Column(name = "job_type", nullable = false)
    private String jobType; // ONE_TIME, REPETITIVE
    
    @Column(name = "group_key")
    private String groupKey;
    
    @Column(name = "can_group", nullable = false)
    private Boolean canGroup = false;
    
    @Column(name = "group_buffer_millis")
    private Long groupBufferMillis;
    
    @Column(name = "interval_millis")
    private Long intervalMillis;
    
    @Column(name = "initial_delay_millis")
    private Long initialDelayMillis;
    
    @Column(name = "repetition_type")
    private String repetitionType; // FIXED_DELAY, FIXED_RATE, CRON
    
    @Column(name = "cron_expression")
    private String cronExpression; // Used when repetitionType == CRON
    
    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 5; // 1-10, 1 being highest priority
    
    @Enumerated(EnumType.STRING)
    @Column(name = "job_priority")
    private JobPriority jobPriority = JobPriority.NORMAL; // All jobs have equal priority (NORMAL)
    
    // Timeout handling removed - jobs can run as long as needed
    
    @Column(name = "retry_enabled", nullable = false)
    private Boolean retryEnabled = true;
    
    @Column(name = "max_retry_attempts", nullable = false)
    private Integer maxRetryAttempts = 3;
    
    @Column(name = "retry_delay_millis")
    private Long retryDelayMillis;
    
    @Column(name = "retry_multiplier")
    private Double retryMultiplier = 2.0;
    
    @Column(name = "retry_max_delay_millis")
    private Long retryMaxDelayMillis = 30000L;
    
    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    @Column(name = "retry_reason")
    private String retryReason;
    
    @Column(name = "metadata")
    private String metadata; // JSON string for additional job data
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_pool_id")
    private ThreadPool threadPool;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_server_id")
    private AppServer appServer;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ScheduledJob() {
        this.createdAt = LocalDateTime.now();
        this.submittedAt = LocalDateTime.now();
    }
    
    public ScheduledJob(String jobId, String jobName, String jobClassName, String jobType) {
        this();
        this.jobId = jobId;
        this.jobName = jobName;
        this.jobClassName = jobClassName;
        this.jobType = jobType;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getJobType() {
        return jobType;
    }
    
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
    
    public String getGroupKey() {
        return groupKey;
    }
    
    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }
    
    public Boolean getCanGroup() {
        return canGroup;
    }
    
    public void setCanGroup(Boolean canGroup) {
        this.canGroup = canGroup;
    }
    
    public Long getGroupBufferMillis() {
        return groupBufferMillis;
    }
    
    public void setGroupBufferMillis(Long groupBufferMillis) {
        this.groupBufferMillis = groupBufferMillis;
    }
    
    public Long getIntervalMillis() {
        return intervalMillis;
    }
    
    public void setIntervalMillis(Long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }
    
    public Long getInitialDelayMillis() {
        return initialDelayMillis;
    }
    
    public void setInitialDelayMillis(Long initialDelayMillis) {
        this.initialDelayMillis = initialDelayMillis;
    }
    
    public String getRepetitionType() {
        return repetitionType;
    }
    
    public void setRepetitionType(String repetitionType) {
        this.repetitionType = repetitionType;
    }
    
    public String getCronExpression() {
        return cronExpression;
    }
    
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public JobPriority getJobPriority() {
        return jobPriority;
    }
    
    public void setJobPriority(JobPriority jobPriority) {
        this.jobPriority = jobPriority;
    }
    
    // Timeout handling removed - jobs can run as long as needed
    
    public Boolean getRetryEnabled() {
        return retryEnabled;
    }
    
    public void setRetryEnabled(Boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }
    
    public Integer getMaxRetryAttempts() {
        return maxRetryAttempts;
    }
    
    public void setMaxRetryAttempts(Integer maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }
    
    public Long getRetryDelayMillis() {
        return retryDelayMillis;
    }
    
    public void setRetryDelayMillis(Long retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
    }
    
    public Double getRetryMultiplier() {
        return retryMultiplier;
    }
    
    public void setRetryMultiplier(Double retryMultiplier) {
        this.retryMultiplier = retryMultiplier;
    }
    
    public Long getRetryMaxDelayMillis() {
        return retryMaxDelayMillis;
    }
    
    public void setRetryMaxDelayMillis(Long retryMaxDelayMillis) {
        this.retryMaxDelayMillis = retryMaxDelayMillis;
    }
    
    public LocalDateTime getLastRetryAt() {
        return lastRetryAt;
    }
    
    public void setLastRetryAt(LocalDateTime lastRetryAt) {
        this.lastRetryAt = lastRetryAt;
    }
    
    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }
    
    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }
    
    public String getRetryReason() {
        return retryReason;
    }
    
    public void setRetryReason(String retryReason) {
        this.retryReason = retryReason;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public ThreadPool getThreadPool() {
        return threadPool;
    }
    
    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }
    
    public AppServer getAppServer() {
        return appServer;
    }
    
    public void setAppServer(AppServer appServer) {
        this.appServer = appServer;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public void markAsStarted() {
        this.status = "RUNNING";
        this.startedAt = LocalDateTime.now();
    }
    
    public void markAsCompleted() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.executionTimeMs = java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
        }
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        if (this.startedAt != null) {
            this.executionTimeMs = java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
        }
    }
    
    public void markAsCancelled() {
        this.status = "CANCELLED";
        this.completedAt = LocalDateTime.now();
    }
    
    public boolean canRetry() {
        return this.retryEnabled && 
               this.retryCount < this.maxRetryAttempts && 
               "FAILED".equals(this.status);
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
        this.status = "PENDING";
        this.startedAt = null;
        this.completedAt = null;
        this.errorMessage = null;
        this.lastRetryAt = LocalDateTime.now();
    }
    
    public void scheduleNextRetry(long delayMillis) {
        this.nextRetryAt = LocalDateTime.now().plusNanos(delayMillis * 1_000_000);
    }
    
    public boolean isRetryScheduled() {
        return this.nextRetryAt != null && this.nextRetryAt.isAfter(LocalDateTime.now());
    }
    
    public boolean isRetryDue() {
        return this.nextRetryAt != null && this.nextRetryAt.isBefore(LocalDateTime.now());
    }
    
    // Timeout handling removed - jobs can run as long as needed
}
