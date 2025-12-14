UPDATE test_report
SET category = COALESCE(NULLIF(category, ''), 'Unknown')
WHERE category IS NULL OR category = '';

UPDATE test_report
SET short_title = COALESCE(NULLIF(short_title, ''), 'Untitled')
WHERE short_title IS NULL OR short_title = '';

UPDATE test_report
SET scenario = COALESCE(NULLIF(scenario, ''), 'Scenario is not provided')
WHERE scenario IS NULL OR scenario = '';

ALTER TABLE test_report ALTER COLUMN category SET NOT NULL;
ALTER TABLE test_report ALTER COLUMN short_title SET NOT NULL;
ALTER TABLE test_report ALTER COLUMN scenario SET NOT NULL;
ALTER TABLE test_report ALTER COLUMN priority SET NOT NULL;
