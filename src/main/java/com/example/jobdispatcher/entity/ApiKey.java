package com.example.jobdispatcher.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an API key for authentication.
 */
@Entity
@Table(name = "api_keys")
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "key_name", nullable = false)
    private String keyName;
    
    @NotBlank
    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;
    
    @NotBlank
    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;
    
    @Column(name = "rate_limit_per_minute", nullable = false)
    private Integer rateLimitPerMinute = 60;
    
    @Column(name = "rate_limit_per_hour", nullable = false)
    private Integer rateLimitPerHour = 1000;
    
    @Column(name = "allowed_job_types")
    private String allowedJobTypes; // Comma-separated: ONE_TIME,REPETITIVE
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ApiKey() {
        this.createdAt = LocalDateTime.now();
        this.clientId = UUID.randomUUID().toString();
    }
    
    public ApiKey(String keyName, String description) {
        this();
        this.keyName = keyName;
        this.description = description;
        this.apiKey = generateApiKey();
    }
    
    public ApiKey(String keyName, String description, String createdBy) {
        this(keyName, description);
        this.createdBy = createdBy;
    }
    
    /**
     * Generate a new API key.
     */
    private String generateApiKey() {
        return "jd_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Check if the API key is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * Check if the API key is valid (active and not expired).
     */
    public boolean isValid() {
        return isActive && !isExpired();
    }
    
    /**
     * Increment usage count and update last used timestamp.
     */
    public void recordUsage() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }
    
    /**
     * Check if a job type is allowed for this API key.
     */
    public boolean isJobTypeAllowed(String jobType) {
        if (allowedJobTypes == null || allowedJobTypes.trim().isEmpty()) {
            return true; // No restrictions
        }
        
        String[] allowedTypes = allowedJobTypes.split(",");
        for (String allowedType : allowedTypes) {
            if (allowedType.trim().equalsIgnoreCase(jobType)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Set allowed job types from array.
     */
    public void setAllowedJobTypesArray(String[] jobTypes) {
        if (jobTypes != null && jobTypes.length > 0) {
            this.allowedJobTypes = String.join(",", jobTypes);
        } else {
            this.allowedJobTypes = null;
        }
    }
    
    /**
     * Get allowed job types as array.
     */
    public String[] getAllowedJobTypesArray() {
        if (allowedJobTypes == null || allowedJobTypes.trim().isEmpty()) {
            return new String[0];
        }
        return allowedJobTypes.split(",");
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getKeyName() {
        return keyName;
    }
    
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }
    
    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
    
    public Long getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }
    
    public Integer getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }
    
    public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }
    
    public Integer getRateLimitPerHour() {
        return rateLimitPerHour;
    }
    
    public void setRateLimitPerHour(Integer rateLimitPerHour) {
        this.rateLimitPerHour = rateLimitPerHour;
    }
    
    public String getAllowedJobTypes() {
        return allowedJobTypes;
    }
    
    public void setAllowedJobTypes(String allowedJobTypes) {
        this.allowedJobTypes = allowedJobTypes;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
    
    // Additional methods for compatibility with existing code
    public String getKeyId() {
        return this.apiKey;
    }
    
    public void setKeyId(String keyId) {
        this.apiKey = keyId;
    }
    
    public String getSecretKey() {
        return this.clientId;
    }
    
    public void setSecretKey(String secretKey) {
        this.clientId = secretKey;
    }
    
    public String getAppServerId() {
        return this.keyName; // Using keyName as appServerId for now
    }
    
    public void setAppServerId(String appServerId) {
        this.keyName = appServerId;
    }
    
    public boolean isActive() {
        return this.isActive != null ? this.isActive : false;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
}
