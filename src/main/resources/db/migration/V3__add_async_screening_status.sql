ALTER TABLE applications
    ADD COLUMN screening_status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    ADD COLUMN screening_error TEXT,
    ADD COLUMN screening_requested_at TIMESTAMP,
    ADD COLUMN screening_completed_at TIMESTAMP;
