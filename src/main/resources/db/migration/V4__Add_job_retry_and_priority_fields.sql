-- V4__Add_job_retry_and_priority_fields.sql
ALTER TABLE scheduled_jobs 
ADD COLUMN job_priority VARCHAR(20) DEFAULT 'NORMAL',
ADD COLUMN timeout_millis BIGINT,
ADD COLUMN retry_enabled BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN max_retry_attempts INT NOT NULL DEFAULT 3,
ADD COLUMN retry_delay_millis BIGINT,
ADD COLUMN retry_multiplier DOUBLE DEFAULT 2.0,
ADD COLUMN retry_max_delay_millis BIGINT DEFAULT 30000,
ADD COLUMN last_retry_at TIMESTAMP NULL,
ADD COLUMN next_retry_at TIMESTAMP NULL,
ADD COLUMN retry_reason TEXT;

-- Add indexes for new fields
CREATE INDEX idx_scheduled_jobs_job_priority ON scheduled_jobs(job_priority);
CREATE INDEX idx_scheduled_jobs_retry_enabled ON scheduled_jobs(retry_enabled);
CREATE INDEX idx_scheduled_jobs_next_retry_at ON scheduled_jobs(next_retry_at);
CREATE INDEX idx_scheduled_jobs_retry_due ON scheduled_jobs(next_retry_at) WHERE next_retry_at IS NOT NULL AND status = 'FAILED';

-- Update existing jobs to have default retry settings
UPDATE scheduled_jobs 
SET retry_enabled = TRUE,
    max_retry_attempts = 3,
    retry_multiplier = 2.0,
    retry_max_delay_millis = 30000
WHERE retry_enabled IS NULL;

