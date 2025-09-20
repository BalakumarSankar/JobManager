package com.example.jobdispatcher.aspect;

import com.example.jobdispatcher.annotation.RateLimited;
import com.example.jobdispatcher.service.RateLimitingService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Aspect for handling rate limiting annotations.
 */
@Aspect
@Component
public class RateLimitingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingAspect.class);
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
    @Around("@annotation(rateLimited)")
    public Object handleRateLimiting(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            logger.warn("No HTTP request found in context, skipping rate limiting");
            return joinPoint.proceed();
        }
        
        String clientIp = rateLimitingService.getClientIp(request);
        String userId = rateLimitingService.getUserId(request);
        String userTier = rateLimitingService.getUserTier(request);
        String jobType = rateLimited.jobType();
        
        // Extract JWT information from request attributes (set by JwtAuthenticationFilter)
        String appServerId = (String) request.getAttribute("appServerId");
        String apiKeyId = (String) request.getAttribute("apiKeyId");
        
        boolean allowed = false;
        
        switch (rateLimited.value()) {
            case IP:
                allowed = rateLimitingService.isAllowedByIp(clientIp);
                break;
            case USER:
                if (userId != null && !userId.trim().isEmpty()) {
                    allowed = rateLimitingService.isAllowedByUser(userId, userTier);
                } else {
                    logger.warn("User ID not provided for user-based rate limiting");
                    allowed = true; // Allow if no user ID provided
                }
                break;
            case JOB_TYPE:
                allowed = rateLimitingService.isAllowedByJobType(jobType);
                break;
            case CUSTOM:
                if (!rateLimited.key().isEmpty()) {
                    // For custom key, we could implement additional logic
                    allowed = rateLimitingService.isAllowedByIp(clientIp);
                } else {
                    allowed = true;
                }
                break;
            case JWT:
                allowed = rateLimitingService.isAllowedByJwt(appServerId, apiKeyId, jobType);
                break;
            case COMBINED:
                // Use JWT-based rate limiting if available, otherwise fall back to traditional method
                if (appServerId != null && apiKeyId != null) {
                    allowed = rateLimitingService.isAllowedByJwt(appServerId, apiKeyId, jobType);
                } else {
                    allowed = rateLimitingService.isAllowed(clientIp, userId, userTier, jobType);
                }
                break;
            default:
                allowed = true;
        }
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for IP: {}, User: {}, JobType: {}", clientIp, userId, jobType);
            
            // Return rate limit exceeded response
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new RateLimitExceededResponse(
                            rateLimited.message(),
                            getRemainingTokens(clientIp, userId, userTier, jobType, rateLimited.value())
                    ));
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * Get current HTTP request from context.
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    /**
     * Get remaining tokens based on rate limit type.
     */
    private long getRemainingTokens(String clientIp, String userId, String userTier, String jobType, RateLimited.RateLimitType rateLimitType) {
        switch (rateLimitType) {
            case IP:
                return rateLimitingService.getRemainingTokensForIp(clientIp);
            case USER:
                return rateLimitingService.getRemainingTokensForUser(userId, userTier);
            case JOB_TYPE:
                return rateLimitingService.getRemainingTokensForJobType(jobType);
            case COMBINED:
                return Math.min(
                        Math.min(
                                rateLimitingService.getRemainingTokensForIp(clientIp),
                                rateLimitingService.getRemainingTokensForJobType(jobType)
                        ),
                        userId != null ? rateLimitingService.getRemainingTokensForUser(userId, userTier) : Long.MAX_VALUE
                );
            default:
                return 0;
        }
    }
    
    /**
     * Response class for rate limit exceeded.
     */
    public static class RateLimitExceededResponse {
        private String message;
        private long remainingTokens;
        private long timestamp;
        
        public RateLimitExceededResponse(String message, long remainingTokens) {
            this.message = message;
            this.remainingTokens = remainingTokens;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public String getMessage() {
            return message;
        }
        
        public long getRemainingTokens() {
            return remainingTokens;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
