package com.example.jobdispatcher.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for rate limiting using Bucket4j and Caffeine cache.
 */
@Configuration
public class RateLimitingConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingConfig.class);
    
    @Value("${rate-limiting.enabled:true}")
    private boolean enabled;
    
    @Value("${rate-limiting.default-requests-per-minute:60}")
    private int defaultRequestsPerMinute;
    
    @Value("${rate-limiting.default-requests-per-hour:1000}")
    private int defaultRequestsPerHour;
    
    @Value("${rate-limiting.default-requests-per-day:10000}")
    private int defaultRequestsPerDay;
    
    @Value("${rate-limiting.burst-capacity:10}")
    private int burstCapacity;
    
    @Value("${rate-limiting.window-size:PT1M}")
    private String windowSize;
    
    // Rate limits for job types
    @Value("${rate-limiting.one-time-job.requests-per-minute:30}")
    private int oneTimeJobRequestsPerMinute;
    
    @Value("${rate-limiting.one-time-job.requests-per-hour:500}")
    private int oneTimeJobRequestsPerHour;
    
    @Value("${rate-limiting.one-time-job.burst-capacity:5}")
    private int oneTimeJobBurstCapacity;
    
    @Value("${rate-limiting.repetitive-job.requests-per-minute:20}")
    private int repetitiveJobRequestsPerMinute;
    
    @Value("${rate-limiting.repetitive-job.requests-per-hour:300}")
    private int repetitiveJobRequestsPerHour;
    
    @Value("${rate-limiting.repetitive-job.burst-capacity:3}")
    private int repetitiveJobBurstCapacity;
    
    // Rate limits for user tiers
    @Value("${rate-limiting.free-tier.requests-per-minute:10}")
    private int freeTierRequestsPerMinute;
    
    @Value("${rate-limiting.free-tier.requests-per-hour:100}")
    private int freeTierRequestsPerHour;
    
    @Value("${rate-limiting.free-tier.requests-per-day:1000}")
    private int freeTierRequestsPerDay;
    
    @Value("${rate-limiting.free-tier.burst-capacity:2}")
    private int freeTierBurstCapacity;
    
    @Value("${rate-limiting.premium-tier.requests-per-minute:50}")
    private int premiumTierRequestsPerMinute;
    
    @Value("${rate-limiting.premium-tier.requests-per-hour:1000}")
    private int premiumTierRequestsPerHour;
    
    @Value("${rate-limiting.premium-tier.requests-per-day:10000}")
    private int premiumTierRequestsPerDay;
    
    @Value("${rate-limiting.premium-tier.burst-capacity:8}")
    private int premiumTierBurstCapacity;
    
    @Value("${rate-limiting.enterprise-tier.requests-per-minute:200}")
    private int enterpriseTierRequestsPerMinute;
    
    @Value("${rate-limiting.enterprise-tier.requests-per-hour:5000}")
    private int enterpriseTierRequestsPerHour;
    
    @Value("${rate-limiting.enterprise-tier.requests-per-day:50000}")
    private int enterpriseTierRequestsPerDay;
    
    @Value("${rate-limiting.enterprise-tier.burst-capacity:20}")
    private int enterpriseTierBurstCapacity;
    
    // Cache for storing buckets
    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();
    
    /**
     * Default bucket for general rate limiting.
     */
    @Bean("defaultBucket")
    public Bucket defaultBucket() {
        return createBucket(defaultRequestsPerMinute, burstCapacity);
    }
    
    /**
     * Bucket for one-time job submissions.
     */
    @Bean("oneTimeJobBucket")
    public Bucket oneTimeJobBucket() {
        return createBucket(oneTimeJobRequestsPerMinute, oneTimeJobBurstCapacity);
    }
    
    /**
     * Bucket for repetitive job submissions.
     */
    @Bean("repetitiveJobBucket")
    public Bucket repetitiveJobBucket() {
        return createBucket(repetitiveJobRequestsPerMinute, repetitiveJobBurstCapacity);
    }
    
    /**
     * Bucket for free tier users.
     */
    @Bean("freeTierBucket")
    public Bucket freeTierBucket() {
        return createBucket(freeTierRequestsPerMinute, freeTierBurstCapacity);
    }
    
    /**
     * Bucket for premium tier users.
     */
    @Bean("premiumTierBucket")
    public Bucket premiumTierBucket() {
        return createBucket(premiumTierRequestsPerMinute, premiumTierBurstCapacity);
    }
    
    /**
     * Bucket for enterprise tier users.
     */
    @Bean("enterpriseTierBucket")
    public Bucket enterpriseTierBucket() {
        return createBucket(enterpriseTierRequestsPerMinute, enterpriseTierBurstCapacity);
    }
    
    /**
     * Create a bucket with specified capacity and burst.
     */
    private Bucket createBucket(int capacity, int burst) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofMinutes(1)));
        return new LocalBucketBuilder()
                .addLimit(limit)
                .build();
    }
    
    /**
     * Get or create a bucket for a specific key.
     */
    public Bucket getBucket(String key) {
        return bucketCache.get(key, k -> createBucket(defaultRequestsPerMinute, burstCapacity));
    }
    
    /**
     * Get bucket for specific job type.
     */
    public Bucket getJobTypeBucket(String jobType) {
        switch (jobType.toUpperCase()) {
            case "ONE_TIME":
                return oneTimeJobBucket();
            case "REPETITIVE":
                return repetitiveJobBucket();
            default:
                return defaultBucket();
        }
    }
    
    /**
     * Get bucket for specific user tier.
     */
    public Bucket getUserTierBucket(String userTier) {
        switch (userTier.toLowerCase()) {
            case "free":
                return freeTierBucket();
            case "premium":
                return premiumTierBucket();
            case "enterprise":
                return enterpriseTierBucket();
            default:
                return freeTierBucket();
        }
    }
    
    // Getters for configuration values
    public boolean isEnabled() { return enabled; }
    public int getDefaultRequestsPerMinute() { return defaultRequestsPerMinute; }
    public int getDefaultRequestsPerHour() { return defaultRequestsPerHour; }
    public int getDefaultRequestsPerDay() { return defaultRequestsPerDay; }
    public int getBurstCapacity() { return burstCapacity; }
    public String getWindowSize() { return windowSize; }
    
    public int getOneTimeJobRequestsPerMinute() { return oneTimeJobRequestsPerMinute; }
    public int getOneTimeJobRequestsPerHour() { return oneTimeJobRequestsPerHour; }
    public int getOneTimeJobBurstCapacity() { return oneTimeJobBurstCapacity; }
    
    public int getRepetitiveJobRequestsPerMinute() { return repetitiveJobRequestsPerMinute; }
    public int getRepetitiveJobRequestsPerHour() { return repetitiveJobRequestsPerHour; }
    public int getRepetitiveJobBurstCapacity() { return repetitiveJobBurstCapacity; }
    
    public int getFreeTierRequestsPerMinute() { return freeTierRequestsPerMinute; }
    public int getFreeTierRequestsPerHour() { return freeTierRequestsPerHour; }
    public int getFreeTierRequestsPerDay() { return freeTierRequestsPerDay; }
    public int getFreeTierBurstCapacity() { return freeTierBurstCapacity; }
    
    public int getPremiumTierRequestsPerMinute() { return premiumTierRequestsPerMinute; }
    public int getPremiumTierRequestsPerHour() { return premiumTierRequestsPerHour; }
    public int getPremiumTierRequestsPerDay() { return premiumTierRequestsPerDay; }
    public int getPremiumTierBurstCapacity() { return premiumTierBurstCapacity; }
    
    public int getEnterpriseTierRequestsPerMinute() { return enterpriseTierRequestsPerMinute; }
    public int getEnterpriseTierRequestsPerHour() { return enterpriseTierRequestsPerHour; }
    public int getEnterpriseTierRequestsPerDay() { return enterpriseTierRequestsPerDay; }
    public int getEnterpriseTierBurstCapacity() { return enterpriseTierBurstCapacity; }
    
    /**
     * Get rate limiting configuration as a map.
     */
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", enabled);
        config.put("defaultRequestsPerMinute", defaultRequestsPerMinute);
        config.put("defaultRequestsPerHour", defaultRequestsPerHour);
        config.put("defaultRequestsPerDay", defaultRequestsPerDay);
        config.put("burstCapacity", burstCapacity);
        config.put("windowSize", windowSize);
        config.put("oneTimeJobRequestsPerMinute", oneTimeJobRequestsPerMinute);
        config.put("repetitiveJobRequestsPerMinute", repetitiveJobRequestsPerMinute);
        config.put("freeTierRequestsPerMinute", freeTierRequestsPerMinute);
        config.put("premiumTierRequestsPerMinute", premiumTierRequestsPerMinute);
        config.put("enterpriseTierRequestsPerMinute", enterpriseTierRequestsPerMinute);
        return config;
    }
}