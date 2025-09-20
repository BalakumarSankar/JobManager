package com.example.jobdispatcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for thread pool settings with separate queues.
 */
@Configuration
@ConfigurationProperties(prefix = "job-dispatcher")
public class ThreadPoolConfig {
    
    private ThreadPoolSettings oneTimeJob = new ThreadPoolSettings();
    private ThreadPoolSettings repetitiveJob = new ThreadPoolSettings();
    private ThreadPoolSettings asyncJob = new ThreadPoolSettings();
    
    public ThreadPoolConfig() {}
    
    public ThreadPoolSettings getOneTimeJob() {
        return oneTimeJob;
    }
    
    public void setOneTimeJob(ThreadPoolSettings oneTimeJob) {
        this.oneTimeJob = oneTimeJob;
    }
    
    public ThreadPoolSettings getRepetitiveJob() {
        return repetitiveJob;
    }
    
    public void setRepetitiveJob(ThreadPoolSettings repetitiveJob) {
        this.repetitiveJob = repetitiveJob;
    }
    
    public ThreadPoolSettings getAsyncJob() {
        return asyncJob;
    }
    
    public void setAsyncJob(ThreadPoolSettings asyncJob) {
        this.asyncJob = asyncJob;
    }
    
    /**
     * Thread pool settings for individual pools.
     */
    public static class ThreadPoolSettings {
        private int corePoolSize = 5;
        private int maxPoolSize = 20;
        private long keepAliveTime = 60L;
        private int queueCapacity = 100;
        private String threadNamePrefix = "job-dispatcher-";
        private String queueType = "LINKED_BLOCKING_QUEUE"; // LINKED_BLOCKING_QUEUE, ARRAY_BLOCKING_QUEUE, SYNCHRONOUS_QUEUE
        
        public ThreadPoolSettings() {}
        
        public int getCorePoolSize() {
            return corePoolSize;
        }
        
        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }
        
        public int getMaxPoolSize() {
            return maxPoolSize;
        }
        
        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }
        
        public long getKeepAliveTime() {
            return keepAliveTime;
        }
        
        public void setKeepAliveTime(long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
        }
        
        public int getQueueCapacity() {
            return queueCapacity;
        }
        
        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
        
        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }
        
        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
        
        public String getQueueType() {
            return queueType;
        }
        
        public void setQueueType(String queueType) {
            this.queueType = queueType;
        }
    }
}
