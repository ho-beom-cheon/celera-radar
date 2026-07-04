ALTER TABLE wholesale_uploads
    ADD COLUMN IF NOT EXISTS mapping_image_url VARCHAR(255);
