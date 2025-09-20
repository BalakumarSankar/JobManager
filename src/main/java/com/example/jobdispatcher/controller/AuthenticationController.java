package com.example.jobdispatcher.controller;

import com.example.jobdispatcher.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simple authentication controller using JWT tokens.
 * Much simpler than complex handshake mechanisms.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
    @Autowired
    private AuthenticationService authenticationService;
    
    /**
     * Authenticate app server and get JWT token.
     * Simple API key-based authentication.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String apiKeyId,
            @RequestParam String apiKeySecret) {
        
        try {
            String token = authenticationService.authenticate(apiKeyId, apiKeySecret);
            
            return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "tokenType", "Bearer",
                "expiresIn", 3600, // 1 hour
                "message", "Authentication successful"
            ));
            
        } catch (SecurityException e) {
            logger.warn("Authentication failed for API key: {}", apiKeyId);
            return ResponseEntity.status(401).body(Map.of(
                "error", "Authentication failed",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Unexpected error during authentication", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", "Authentication service unavailable"
            ));
        }
    }
    
    /**
     * Validate JWT token.
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        try {
            boolean isValid = authenticationService.validateToken(token);
            
            if (isValid) {
                String appServerId = authenticationService.getAppServerIdFromToken(token);
                String apiKeyId = authenticationService.getApiKeyIdFromToken(token);
                
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "appServerId", appServerId,
                    "apiKeyId", apiKeyId,
                    "message", "Token is valid"
                ));
            } else {
                return ResponseEntity.status(401).body(Map.of(
                    "valid", false,
                    "message", "Token is invalid or expired"
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error validating token", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Token validation failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get authentication statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAuthStats() {
        try {
            Map<String, Object> stats = authenticationService.getAuthStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting auth stats", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get authentication statistics",
                "message", e.getMessage()
            ));
        }
    }
}

