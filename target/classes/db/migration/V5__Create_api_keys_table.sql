-- V5__Create_api_keys_table.sql
CREATE TABLE api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_name VARCHAR(100) NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    usage_count BIGINT NOT NULL DEFAULT 0,
    rate_limit_per_minute INT NOT NULL DEFAULT 60,
    rate_limit_per_hour INT NOT NULL DEFAULT 1000,
    allowed_job_types VARCHAR(500),
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_api_keys_api_key (api_key),
    INDEX idx_api_keys_client_id (client_id),
    INDEX idx_api_keys_key_name (key_name),
    INDEX idx_api_keys_active (is_active),
    INDEX idx_api_keys_expires_at (expires_at),
    INDEX idx_api_keys_last_used_at (last_used_at),
    INDEX idx_api_keys_created_by (created_by),
    INDEX idx_api_keys_usage_count (usage_count)
);

-- Insert default API key for testing
INSERT INTO api_keys (key_name, api_key, client_id, description, created_by) VALUES
('default-test-key', 'jd_test_default_key_123456789', 'test-client-001', 'Default API key for testing', 'system'),
('admin-key', 'jd_admin_key_987654321', 'admin-client-001', 'Admin API key with full access', 'admin');

