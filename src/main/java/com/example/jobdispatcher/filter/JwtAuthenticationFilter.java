package com.example.jobdispatcher.filter;

import com.example.jobdispatcher.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT Authentication Filter for securing job submission endpoints.
 * Validates JWT tokens on protected endpoints.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        // Validate token
        if (token == null || !authenticationService.validateToken(token)) {
            logger.warn("Unauthorized access attempt to: {} from IP: {}", requestPath, getClientIpAddress(request));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing JWT token\"}");
            return;
        }
        
        // Extract app server ID and add to request attributes
        String appServerId = authenticationService.getAppServerIdFromToken(token);
        String apiKeyId = authenticationService.getApiKeyIdFromToken(token);
        
        request.setAttribute("appServerId", appServerId);
        request.setAttribute("apiKeyId", apiKeyId);
        request.setAttribute("jwtToken", token);
        
        logger.debug("Authenticated request from app server: {} (API key: {}) to: {}", 
                   appServerId, apiKeyId, requestPath);
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if the endpoint is public (doesn't require authentication).
     */
    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.startsWith("/api/auth/") ||
               requestPath.startsWith("/api/jobs/health") ||
               requestPath.startsWith("/actuator/") ||
               requestPath.startsWith("/swagger-ui/") ||
               requestPath.startsWith("/v3/api-docs") ||
               requestPath.equals("/favicon.ico");
    }
    
    /**
     * Get client IP address from request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

