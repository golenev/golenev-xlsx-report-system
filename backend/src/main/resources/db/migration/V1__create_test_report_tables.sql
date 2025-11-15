CREATE TABLE IF NOT EXISTS test_report (
    id BIGSERIAL PRIMARY KEY,
    test_id TEXT NOT NULL UNIQUE,
    category TEXT,
    short_title TEXT,
    issue_link TEXT,
    ready_date DATE,
    general_status TEXT,
    scenario TEXT,
    notes TEXT,
    run_1_status TEXT,
    run_2_status TEXT,
    run_3_status TEXT,
    run_4_status TEXT,
    run_5_status TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

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
