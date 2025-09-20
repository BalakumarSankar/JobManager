package com.example.jobdispatcher.service;

import io.github.bucket4j.Bucket;
import com.example.jobdispatcher.config.RateLimitingConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing rate limiting functionality using Bucket4j.
 */
@Service
public class RateLimitingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    
    @Autowired
    private RateLimitingConfig rateLimitingConfig;
    
    @Autowired
    @Qualifier("defaultBucket")
    private Bucket defaultBucket;
    
    @Autowired
    @Qualifier("oneTimeJobBucket")
    private Bucket oneTimeJobBucket;
    
    @Autowired
    @Qualifier("repetitiveJobBucket")
    private Bucket repetitiveJobBucket;
    
    @Autowired
    @Qualifier("freeTierBucket")
    private Bucket freeTierBucket;
    
    @Autowired
    @Qualifier("premiumTierBucket")
    private Bucket premiumTierBucket;
    
    @Autowired
    @Qualifier("enterpriseTierBucket")
    private Bucket enterpriseTierBucket;
    
    // Cache for per-IP rate limiting
    private final Cache<String, Bucket> ipBuckets = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();
    
    // Cache for per-app-server rate limiting
    private final Cache<String, Bucket> appServerBuckets = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();
    
    // Cache for per-API-key rate limiting
    private final Cache<String, Bucket> apiKeyBuckets = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();
    
    // Cache for per-user rate limiting
    private final Cache<String, Bucket> userBuckets = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();
    
    /**
     * Check if request is allowed based on IP address.
     */
    public boolean isAllowedByIp(String clientIp) {
        if (!rateLimitingConfig.isEnabled()) {
            return true;
        }
        
        Bucket bucket = ipBuckets.get(clientIp, k -> rateLimitingConfig.getBucket(k));
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
        }
        
        return allowed;
    }
    
    /**
     * Check if request is allowed based on app server ID.
     */
    public boolean isAllowedByAppServer(String appServerId) {
        if (!rateLimitingConfig.isEnabled() || appServerId == null) {
            return true;
        }
        
        Bucket bucket = appServerBuckets.get(appServerId, k -> rateLimitingConfig.getBucket(k));
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for app server: {}", appServerId);
        }
        
        return allowed;
    }
    
    /**
     * Check if request is allowed based on API key ID.
     */
    public boolean isAllowedByApiKey(String apiKeyId) {
        if (!rateLimitingConfig.isEnabled() || apiKeyId == null) {
            return true;
        }
        
        Bucket bucket = apiKeyBuckets.get(apiKeyId, k -> rateLimitingConfig.getBucket(k));
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for API key: {}", apiKeyId);
        }
        
        return allowed;
    }
    
    /**
     * Check if request is allowed based on user ID.
     */
    public boolean isAllowedByUser(String userId, String userTier) {
        if (!rateLimitingConfig.isEnabled()) {
            return true;
        }
        
        Bucket bucket = getUserBucket(userId, userTier);
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for user: {} (tier: {})", userId, userTier);
        }
        
        return allowed;
    }
    
    /**
     * Check if request is allowed based on job type.
     */
    public boolean isAllowedByJobType(String jobType) {
        if (!rateLimitingConfig.isEnabled()) {
            return true;
        }
        
        Bucket bucket = getJobTypeBucket(jobType);
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            logger.warn("Rate limit exceeded for job type: {}", jobType);
        }
        
        return allowed;
    }
    
    /**
     * Check if request is allowed based on JWT token information.
     */
    public boolean isAllowedByJwt(String appServerId, String apiKeyId, String jobType) {
        if (!rateLimitingConfig.isEnabled()) {
            return true;
        }
        
        // Check app server rate limit
        if (appServerId != null && !isAllowedByAppServer(appServerId)) {
            return false;
        }
        
        // Check API key rate limit
        if (apiKeyId != null && !isAllowedByApiKey(apiKeyId)) {
            return false;
        }
        
        // Check job type rate limit
        if (jobType != null && !isAllowedByJobType(jobType)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if request is allowed based on multiple criteria.
     */
    public boolean isAllowed(String clientIp, String userId, String userTier, String jobType) {
        if (!rateLimitingConfig.isEnabled()) {
            return true;
        }
        
        // Check IP rate limit
        if (!isAllowedByIp(clientIp)) {
            return false;
        }
        
        // Check user rate limit (if userId provided)
        if (userId != null && !userId.trim().isEmpty()) {
            if (!isAllowedByUser(userId, userTier)) {
                return false;
            }
        }
        
        // Check job type rate limit
        if (jobType != null && !jobType.trim().isEmpty()) {
            if (!isAllowedByJobType(jobType)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get remaining tokens for IP.
     */
    public long getRemainingTokensForIp(String clientIp) {
        Bucket bucket = ipBuckets.getIfPresent(clientIp);
        return bucket != null ? bucket.getAvailableTokens() : rateLimitingConfig.getDefaultRequestsPerMinute();
    }
    
    /**
     * Get remaining tokens for user.
     */
    public long getRemainingTokensForUser(String userId, String userTier) {
        Bucket bucket = getUserBucket(userId, userTier);
        return bucket.getAvailableTokens();
    }
    
    /**
     * Get remaining tokens for job type.
     */
    public long getRemainingTokensForJobType(String jobType) {
        Bucket bucket = getJobTypeBucket(jobType);
        return bucket.getAvailableTokens();
    }
    
    /**
     * Get user-specific bucket based on tier.
     */
    private Bucket getUserBucket(String userId, String userTier) {
        String key = userId + ":" + userTier;
        return userBuckets.get(key, k -> rateLimitingConfig.getUserTierBucket(userTier));
    }
    
    /**
     * Get job type-specific bucket.
     */
    private Bucket getJobTypeBucket(String jobType) {
        return rateLimitingConfig.getJobTypeBucket(jobType);
    }
    
    /**
     * Get client IP address from request.
     */
    public String getClientIp(HttpServletRequest request) {
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
    
    /**
     * Extract user ID from request headers.
     */
    public String getUserId(HttpServletRequest request) {
        return request.getHeader("X-User-ID");
    }
    
    /**
     * Extract user tier from request headers.
     */
    public String getUserTier(HttpServletRequest request) {
        String tier = request.getHeader("X-User-Tier");
        return tier != null ? tier : "free";
    }
    
    /**
     * Get rate limiting statistics.
     */
    public Map<String, Object> getRateLimitingStats() {
        return Map.of(
            "enabled", rateLimitingConfig.isEnabled(),
            "configuration", rateLimitingConfig.getConfiguration(),
            "ipCacheSize", ipBuckets.estimatedSize(),
            "appServerCacheSize", appServerBuckets.estimatedSize(),
            "apiKeyCacheSize", apiKeyBuckets.estimatedSize(),
            "userCacheSize", userBuckets.estimatedSize(),
            "timestamp", System.currentTimeMillis()
        );
    }
}