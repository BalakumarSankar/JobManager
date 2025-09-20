package com.example.jobdispatcher.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Configuration class for setting up thread pools and scheduling with separate queues.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class ExecutorConfig implements AsyncConfigurer {
    
    @Autowired
    private ThreadPoolConfig threadPoolConfig;
    
    /**
     * Thread pool executor for one-time jobs with dedicated queue.
     */
    @Bean(name = "oneTimeJobExecutor")
    public Executor oneTimeJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        ThreadPoolConfig.ThreadPoolSettings settings = threadPoolConfig.getOneTimeJob();
        
        executor.setCorePoolSize(settings.getCorePoolSize());
        executor.setMaxPoolSize(settings.getMaxPoolSize());
        executor.setQueueCapacity(settings.getQueueCapacity());
        executor.setKeepAliveSeconds((int) settings.getKeepAliveTime());
        executor.setThreadNamePrefix(settings.getThreadNamePrefix() + "onetime-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        // Set custom queue based on configuration
        executor.setQueueCapacity(settings.getQueueCapacity());
        
        executor.initialize();
        return executor;
    }
    
    /**
     * Thread pool scheduler for repetitive jobs with dedicated queue.
     */
    @Bean(name = "repetitiveJobScheduler")
    public ThreadPoolTaskScheduler repetitiveJobScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        ThreadPoolConfig.ThreadPoolSettings settings = threadPoolConfig.getRepetitiveJob();
        
        scheduler.setPoolSize(settings.getCorePoolSize());
        scheduler.setThreadNamePrefix(settings.getThreadNamePrefix() + "repetitive-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        
        scheduler.initialize();
        return scheduler;
    }
    
    /**
     * Default async executor for @Async methods with dedicated queue.
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        ThreadPoolConfig.ThreadPoolSettings settings = threadPoolConfig.getAsyncJob();
        
        executor.setCorePoolSize(settings.getCorePoolSize());
        executor.setMaxPoolSize(settings.getMaxPoolSize());
        executor.setQueueCapacity(settings.getQueueCapacity());
        executor.setKeepAliveSeconds((int) settings.getKeepAliveTime());
        executor.setThreadNamePrefix(settings.getThreadNamePrefix() + "async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * Create a blocking queue based on the specified type.
     */
    private BlockingQueue<Runnable> createQueue(String queueType, int capacity) {
        switch (queueType.toUpperCase()) {
            case "ARRAY_BLOCKING_QUEUE":
                return new ArrayBlockingQueue<>(capacity);
            case "SYNCHRONOUS_QUEUE":
                return new SynchronousQueue<>();
            case "LINKED_BLOCKING_QUEUE":
            default:
                return new LinkedBlockingQueue<>(capacity);
        }
    }
}
