package com.example.jobdispatcher.service;

import com.example.jobdispatcher.entity.ApiKey;
import com.example.jobdispatcher.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing API key authentication and authorization.
 */
@Service
@Transactional
public class ApiKeyService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyService.class);
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    /**
     * Validate an API key and return the associated ApiKey entity.
     */
    public Optional<ApiKey> validateApiKey(String apiKeyValue) {
        if (apiKeyValue == null || apiKeyValue.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKey(apiKeyValue.trim());
            
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                
                if (apiKey.isValid()) {
                    // Record usage
                    apiKey.recordUsage();
                    apiKeyRepository.save(apiKey);
                    
                    logger.debug("Valid API key used: {} (client: {})", apiKey.getKeyName(), apiKey.getClientId());
                    return Optional.of(apiKey);
                } else {
                    logger.warn("Invalid API key used: {} (active: {}, expired: {})", 
                               apiKeyValue, apiKey.getIsActive(), apiKey.isExpired());
                }
            } else {
                logger.warn("Unknown API key used: {}", apiKeyValue);
            }
            
        } catch (Exception e) {
            logger.error("Error validating API key", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Check if an API key is authorized for a specific job type.
     */
    public boolean isAuthorizedForJobType(String apiKeyValue, String jobType) {
        Optional<ApiKey> apiKeyOpt = validateApiKey(apiKeyValue);
        
        if (apiKeyOpt.isPresent()) {
            ApiKey apiKey = apiKeyOpt.get();
            return apiKey.isJobTypeAllowed(jobType);
        }
        
        return false;
    }
    
    /**
     * Create a new API key.
     */
    public ApiKey createApiKey(String keyName, String description, String createdBy) {
        ApiKey apiKey = new ApiKey(keyName, description, createdBy);
        ApiKey savedApiKey = apiKeyRepository.save(apiKey);
        
        logger.info("Created new API key: {} (client: {})", keyName, savedApiKey.getClientId());
        return savedApiKey;
    }
    
    /**
     * Create a new API key with custom settings.
     */
    public ApiKey createApiKey(String keyName, String description, String createdBy, 
                              Integer rateLimitPerMinute, Integer rateLimitPerHour, 
                              String[] allowedJobTypes, LocalDateTime expiresAt) {
        ApiKey apiKey = new ApiKey(keyName, description, createdBy);
        
        if (rateLimitPerMinute != null) {
            apiKey.setRateLimitPerMinute(rateLimitPerMinute);
        }
        if (rateLimitPerHour != null) {
            apiKey.setRateLimitPerHour(rateLimitPerHour);
        }
        if (allowedJobTypes != null) {
            apiKey.setAllowedJobTypesArray(allowedJobTypes);
        }
        if (expiresAt != null) {
            apiKey.setExpiresAt(expiresAt);
        }
        
        ApiKey savedApiKey = apiKeyRepository.save(apiKey);
        
        logger.info("Created new API key with custom settings: {} (client: {})", keyName, savedApiKey.getClientId());
        return savedApiKey;
    }
    
    /**
     * Revoke (deactivate) an API key.
     */
    public boolean revokeApiKey(String apiKeyValue) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKey(apiKeyValue);
            
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                apiKey.setIsActive(false);
                apiKeyRepository.save(apiKey);
                
                logger.info("Revoked API key: {} (client: {})", apiKey.getKeyName(), apiKey.getClientId());
                return true;
            }
            
        } catch (Exception e) {
            logger.error("Error revoking API key: {}", apiKeyValue, e);
        }
        
        return false;
    }
    
    /**
     * Regenerate an API key (creates new key value).
     */
    public Optional<ApiKey> regenerateApiKey(String apiKeyValue) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKey(apiKeyValue);
            
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                String oldKey = apiKey.getApiKey();
                
                // Generate new API key
                apiKey.setApiKey("jd_" + java.util.UUID.randomUUID().toString().replace("-", ""));
                ApiKey updatedApiKey = apiKeyRepository.save(apiKey);
                
                logger.info("Regenerated API key: {} (client: {})", apiKey.getKeyName(), apiKey.getClientId());
                return Optional.of(updatedApiKey);
            }
            
        } catch (Exception e) {
            logger.error("Error regenerating API key: {}", apiKeyValue, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get all active API keys.
     */
    public List<ApiKey> getAllActiveApiKeys() {
        return apiKeyRepository.findByIsActiveTrue();
    }
    
    /**
     * Get API key by client ID.
     */
    public Optional<ApiKey> getApiKeyByClientId(String clientId) {
        return apiKeyRepository.findByClientId(clientId);
    }
    
    /**
     * Get API key statistics.
     */
    public java.util.Map<String, Object> getApiKeyStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        long totalKeys = apiKeyRepository.count();
        long activeKeys = apiKeyRepository.countByIsActiveTrue();
        long expiredKeys = apiKeyRepository.findExpiredKeys(LocalDateTime.now()).size();
        
        stats.put("totalKeys", totalKeys);
        stats.put("activeKeys", activeKeys);
        stats.put("expiredKeys", expiredKeys);
        stats.put("inactiveKeys", totalKeys - activeKeys);
        
        // High usage keys (more than 1000 uses)
        List<ApiKey> highUsageKeys = apiKeyRepository.findHighUsageKeys(1000L);
        stats.put("highUsageKeys", highUsageKeys.size());
        
        // Recently created keys (last 30 days)
        List<ApiKey> recentKeys = apiKeyRepository.findRecentlyCreatedKeys(LocalDateTime.now().minusDays(30));
        stats.put("recentlyCreatedKeys", recentKeys.size());
        
        // Recently used keys (last 7 days)
        List<ApiKey> recentlyUsedKeys = apiKeyRepository.findRecentlyUsedKeys(LocalDateTime.now().minusDays(7));
        stats.put("recentlyUsedKeys", recentlyUsedKeys.size());
        
        return stats;
    }
    
    /**
     * Clean up expired API keys (deactivate them).
     */
    @Transactional
    public int cleanupExpiredKeys() {
        try {
            List<ApiKey> expiredKeys = apiKeyRepository.findExpiredKeys(LocalDateTime.now());
            
            for (ApiKey apiKey : expiredKeys) {
                apiKey.setIsActive(false);
                apiKeyRepository.save(apiKey);
            }
            
            logger.info("Cleaned up {} expired API keys", expiredKeys.size());
            return expiredKeys.size();
            
        } catch (Exception e) {
            logger.error("Error cleaning up expired API keys", e);
            return 0;
        }
    }
    
    /**
     * Get API key usage statistics for a specific key.
     */
    public java.util.Map<String, Object> getApiKeyUsageStats(String apiKeyValue) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKey(apiKeyValue);
            
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                
                stats.put("keyName", apiKey.getKeyName());
                stats.put("clientId", apiKey.getClientId());
                stats.put("isActive", apiKey.getIsActive());
                stats.put("isExpired", apiKey.isExpired());
                stats.put("usageCount", apiKey.getUsageCount());
                stats.put("lastUsedAt", apiKey.getLastUsedAt());
                stats.put("createdAt", apiKey.getCreatedAt());
                stats.put("expiresAt", apiKey.getExpiresAt());
                stats.put("rateLimitPerMinute", apiKey.getRateLimitPerMinute());
                stats.put("rateLimitPerHour", apiKey.getRateLimitPerHour());
                stats.put("allowedJobTypes", apiKey.getAllowedJobTypesArray());
                
            } else {
                stats.put("error", "API key not found");
            }
            
        } catch (Exception e) {
            logger.error("Error getting API key usage stats", e);
            stats.put("error", "Failed to retrieve statistics");
        }
        
        return stats;
    }
    
    /**
     * Save an API key.
     */
    public ApiKey saveApiKey(ApiKey apiKey) {
        return apiKeyRepository.save(apiKey);
    }
    
    /**
     * Get all API keys.
     */
    public List<ApiKey> getAllApiKeys() {
        return apiKeyRepository.findAll();
    }
    
    /**
     * Get API keys by app server ID.
     */
    public List<ApiKey> getApiKeysByAppServerId(String appServerId) {
        return apiKeyRepository.findByAppServerId(appServerId);
    }
    
    /**
     * Get API key by key ID.
     */
    public Optional<ApiKey> getApiKeyById(String keyId) {
        return apiKeyRepository.findByKeyId(keyId);
    }
    
    /**
     * Deactivate an API key.
     */
    public boolean deactivateApiKey(String keyId) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyId(keyId);
        if (apiKeyOpt.isPresent()) {
            ApiKey apiKey = apiKeyOpt.get();
            apiKey.setActive(false);
            apiKeyRepository.save(apiKey);
            logger.info("API key deactivated: {}", keyId);
            return true;
        }
        return false;
    }
    
    /**
     * Activate an API key.
     */
    public boolean activateApiKey(String keyId) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyId(keyId);
        if (apiKeyOpt.isPresent()) {
            ApiKey apiKey = apiKeyOpt.get();
            apiKey.setActive(true);
            apiKeyRepository.save(apiKey);
            logger.info("API key activated: {}", keyId);
            return true;
        }
        return false;
    }
    
    /**
     * Delete an API key permanently.
     */
    public boolean deleteApiKey(String keyId) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyId(keyId);
        if (apiKeyOpt.isPresent()) {
            apiKeyRepository.delete(apiKeyOpt.get());
            logger.info("API key deleted: {}", keyId);
            return true;
        }
        return false;
    }
    
    /**
     * Regenerate secret key for an API key.
     */
    public String regenerateSecretKey(String keyId) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyId(keyId);
        if (apiKeyOpt.isPresent()) {
            ApiKey apiKey = apiKeyOpt.get();
            String newSecretKey = generateSecretKey();
            apiKey.setSecretKey(newSecretKey);
            apiKeyRepository.save(apiKey);
            logger.info("Secret key regenerated for API key: {}", keyId);
            return newSecretKey;
        }
        return null;
    }
    
    /**
     * Generate a new secret key.
     */
    private String generateSecretKey() {
        return java.util.UUID.randomUUID().toString().replace("-", "") + 
               java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
