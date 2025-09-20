package com.example.jobdispatcher.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for rate limiting job submissions.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    
    /**
     * Rate limiting key type.
     */
    RateLimitType value() default RateLimitType.IP;
    
    /**
     * Custom rate limit key (optional).
     */
    String key() default "";
    
    /**
     * Job type for rate limiting.
     */
    String jobType() default "";
    
    /**
     * Custom error message when rate limit is exceeded.
     */
    String message() default "Rate limit exceeded. Please try again later.";
    
    enum RateLimitType {
        IP,           // Rate limit by IP address
        USER,         // Rate limit by user ID
        JOB_TYPE,     // Rate limit by job type
        JWT,          // Rate limit by JWT token (app server + API key)
        CUSTOM,       // Rate limit by custom key
        COMBINED      // Rate limit by multiple criteria
    }
}
