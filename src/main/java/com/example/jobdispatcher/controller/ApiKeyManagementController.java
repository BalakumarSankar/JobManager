package com.example.jobdispatcher.controller;

import com.example.jobdispatcher.entity.ApiKey;
import com.example.jobdispatcher.entity.AppServer;
import com.example.jobdispatcher.service.ApiKeyService;
import com.example.jobdispatcher.service.DatabasePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for API key management operations.
 */
@RestController
@RequestMapping("/api/admin")
public class ApiKeyManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyManagementController.class);
    
    @Autowired
    private ApiKeyService apiKeyService;
    
    @Autowired
    private DatabasePersistenceService databasePersistenceService;
    
    /**
     * Create a new API key for an app server.
     */
    @PostMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> createApiKey(
            @RequestParam String appServerId,
            @RequestParam(required = false) String description) {
        
        try {
            logger.info("Creating API key for app server: {}", appServerId);
            
            // Verify app server exists
            AppServer appServer = databasePersistenceService.findAppServerByAppServerId(appServerId);
            if (appServer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "App server not found", "appServerId", appServerId));
            }
            
            // Generate API key
            String keyId = "ak_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String secretKey = UUID.randomUUID().toString().replace("-", "") + 
                              UUID.randomUUID().toString().replace("-", "");
            
            ApiKey apiKey = new ApiKey();
            apiKey.setKeyId(keyId);
            apiKey.setSecretKey(secretKey);
            apiKey.setAppServerId(appServerId);
            apiKey.setDescription(description != null ? description : "Generated API key");
            apiKey.setActive(true);
            apiKey.setCreatedAt(LocalDateTime.now());
            apiKey.setLastUsedAt(null);
            
            ApiKey savedApiKey = apiKeyService.saveApiKey(apiKey);
            
            logger.info("API key created successfully: {} for app server: {}", keyId, appServerId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "keyId", savedApiKey.getKeyId(),
                "secretKey", savedApiKey.getSecretKey(),
                "appServerId", savedApiKey.getAppServerId(),
                "description", savedApiKey.getDescription(),
                "createdAt", savedApiKey.getCreatedAt(),
                "message", "API key created successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error creating API key for app server: {}", appServerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create API key", "message", e.getMessage()));
        }
    }
    
    /**
     * Get all API keys.
     */
    @GetMapping("/api-keys")
    public ResponseEntity<List<ApiKey>> getAllApiKeys() {
        try {
            List<ApiKey> apiKeys = apiKeyService.getAllApiKeys();
            return ResponseEntity.ok(apiKeys);
        } catch (Exception e) {
            logger.error("Error retrieving API keys", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get API keys by app server ID.
     */
    @GetMapping("/api-keys/app-server/{appServerId}")
    public ResponseEntity<List<ApiKey>> getApiKeysByAppServer(@PathVariable String appServerId) {
        try {
            List<ApiKey> apiKeys = apiKeyService.getApiKeysByAppServerId(appServerId);
            return ResponseEntity.ok(apiKeys);
        } catch (Exception e) {
            logger.error("Error retrieving API keys for app server: {}", appServerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get API key by key ID.
     */
    @GetMapping("/api-keys/{keyId}")
    public ResponseEntity<ApiKey> getApiKeyById(@PathVariable String keyId) {
        try {
            return apiKeyService.getApiKeyById(keyId)
                .map(apiKey -> ResponseEntity.ok(apiKey))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error retrieving API key: {}", keyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Deactivate an API key.
     */
    @PutMapping("/api-keys/{keyId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateApiKey(@PathVariable String keyId) {
        try {
            logger.info("Deactivating API key: {}", keyId);
            
            boolean deactivated = apiKeyService.deactivateApiKey(keyId);
            
            if (deactivated) {
                return ResponseEntity.ok(Map.of(
                    "keyId", keyId,
                    "message", "API key deactivated successfully",
                    "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "API key not found", "keyId", keyId));
            }
            
        } catch (Exception e) {
            logger.error("Error deactivating API key: {}", keyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to deactivate API key", "message", e.getMessage()));
        }
    }
    
    /**
     * Reactivate an API key.
     */
    @PutMapping("/api-keys/{keyId}/activate")
    public ResponseEntity<Map<String, Object>> activateApiKey(@PathVariable String keyId) {
        try {
            logger.info("Reactivating API key: {}", keyId);
            
            boolean activated = apiKeyService.activateApiKey(keyId);
            
            if (activated) {
                return ResponseEntity.ok(Map.of(
                    "keyId", keyId,
                    "message", "API key activated successfully",
                    "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "API key not found", "keyId", keyId));
            }
            
        } catch (Exception e) {
            logger.error("Error activating API key: {}", keyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to activate API key", "message", e.getMessage()));
        }
    }
    
    /**
     * Delete an API key permanently.
     */
    @DeleteMapping("/api-keys/{keyId}")
    public ResponseEntity<Map<String, Object>> deleteApiKey(@PathVariable String keyId) {
        try {
            logger.info("Deleting API key: {}", keyId);
            
            boolean deleted = apiKeyService.deleteApiKey(keyId);
            
            if (deleted) {
                return ResponseEntity.ok(Map.of(
                    "keyId", keyId,
                    "message", "API key deleted successfully",
                    "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "API key not found", "keyId", keyId));
            }
            
        } catch (Exception e) {
            logger.error("Error deleting API key: {}", keyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete API key", "message", e.getMessage()));
        }
    }
    
    /**
     * Regenerate secret key for an API key.
     */
    @PutMapping("/api-keys/{keyId}/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateSecretKey(@PathVariable String keyId) {
        try {
            logger.info("Regenerating secret key for API key: {}", keyId);
            
            String newSecretKey = apiKeyService.regenerateSecretKey(keyId);
            
            if (newSecretKey != null) {
                return ResponseEntity.ok(Map.of(
                    "keyId", keyId,
                    "secretKey", newSecretKey,
                    "message", "Secret key regenerated successfully",
                    "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "API key not found", "keyId", keyId));
            }
            
        } catch (Exception e) {
            logger.error("Error regenerating secret key for API key: {}", keyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to regenerate secret key", "message", e.getMessage()));
        }
    }
    
    /**
     * Get API key usage statistics.
     */
    @GetMapping("/api-keys/stats")
    public ResponseEntity<Map<String, Object>> getApiKeyStats() {
        try {
            Map<String, Object> stats = apiKeyService.getApiKeyStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error retrieving API key statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve API key statistics", "message", e.getMessage()));
        }
    }
}

