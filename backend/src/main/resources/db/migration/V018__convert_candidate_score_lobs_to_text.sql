DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'candidate_score'
          AND column_name = 'reasons'
          AND data_type = 'oid'
    ) THEN
        ALTER TABLE candidate_score ADD COLUMN reasons_text TEXT;
        UPDATE candidate_score
        SET reasons_text = convert_from(lo_get(reasons), 'UTF8');
        ALTER TABLE candidate_score DROP COLUMN reasons;
        ALTER TABLE candidate_score RENAME COLUMN reasons_text TO reasons;
        ALTER TABLE candidate_score ALTER COLUMN reasons SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'candidate_score'
          AND column_name = 'warnings'
          AND data_type = 'oid'
    ) THEN
        ALTER TABLE candidate_score ADD COLUMN warnings_text TEXT;
        UPDATE candidate_score
        SET warnings_text = convert_from(lo_get(warnings), 'UTF8');
        ALTER TABLE candidate_score DROP COLUMN warnings;
        ALTER TABLE candidate_score RENAME COLUMN warnings_text TO warnings;
        ALTER TABLE candidate_score ALTER COLUMN warnings SET NOT NULL;
    END IF;
END $$;
