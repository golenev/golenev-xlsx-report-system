CREATE TABLE IF NOT EXISTS regressions (
    id BIGSERIAL PRIMARY KEY,
    regression_date DATE NOT NULL UNIQUE,
    payload JSONB NOT NULL
);

ALTER TABLE test_report
    ADD COLUMN IF NOT EXISTS regression_status TEXT;

ALTER TABLE test_report
    DROP COLUMN IF EXISTS run_1_status,
    DROP COLUMN IF EXISTS run_2_status,
    DROP COLUMN IF EXISTS run_3_status,
    DROP COLUMN IF EXISTS run_4_status,
    DROP COLUMN IF EXISTS run_5_status;

DROP TABLE IF EXISTS test_run_metadata;
