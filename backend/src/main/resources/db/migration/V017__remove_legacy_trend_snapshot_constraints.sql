DO $$
DECLARE
    legacy_constraint RECORD;
BEGIN
    FOR legacy_constraint IN
        SELECT constraint_record.conname
        FROM pg_constraint constraint_record
        JOIN pg_class source_table ON source_table.oid = constraint_record.conrelid
        JOIN pg_class target_table ON target_table.oid = constraint_record.confrelid
        WHERE source_table.relname = 'trend_snapshots'
          AND target_table.relname = 'keyword_master'
    LOOP
        EXECUTE format(
                'ALTER TABLE trend_snapshots DROP CONSTRAINT IF EXISTS %I',
                legacy_constraint.conname
        );
    END LOOP;
END $$;

ALTER TABLE trend_snapshots
    DROP CONSTRAINT IF EXISTS uk_trend_snapshot_keyword_period_time_unit;

ALTER TABLE trend_snapshots
    DROP COLUMN IF EXISTS period;
