-- V2__Create_app_servers_table.sql
CREATE TABLE app_servers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    context_path VARCHAR(255) DEFAULT '',
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    health_check_url VARCHAR(500),
    last_health_check TIMESTAMP NULL,
    health_status VARCHAR(20) DEFAULT 'UNKNOWN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_app_servers_name (name),
    INDEX idx_app_servers_host_port (host, port),
    INDEX idx_app_servers_active (is_active),
    INDEX idx_app_servers_health_status (health_status),
    INDEX idx_app_servers_active_health (is_active, health_status),
    INDEX idx_app_servers_last_health_check (last_health_check)
);

-- Insert default app server configurations
INSERT INTO app_servers (name, host, port, context_path, description, health_check_url) VALUES
('local-server', 'localhost', 8080, '', 'Local development server', '/api/jobs/health'),
('production-server-1', 'prod-server-1.example.com', 8080, '/api', 'Production server 1', '/health'),
('production-server-2', 'prod-server-2.example.com', 8080, '/api', 'Production server 2', '/health'),
('staging-server', 'staging.example.com', 8080, '/api', 'Staging server', '/health');

