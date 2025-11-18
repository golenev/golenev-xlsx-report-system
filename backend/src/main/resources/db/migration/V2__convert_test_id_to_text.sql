ALTER TABLE test_report
    ALTER COLUMN test_id TYPE TEXT
    USING test_id::TEXT;
