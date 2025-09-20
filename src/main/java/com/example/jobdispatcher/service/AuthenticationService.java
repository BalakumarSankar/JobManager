package com.example.jobdispatcher.service;

import com.example.jobdispatcher.entity.AppServer;
import com.example.jobdispatcher.entity.ApiKey;
import com.example.jobdispatcher.repository.AppServerRepository;
import com.example.jobdispatcher.repository.ApiKeyRepository;
import com.example.jobdispatcher.config.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Simple and secure authentication service using JWT tokens.
 * Much better than complex handshake mechanisms.
 */
@Service
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    @Autowired
    private AppServerRepository appServerRepository;
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * Authenticate app server and generate JWT token.
     * Simple API key-based authentication.
     */
    public String authenticate(String apiKeyId, String apiKeySecret) {
        logger.info("Authenticating API key: {}", apiKeyId);
        
        // Find API key
        ApiKey apiKey = apiKeyRepository.findByKeyIdAndActiveTrue(apiKeyId).orElse(null);
        if (apiKey == null) {
            throw new SecurityException("Invalid API key: " + apiKeyId);
        }
        
        // Verify secret
        if (!apiKeySecret.equals(apiKey.getSecretKey())) {
            throw new SecurityException("Invalid API key secret for: " + apiKeyId);
        }
        
        // Get app server
        AppServer appServer = appServerRepository.findByAppServerIdAndActiveTrue(apiKey.getAppServerId()).orElse(null);
        if (appServer == null) {
            throw new SecurityException("App server not found or inactive: " + apiKey.getAppServerId());
        }
        
        // Update last authentication time
        appServer.setLastHandshakeAt(LocalDateTime.now());
        appServerRepository.save(appServer);
        
        // Generate JWT token
        String[] permissions = {"job:submit", "job:query", "job:status"};
        String token = jwtUtil.generateToken(appServer.getAppServerId(), apiKeyId, permissions);
        
        logger.info("Authentication successful for app server: {} (API key: {})", 
                   appServer.getAppServerId(), apiKeyId);
        
        return token;
    }
    
    /**
     * Validate JWT token.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        try {
            return jwtUtil.validateToken(token) && !jwtUtil.isTokenExpired(token);
        } catch (Exception e) {
            logger.warn("Token validation failed", e);
            return false;
        }
    }
    
    /**
     * Get app server ID from token.
     */
    public String getAppServerIdFromToken(String token) {
        try {
            return jwtUtil.getAppServerIdFromToken(token);
        } catch (Exception e) {
            logger.warn("Failed to extract app server ID from token", e);
            return null;
        }
    }
    
    /**
     * Get API key ID from token.
     */
    public String getApiKeyIdFromToken(String token) {
        try {
            return jwtUtil.getApiKeyIdFromToken(token);
        } catch (Exception e) {
            logger.warn("Failed to extract API key ID from token", e);
            return null;
        }
    }
    
    /**
     * Check if token has specific permission.
     */
    public boolean hasPermission(String token, String permission) {
        try {
            String[] permissions = jwtUtil.getPermissionsFromToken(token);
            for (String perm : permissions) {
                if (perm.equals(permission)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.warn("Failed to check permissions", e);
            return false;
        }
    }
    
    /**
     * Get authentication statistics.
     */
    public Map<String, Object> getAuthStats() {
        long activeAppServers = appServerRepository.countByActiveTrue();
        long activeApiKeys = apiKeyRepository.countByActiveTrue();
        
        return Map.of(
            "activeAppServers", activeAppServers,
            "activeApiKeys", activeApiKeys,
            "timestamp", System.currentTimeMillis()
        );
    }
}
