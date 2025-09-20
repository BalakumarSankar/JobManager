package com.example.jobdispatcher.repository;

import com.example.jobdispatcher.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ApiKey entity operations.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    /**
     * Find API key by the actual key value.
     */
    Optional<ApiKey> findByApiKey(String apiKey);
    
    /**
     * Find API key by client ID.
     */
    Optional<ApiKey> findByClientId(String clientId);
    
    /**
     * Find API key by key name.
     */
    Optional<ApiKey> findByKeyName(String keyName);
    
    /**
     * Find all active API keys.
     */
    List<ApiKey> findByIsActiveTrue();
    
    /**
     * Find all API keys created by a specific user.
     */
    List<ApiKey> findByCreatedBy(String createdBy);
    
    /**
     * Find API keys that are expired.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.expiresAt < :currentTime AND ak.isActive = true")
    List<ApiKey> findExpiredKeys(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find API keys that expire soon (within specified days).
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.expiresAt BETWEEN :currentTime AND :expiryTime AND ak.isActive = true")
    List<ApiKey> findKeysExpiringSoon(@Param("currentTime") LocalDateTime currentTime, 
                                     @Param("expiryTime") LocalDateTime expiryTime);
    
    /**
     * Find API keys that haven't been used recently.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.lastUsedAt < :cutoffTime AND ak.isActive = true")
    List<ApiKey> findUnusedKeys(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Count active API keys.
     */
    long countByIsActiveTrue();
    
    /**
     * Count API keys by creator.
     */
    long countByCreatedBy(String createdBy);
    
    /**
     * Check if API key exists by key value.
     */
    boolean existsByApiKey(String apiKey);
    
    /**
     * Check if API key exists by client ID.
     */
    boolean existsByClientId(String clientId);
    
    /**
     * Check if API key exists by key name.
     */
    boolean existsByKeyName(String keyName);
    
    /**
     * Find API keys with high usage count.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.usageCount > :minUsage AND ak.isActive = true ORDER BY ak.usageCount DESC")
    List<ApiKey> findHighUsageKeys(@Param("minUsage") Long minUsage);
    
    /**
     * Find API keys created in the last N days.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.createdAt >= :startDate ORDER BY ak.createdAt DESC")
    List<ApiKey> findRecentlyCreatedKeys(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Find API keys used in the last N days.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.lastUsedAt >= :startDate AND ak.isActive = true ORDER BY ak.lastUsedAt DESC")
    List<ApiKey> findRecentlyUsedKeys(@Param("startDate") LocalDateTime startDate);
    
    // Additional methods for compatibility with existing code
    Optional<ApiKey> findByKeyId(String keyId);
    
    Optional<ApiKey> findByKeyIdAndActiveTrue(String keyId);
    
    List<ApiKey> findByAppServerId(String appServerId);
    
    long countByActiveTrue();
}
