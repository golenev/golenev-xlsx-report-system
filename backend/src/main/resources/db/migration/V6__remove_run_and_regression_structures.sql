ALTER TABLE test_report
    DROP COLUMN IF EXISTS run_1_status,
    DROP COLUMN IF EXISTS run_2_status,
    DROP COLUMN IF EXISTS run_3_status,
    DROP COLUMN IF EXISTS run_4_status,
    DROP COLUMN IF EXISTS run_5_status,
    DROP COLUMN IF EXISTS regression_status,
    DROP COLUMN IF EXISTS regression_date;

DROP TABLE IF EXISTS test_run_metadata;
DROP TABLE IF EXISTS regressions;
