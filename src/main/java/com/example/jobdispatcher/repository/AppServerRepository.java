package com.example.jobdispatcher.repository;

import com.example.jobdispatcher.entity.AppServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AppServer entity operations.
 */
@Repository
public interface AppServerRepository extends JpaRepository<AppServer, Long> {
    
    /**
     * Find app server by name.
     */
    Optional<AppServer> findByName(String name);
    
    /**
     * Find all active app servers.
     */
    List<AppServer> findByIsActiveTrue();
    
    /**
     * Find app servers by health status.
     */
    List<AppServer> findByHealthStatus(String healthStatus);
    
    /**
     * Find active app servers by health status.
     */
    List<AppServer> findByIsActiveTrueAndHealthStatus(String healthStatus);
    
    /**
     * Find app servers by host and port.
     */
    Optional<AppServer> findByHostAndPort(String host, Integer port);
    
    /**
     * Check if app server exists by name.
     */
    boolean existsByName(String name);
    
    /**
     * Find app servers with last health check older than specified time.
     */
    @Query("SELECT as FROM AppServer as WHERE as.lastHealthCheck < :cutoffTime AND as.isActive = true")
    List<AppServer> findActiveServersWithOldHealthCheck(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Count active app servers by health status.
     */
    @Query("SELECT COUNT(as) FROM AppServer as WHERE as.healthStatus = :status AND as.isActive = true")
    long countActiveByHealthStatus(@Param("status") String healthStatus);
    
    /**
     * Find app servers by host.
     */
    List<AppServer> findByHost(String host);
    
    /**
     * Find app servers by port range.
     */
    @Query("SELECT as FROM AppServer as WHERE as.port BETWEEN :minPort AND :maxPort AND as.isActive = true")
    List<AppServer> findActiveServersByPortRange(@Param("minPort") Integer minPort, @Param("maxPort") Integer maxPort);
    
    // Additional methods for compatibility with existing code
    Optional<AppServer> findByAppServerId(String appServerId);
    
    Optional<AppServer> findByAppServerIdAndActiveTrue(String appServerId);
    
    long countByActiveTrue();
}
