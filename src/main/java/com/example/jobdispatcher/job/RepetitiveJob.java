package com.example.jobdispatcher.job;

/**
 * Interface for repetitive jobs that can be executed multiple times with different intervals.
 * These jobs support various repetition patterns like fixed delay, fixed rate, or cron expressions.
 */
public interface RepetitiveJob extends Job {
    
    /**
     * Get the repetition interval in milliseconds.
     * @return the interval between executions
     */
    long getIntervalMillis();
    
    /**
     * Get the initial delay before the first execution in milliseconds.
     * @return the initial delay
     */
    default long getInitialDelayMillis() {
        return 0;
    }
    
    /**
     * Indicates that this is a repetitive job.
     * @return always false for repetitive jobs
     */
    default boolean isOneTime() {
        return false;
    }
    
    /**
     * Get the repetition type (FIXED_DELAY, FIXED_RATE, CRON).
     * @return the repetition type
     */
    RepetitionType getRepetitionType();
    
    /**
     * Enum for different repetition types.
     */
    enum RepetitionType {
        FIXED_DELAY,    // Fixed delay between the end of one execution and the start of the next
        FIXED_RATE,     // Fixed rate regardless of execution time
        CRON           // Cron expression based scheduling
    }
}
