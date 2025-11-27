CREATE TABLE IF NOT EXISTS regressions (
    id BIGSERIAL PRIMARY KEY,
    status TEXT NOT NULL,
    regression_date DATE NOT NULL UNIQUE,
    payload JSONB
);
