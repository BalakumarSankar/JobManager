package com.example.jobdispatcher.repository;

import com.example.jobdispatcher.entity.ThreadPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ThreadPool entity operations.
 */
@Repository
public interface ThreadPoolRepository extends JpaRepository<ThreadPool, Long> {
    
    /**
     * Find thread pool by name.
     */
    Optional<ThreadPool> findByName(String name);
    
    /**
     * Find all active thread pools.
     */
    List<ThreadPool> findByIsActiveTrue();
    
    /**
     * Find thread pools by type.
     */
    List<ThreadPool> findByType(String type);
    
    /**
     * Find active thread pools by type.
     */
    List<ThreadPool> findByTypeAndIsActiveTrue(String type);
    
    /**
     * Check if thread pool exists by name.
     */
    boolean existsByName(String name);
    
    /**
     * Find thread pools with queue capacity greater than specified value.
     */
    @Query("SELECT tp FROM ThreadPool tp WHERE tp.queueCapacity > :capacity AND tp.isActive = true")
    List<ThreadPool> findActiveThreadPoolsWithCapacityGreaterThan(@Param("capacity") Integer capacity);
    
    /**
     * Count active thread pools by type.
     */
    @Query("SELECT COUNT(tp) FROM ThreadPool tp WHERE tp.type = :type AND tp.isActive = true")
    long countActiveByType(@Param("type") String type);
}

