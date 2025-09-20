package com.example.jobdispatcher.test;

import com.example.jobdispatcher.config.JwtUtil;
import com.example.jobdispatcher.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify JWT authentication functionality.
 */
@SpringBootTest
public class JwtAuthenticationTest {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Test
    public void testJwtTokenGeneration() {
        // Test JWT token generation
        String appServerId = "test-server-1";
        String apiKeyId = "test-key-1";
        String[] permissions = {"job:submit", "job:query"};
        
        String token = jwtUtil.generateToken(appServerId, apiKeyId, permissions);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
        
        // Test token validation
        assertTrue(jwtUtil.validateToken(token));
        
        // Test token content extraction
        assertEquals(appServerId, jwtUtil.getAppServerIdFromToken(token));
        assertEquals(apiKeyId, jwtUtil.getApiKeyIdFromToken(token));
        
        String[] extractedPermissions = jwtUtil.getPermissionsFromToken(token);
        assertArrayEquals(permissions, extractedPermissions);
    }
    
    @Test
    public void testInvalidToken() {
        // Test invalid token
        String invalidToken = "invalid.token.here";
        
        assertFalse(jwtUtil.validateToken(invalidToken));
        assertNull(jwtUtil.getAppServerIdFromToken(invalidToken));
        assertNull(jwtUtil.getApiKeyIdFromToken(invalidToken));
    }
    
    @Test
    public void testAuthenticationService() {
        // Test authentication service methods
        assertNotNull(authenticationService);
        
        // Test with null token
        assertFalse(authenticationService.validateToken(null));
        assertFalse(authenticationService.validateToken(""));
        
        // Test statistics
        var stats = authenticationService.getAuthStats();
        assertNotNull(stats);
        assertTrue(stats.containsKey("timestamp"));
    }
}

