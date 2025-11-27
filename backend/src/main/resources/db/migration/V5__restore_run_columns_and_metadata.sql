ALTER TABLE test_report
    ADD COLUMN IF NOT EXISTS run_1_status TEXT,
    ADD COLUMN IF NOT EXISTS run_2_status TEXT,
    ADD COLUMN IF NOT EXISTS run_3_status TEXT,
    ADD COLUMN IF NOT EXISTS run_4_status TEXT,
    ADD COLUMN IF NOT EXISTS run_5_status TEXT;

CREATE TABLE IF NOT EXISTS test_run_metadata (
    run_index INT PRIMARY KEY,
    run_date DATE
);

INSERT INTO test_run_metadata (run_index, run_date) VALUES
    (1, NULL),
    (2, NULL),
    (3, NULL),
    (4, NULL),
    (5, NULL)
ON CONFLICT (run_index) DO NOTHING;
