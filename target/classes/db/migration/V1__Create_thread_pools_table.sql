-- V1__Create_thread_pools_table.sql
CREATE TABLE thread_pools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    core_pool_size INT NOT NULL,
    max_pool_size INT NOT NULL,
    keep_alive_time BIGINT NOT NULL,
    queue_capacity INT NOT NULL,
    thread_name_prefix VARCHAR(100) NOT NULL,
    queue_type VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_thread_pools_name (name),
    INDEX idx_thread_pools_type (type),
    INDEX idx_thread_pools_active (is_active),
    INDEX idx_thread_pools_type_active (type, is_active)
);

-- Insert default thread pool configurations
INSERT INTO thread_pools (name, type, core_pool_size, max_pool_size, keep_alive_time, queue_capacity, thread_name_prefix, queue_type) VALUES
('one-time-job-executor', 'ONE_TIME', 5, 20, 60, 100, 'job-dispatcher-onetime-', 'LINKED_BLOCKING_QUEUE'),
('repetitive-job-scheduler', 'REPETITIVE', 3, 10, 120, 50, 'job-dispatcher-repetitive-', 'LINKED_BLOCKING_QUEUE'),
('async-job-executor', 'ASYNC', 8, 25, 90, 200, 'job-dispatcher-async-', 'LINKED_BLOCKING_QUEUE');

