package com.example.jobdispatcher.repository;

import com.example.jobdispatcher.entity.ScheduledJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ScheduledJob entity operations.
 */
@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {
    
    /**
     * Find scheduled job by job ID.
     */
    Optional<ScheduledJob> findByJobId(String jobId);
    
    /**
     * Find jobs by status.
     */
    List<ScheduledJob> findByStatus(String status);
    
    /**
     * Find jobs by job type.
     */
    List<ScheduledJob> findByJobType(String jobType);
    
    /**
     * Find jobs by status and job type.
     */
    List<ScheduledJob> findByStatusAndJobType(String status, String jobType);
    
    /**
     * Find jobs by group key.
     */
    List<ScheduledJob> findByGroupKey(String groupKey);
    
    /**
     * Find jobs by group key and status.
     */
    List<ScheduledJob> findByGroupKeyAndStatus(String groupKey, String status);
    
    /**
     * Find jobs by thread pool.
     */
    List<ScheduledJob> findByThreadPoolId(Long threadPoolId);
    
    /**
     * Find jobs by app server.
     */
    List<ScheduledJob> findByAppServerId(Long appServerId);
    
    /**
     * Find jobs submitted after specified time.
     */
    List<ScheduledJob> findBySubmittedAtAfter(LocalDateTime submittedAt);
    
    /**
     * Find jobs submitted between specified times.
     */
    List<ScheduledJob> findBySubmittedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find jobs by priority.
     */
    List<ScheduledJob> findByPriority(Integer priority);
    
    /**
     * Find jobs by priority range.
     */
    @Query("SELECT sj FROM ScheduledJob sj WHERE sj.priority BETWEEN :minPriority AND :maxPriority ORDER BY sj.priority ASC")
    List<ScheduledJob> findByPriorityRange(@Param("minPriority") Integer minPriority, @Param("maxPriority") Integer maxPriority);
    
    /**
     * Find jobs that can be retried.
     */
    @Query("SELECT sj FROM ScheduledJob sj WHERE sj.status = 'FAILED' AND sj.retryCount < sj.maxRetries")
    List<ScheduledJob> findJobsForRetry();
    
    /**
     * Find running jobs older than specified time.
     */
    @Query("SELECT sj FROM ScheduledJob sj WHERE sj.status = 'RUNNING' AND sj.startedAt < :cutoffTime")
    List<ScheduledJob> findLongRunningJobs(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Count jobs by status.
     */
    long countByStatus(String status);
    
    /**
     * Count jobs by job type.
     */
    long countByJobType(String jobType);
    
    /**
     * Count jobs by status and job type.
     */
    long countByStatusAndJobType(String status, String jobType);
    
    /**
     * Count jobs by group key.
     */
    long countByGroupKey(String groupKey);
    
    /**
     * Find jobs with pagination.
     */
    Page<ScheduledJob> findByStatus(String status, Pageable pageable);
    
    /**
     * Find jobs by status and job type with pagination.
     */
    Page<ScheduledJob> findByStatusAndJobType(String status, String jobType, Pageable pageable);
    
    /**
     * Find jobs by thread pool with pagination.
     */
    Page<ScheduledJob> findByThreadPoolId(Long threadPoolId, Pageable pageable);
    
    /**
     * Find jobs by app server with pagination.
     */
    Page<ScheduledJob> findByAppServerId(Long appServerId, Pageable pageable);
    
    /**
     * Find jobs submitted today.
     */
    @Query("SELECT sj FROM ScheduledJob sj WHERE DATE(sj.submittedAt) = CURRENT_DATE")
    List<ScheduledJob> findJobsSubmittedToday();
    
    /**
     * Find jobs completed today.
     */
    @Query("SELECT sj FROM ScheduledJob sj WHERE DATE(sj.completedAt) = CURRENT_DATE")
    List<ScheduledJob> findJobsCompletedToday();
    
    /**
     * Find average execution time by job type.
     */
    @Query("SELECT AVG(sj.executionTimeMs) FROM ScheduledJob sj WHERE sj.jobType = :jobType AND sj.status = 'COMPLETED' AND sj.executionTimeMs IS NOT NULL")
    Double findAverageExecutionTimeByJobType(@Param("jobType") String jobType);
    
    /**
     * Find jobs with execution time greater than specified value.
     */
    @Query("SELECT sj FROM ScheduledJob sj WHERE sj.executionTimeMs > :executionTime AND sj.status = 'COMPLETED'")
    List<ScheduledJob> findJobsWithLongExecutionTime(@Param("executionTime") Long executionTime);
    
    /**
     * Check if job exists by job ID.
     */
    boolean existsByJobId(String jobId);
    
    /**
     * Delete jobs by status.
     */
    void deleteByStatus(String status);
    
    /**
     * Delete jobs older than specified time.
     */
    void deleteBySubmittedAtBefore(LocalDateTime cutoffTime);
}

