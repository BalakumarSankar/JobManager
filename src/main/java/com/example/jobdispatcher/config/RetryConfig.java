package com.example.jobdispatcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for job retry settings with exponential backoff.
 */
@Configuration
@ConfigurationProperties(prefix = "job-retry")
public class RetryConfig {
    
    private boolean enabled = true;
    private int defaultMaxAttempts = 3;
    private long defaultInitialDelay = 1000; // 1 second
    private double defaultMultiplier = 2.0;
    private long defaultMaxDelay = 30000; // 30 seconds
    private double defaultJitter = 0.1; // 10% jitter
    private List<String> retryableExceptions = Arrays.asList(
        "java.lang.RuntimeException",
        "java.lang.Exception"
    );
    private List<String> nonRetryableExceptions = Arrays.asList(
        "java.lang.IllegalArgumentException",
        "java.lang.SecurityException"
    );
    
    // Per-job-type retry settings
    private JobTypeRetrySettings oneTimeJob = new JobTypeRetrySettings();
    private JobTypeRetrySettings repetitiveJob = new JobTypeRetrySettings();
    
    public static class JobTypeRetrySettings {
        private int maxAttempts = 3;
        private long initialDelay = 1000;
        private double multiplier = 2.0;
        private long maxDelay = 30000;
        private double jitter = 0.1;
        
        // Getters and Setters
        public int getMaxAttempts() {
            return maxAttempts;
        }
        
        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
        
        public long getInitialDelay() {
            return initialDelay;
        }
        
        public void setInitialDelay(long initialDelay) {
            this.initialDelay = initialDelay;
        }
        
        public double getMultiplier() {
            return multiplier;
        }
        
        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
        
        public long getMaxDelay() {
            return maxDelay;
        }
        
        public void setMaxDelay(long maxDelay) {
            this.maxDelay = maxDelay;
        }
        
        public double getJitter() {
            return jitter;
        }
        
        public void setJitter(double jitter) {
            this.jitter = jitter;
        }
    }
    
    /**
     * Calculate the delay for a given attempt with exponential backoff and jitter.
     */
    public long calculateDelay(int attempt, String jobType) {
        JobTypeRetrySettings settings = getSettingsForJobType(jobType);
        
        // Calculate exponential backoff: initialDelay * (multiplier ^ (attempt - 1))
        long delay = (long) (settings.getInitialDelay() * Math.pow(settings.getMultiplier(), attempt - 1));
        
        // Apply maximum delay limit
        delay = Math.min(delay, settings.getMaxDelay());
        
        // Apply jitter to prevent thundering herd
        if (settings.getJitter() > 0) {
            double jitterFactor = 1.0 + (Math.random() - 0.5) * 2 * settings.getJitter();
            delay = (long) (delay * jitterFactor);
        }
        
        return Math.max(delay, 0);
    }
    
    /**
     * Get retry settings for a specific job type.
     */
    public JobTypeRetrySettings getSettingsForJobType(String jobType) {
        if ("ONE_TIME".equalsIgnoreCase(jobType)) {
            return oneTimeJob;
        } else if ("REPETITIVE".equalsIgnoreCase(jobType)) {
            return repetitiveJob;
        } else {
            // Return default settings
            JobTypeRetrySettings defaultSettings = new JobTypeRetrySettings();
            defaultSettings.setMaxAttempts(defaultMaxAttempts);
            defaultSettings.setInitialDelay(defaultInitialDelay);
            defaultSettings.setMultiplier(defaultMultiplier);
            defaultSettings.setMaxDelay(defaultMaxDelay);
            defaultSettings.setJitter(defaultJitter);
            return defaultSettings;
        }
    }
    
    /**
     * Check if an exception is retryable.
     */
    public boolean isRetryableException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String exceptionName = throwable.getClass().getName();
        
        // Check if it's explicitly non-retryable
        for (String nonRetryable : nonRetryableExceptions) {
            if (exceptionName.equals(nonRetryable) || exceptionName.startsWith(nonRetryable + ".")) {
                return false;
            }
        }
        
        // Check if it's explicitly retryable
        for (String retryable : retryableExceptions) {
            if (exceptionName.equals(retryable) || exceptionName.startsWith(retryable + ".")) {
                return true;
            }
        }
        
        // Default to retryable for unknown exceptions
        return true;
    }
    
    /**
     * Check if a job should be retried based on attempt count.
     */
    public boolean shouldRetry(int currentAttempt, int maxAttempts) {
        return currentAttempt < maxAttempts;
    }
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getDefaultMaxAttempts() {
        return defaultMaxAttempts;
    }
    
    public void setDefaultMaxAttempts(int defaultMaxAttempts) {
        this.defaultMaxAttempts = defaultMaxAttempts;
    }
    
    public long getDefaultInitialDelay() {
        return defaultInitialDelay;
    }
    
    public void setDefaultInitialDelay(long defaultInitialDelay) {
        this.defaultInitialDelay = defaultInitialDelay;
    }
    
    public double getDefaultMultiplier() {
        return defaultMultiplier;
    }
    
    public void setDefaultMultiplier(double defaultMultiplier) {
        this.defaultMultiplier = defaultMultiplier;
    }
    
    public long getDefaultMaxDelay() {
        return defaultMaxDelay;
    }
    
    public void setDefaultMaxDelay(long defaultMaxDelay) {
        this.defaultMaxDelay = defaultMaxDelay;
    }
    
    public double getDefaultJitter() {
        return defaultJitter;
    }
    
    public void setDefaultJitter(double defaultJitter) {
        this.defaultJitter = defaultJitter;
    }
    
    public List<String> getRetryableExceptions() {
        return retryableExceptions;
    }
    
    public void setRetryableExceptions(List<String> retryableExceptions) {
        this.retryableExceptions = retryableExceptions;
    }
    
    public List<String> getNonRetryableExceptions() {
        return nonRetryableExceptions;
    }
    
    public void setNonRetryableExceptions(List<String> nonRetryableExceptions) {
        this.nonRetryableExceptions = nonRetryableExceptions;
    }
    
    public JobTypeRetrySettings getOneTimeJob() {
        return oneTimeJob;
    }
    
    public void setOneTimeJob(JobTypeRetrySettings oneTimeJob) {
        this.oneTimeJob = oneTimeJob;
    }
    
    public JobTypeRetrySettings getRepetitiveJob() {
        return repetitiveJob;
    }
    
    public void setRepetitiveJob(JobTypeRetrySettings repetitiveJob) {
        this.repetitiveJob = repetitiveJob;
    }
}

