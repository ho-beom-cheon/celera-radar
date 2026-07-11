ALTER TABLE wholesale_uploads
    ADD COLUMN IF NOT EXISTS raw_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS raw_deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS raw_delete_failed_at TIMESTAMPTZ;

UPDATE wholesale_uploads
SET raw_expires_at = created_at + INTERVAL '7 days'
WHERE raw_expires_at IS NULL;

ALTER TABLE wholesale_uploads
    ALTER COLUMN raw_expires_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_wholesale_uploads_raw_expiry
    ON wholesale_uploads(raw_expires_at)
    WHERE raw_deleted_at IS NULL;
