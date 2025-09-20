package com.example.jobdispatcher.filter;

import com.example.jobdispatcher.entity.ApiKey;
import com.example.jobdispatcher.service.ApiKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Filter for API key authentication.
 * Validates API keys for job submission endpoints.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    
    @Autowired
    private ApiKeyService apiKeyService;
    
    // Endpoints that require API key authentication
    private static final String[] PROTECTED_ENDPOINTS = {
        "/api/jobs/onetime",
        "/api/jobs/repetitive"
    };
    
    // Endpoints that are exempt from API key authentication
    private static final String[] EXEMPT_ENDPOINTS = {
        "/api/jobs/health",
        "/api/jobs/stats",
        "/api/jobs/thread-pool-stats",
        "/api/jobs/grouping-stats",
        "/api/jobs/database-stats",
        "/api/jobs/rate-limiting-stats",
        "/api/jobs/retry-stats"
    };
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip authentication for exempt endpoints
        if (isExemptEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Only protect POST endpoints for job submission
        if (!"POST".equals(method) || !isProtectedEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract API key from request
        String apiKey = extractApiKey(request);
        
        if (apiKey == null) {
            logger.warn("Missing API key for request: {} {}", method, requestPath);
            sendUnauthorizedResponse(response, "API key is required");
            return;
        }
        
        // Validate API key
        Optional<ApiKey> apiKeyEntity = apiKeyService.validateApiKey(apiKey);
        
        if (!apiKeyEntity.isPresent()) {
            logger.warn("Invalid API key for request: {} {}", method, requestPath);
            sendUnauthorizedResponse(response, "Invalid API key");
            return;
        }
        
        ApiKey validApiKey = apiKeyEntity.get();
        
        // Check job type authorization
        String jobType = extractJobTypeFromPath(requestPath);
        if (jobType != null && !apiKeyService.isAuthorizedForJobType(apiKey, jobType)) {
            logger.warn("API key {} not authorized for job type: {}", validApiKey.getKeyName(), jobType);
            sendForbiddenResponse(response, "API key not authorized for this job type");
            return;
        }
        
        // Add API key information to request attributes for use in controllers
        request.setAttribute("apiKey", validApiKey);
        request.setAttribute("clientId", validApiKey.getClientId());
        
        logger.debug("API key authentication successful for client: {}", validApiKey.getClientId());
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract API key from request headers.
     */
    private String extractApiKey(HttpServletRequest request) {
        // Check Authorization header (Bearer token)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Check X-API-Key header
        String apiKeyHeader = request.getHeader("X-API-Key");
        if (apiKeyHeader != null && !apiKeyHeader.trim().isEmpty()) {
            return apiKeyHeader.trim();
        }
        
        // Check query parameter (less secure, but sometimes needed)
        String apiKeyParam = request.getParameter("api_key");
        if (apiKeyParam != null && !apiKeyParam.trim().isEmpty()) {
            return apiKeyParam.trim();
        }
        
        return null;
    }
    
    /**
     * Extract job type from request path.
     */
    private String extractJobTypeFromPath(String path) {
        if (path.contains("/onetime")) {
            return "ONE_TIME";
        } else if (path.contains("/repetitive")) {
            return "REPETITIVE";
        }
        return null;
    }
    
    /**
     * Check if endpoint is protected (requires API key).
     */
    private boolean isProtectedEndpoint(String path) {
        for (String endpoint : PROTECTED_ENDPOINTS) {
            if (path.startsWith(endpoint)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if endpoint is exempt from API key authentication.
     */
    private boolean isExemptEndpoint(String path) {
        for (String endpoint : EXEMPT_ENDPOINTS) {
            if (path.startsWith(endpoint)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Send unauthorized response.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":%d}",
            message, System.currentTimeMillis()
        );
        
        response.getWriter().write(jsonResponse);
    }
    
    /**
     * Send forbidden response.
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\":\"Forbidden\",\"message\":\"%s\",\"timestamp\":%d}",
            message, System.currentTimeMillis()
        );
        
        response.getWriter().write(jsonResponse);
    }
}

