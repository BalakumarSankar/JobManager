package com.example.jobdispatcher.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a thread pool configuration.
 */
@Entity
@Table(name = "thread_pools")
public class ThreadPool {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    
    @NotBlank
    @Column(name = "type", nullable = false)
    private String type; // ONE_TIME, REPETITIVE, ASYNC
    
    @NotNull
    @Positive
    @Column(name = "core_pool_size", nullable = false)
    private Integer corePoolSize;
    
    @NotNull
    @Positive
    @Column(name = "max_pool_size", nullable = false)
    private Integer maxPoolSize;
    
    @NotNull
    @Positive
    @Column(name = "keep_alive_time", nullable = false)
    private Long keepAliveTime;
    
    @NotNull
    @Positive
    @Column(name = "queue_capacity", nullable = false)
    private Integer queueCapacity;
    
    @NotBlank
    @Column(name = "thread_name_prefix", nullable = false)
    private String threadNamePrefix;
    
    @NotBlank
    @Column(name = "queue_type", nullable = false)
    private String queueType; // LINKED_BLOCKING_QUEUE, ARRAY_BLOCKING_QUEUE, SYNCHRONOUS_QUEUE
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "threadPool", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ScheduledJob> scheduledJobs;
    
    // Constructors
    public ThreadPool() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ThreadPool(String name, String type, Integer corePoolSize, Integer maxPoolSize, 
                     Long keepAliveTime, Integer queueCapacity, String threadNamePrefix, String queueType) {
        this();
        this.name = name;
        this.type = type;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.queueCapacity = queueCapacity;
        this.threadNamePrefix = threadNamePrefix;
        this.queueType = queueType;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Integer getCorePoolSize() {
        return corePoolSize;
    }
    
    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }
    
    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }
    
    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }
    
    public Long getKeepAliveTime() {
        return keepAliveTime;
    }
    
    public void setKeepAliveTime(Long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }
    
    public Integer getQueueCapacity() {
        return queueCapacity;
    }
    
    public void setQueueCapacity(Integer queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
    
    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }
    
    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }
    
    public String getQueueType() {
        return queueType;
    }
    
    public void setQueueType(String queueType) {
        this.queueType = queueType;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
    
    public List<ScheduledJob> getScheduledJobs() {
        return scheduledJobs;
    }
    
    public void setScheduledJobs(List<ScheduledJob> scheduledJobs) {
        this.scheduledJobs = scheduledJobs;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

