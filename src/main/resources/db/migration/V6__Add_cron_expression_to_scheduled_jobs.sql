-- Add cron_expression column to scheduled_jobs for CRON-based repetition
ALTER TABLE scheduled_jobs
ADD COLUMN IF NOT EXISTS cron_expression VARCHAR(255);



