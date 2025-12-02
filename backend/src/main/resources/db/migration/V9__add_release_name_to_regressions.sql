ALTER TABLE regressions
    ADD COLUMN IF NOT EXISTS release_name TEXT;

UPDATE regressions
SET release_name = COALESCE(NULLIF(TRIM(release_name), ''), TO_CHAR(regression_date, 'YYYY-MM-DD'))
WHERE release_name IS NULL OR release_name = '';

ALTER TABLE regressions
    ALTER COLUMN release_name SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_regressions_release_name_unique ON regressions (release_name);
ALTER TABLE regressions DROP CONSTRAINT IF EXISTS regressions_regression_date_key;
