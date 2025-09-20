package com.example.jobdispatcher.job;

/**
 * Interface for one-time jobs that are executed only once.
 * These jobs are dispatched immediately and not repeated.
 */
public interface OneTimeJob extends Job {
    
    /**
     * Indicates that this is a one-time job.
     * @return always true for one-time jobs
     */
    default boolean isOneTime() {
        return true;
    }
}
