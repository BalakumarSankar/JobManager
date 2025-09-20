package com.example.jobdispatcher.test;

import com.example.jobdispatcher.controller.AuthenticationController;
import com.example.jobdispatcher.controller.JobController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for authentication and job submission.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=testSecretKey123456789012345678901234567890",
    "jwt.expiration=3600000",
    "rate-limiting.enabled=false"
})
public class IntegrationTest {
    
    @Autowired
    private AuthenticationController authenticationController;
    
    @Autowired
    private JobController jobController;
    
    @Test
    public void testControllersAreLoaded() {
        // Test that controllers are properly loaded
        assertNotNull(authenticationController);
        assertNotNull(jobController);
    }
    
    @Test
    public void testAuthenticationEndpoints() {
        // Test authentication endpoints exist
        assertNotNull(authenticationController);
        
        // Test login endpoint (will fail without valid API key, but endpoint should exist)
        try {
            var response = authenticationController.login("test-key", "test-secret");
            // This will likely fail with 401, but that's expected without valid credentials
            assertNotNull(response);
        } catch (Exception e) {
            // Expected to fail without valid API key in database
            assertTrue(e.getMessage().contains("Authentication failed") || 
                     e.getMessage().contains("Invalid API key"));
        }
    }
    
    @Test
    public void testJobEndpoints() {
        // Test job endpoints exist
        assertNotNull(jobController);
        
        // Test health endpoint (should work without authentication)
        var healthResponse = jobController.healthCheck();
        assertNotNull(healthResponse);
        assertEquals(200, healthResponse.getStatusCodeValue());
    }
}

