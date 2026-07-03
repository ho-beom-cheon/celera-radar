ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(30) NOT NULL DEFAULT 'USER';

UPDATE users
SET role = 'USER'
WHERE role IS NULL;

ALTER TABLE users ALTER COLUMN role SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_users_role'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT ck_users_role CHECK (role IN ('USER','ADMIN'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
