package com.example.jobdispatcher.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing an application server configuration.
 */
@Entity
@Table(name = "app_servers")
public class AppServer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    
    @NotBlank
    @Column(name = "host", nullable = false)
    private String host;
    
    @NotNull
    @Column(name = "port", nullable = false)
    private Integer port;
    
    @Column(name = "context_path")
    private String contextPath = "";
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "health_check_url")
    private String healthCheckUrl;
    
    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;
    
    @Column(name = "health_status")
    private String healthStatus = "UNKNOWN"; // UP, DOWN, UNKNOWN
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "appServer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ScheduledJob> scheduledJobs;
    
    // Constructors
    public AppServer() {
        this.createdAt = LocalDateTime.now();
    }
    
    public AppServer(String name, String host, Integer port) {
        this();
        this.name = name;
        this.host = host;
        this.port = port;
    }
    
    public AppServer(String name, String host, Integer port, String contextPath, String description) {
        this(name, host, port);
        this.contextPath = contextPath;
        this.description = description;
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
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public Integer getPort() {
        return port;
    }
    
    public void setPort(Integer port) {
        this.port = port;
    }
    
    public String getContextPath() {
        return contextPath;
    }
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
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
    
    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }
    
    public void setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
    }
    
    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }
    
    public void setLastHealthCheck(LocalDateTime lastHealthCheck) {
        this.lastHealthCheck = lastHealthCheck;
    }
    
    public String getHealthStatus() {
        return healthStatus;
    }
    
    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
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
    
    // Helper methods
    public String getBaseUrl() {
        return "http://" + host + ":" + port + (contextPath != null ? contextPath : "");
    }
    
    public String getFullHealthCheckUrl() {
        if (healthCheckUrl != null && !healthCheckUrl.isEmpty()) {
            return healthCheckUrl.startsWith("http") ? healthCheckUrl : getBaseUrl() + healthCheckUrl;
        }
        return getBaseUrl() + "/health";
    }
    
    // Additional methods for compatibility with existing code
    public String getAppServerId() {
        return this.name;
    }
    
    public void setAppServerId(String appServerId) {
        this.name = appServerId;
    }
    
    public void setLastHandshakeAt(LocalDateTime lastHandshakeAt) {
        this.lastHealthCheck = lastHandshakeAt;
    }
    
    public LocalDateTime getLastHandshakeAt() {
        return this.lastHealthCheck;
    }
}
