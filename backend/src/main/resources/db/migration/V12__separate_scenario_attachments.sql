CREATE OR REPLACE FUNCTION report_parse_scenario(raw TEXT)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN raw::JSONB;
EXCEPTION WHEN others THEN
    RETURN jsonb_build_object(
        'steps', jsonb_build_array(
            jsonb_build_object(
                'number', 1,
                'text', raw,
                'subSteps', '[]'::JSONB,
                'parameters', '[]'::JSONB
            )
        )
    );
END;
$$;

ALTER TABLE test_report
    ALTER COLUMN scenario TYPE JSONB USING report_parse_scenario(scenario);

CREATE TABLE test_attachment (
    id BIGSERIAL PRIMARY KEY,
    test_id TEXT NOT NULL REFERENCES test_report(test_id) ON UPDATE CASCADE ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    name TEXT,
    media_type TEXT,
    content TEXT NOT NULL,
    source TEXT,
    size_bytes BIGINT
);

CREATE INDEX idx_test_attachment_test_step ON test_attachment(test_id, step_number);

INSERT INTO test_attachment(test_id, step_number, name, media_type, content, source, size_bytes)
SELECT report.test_id,
       numbered.step_number,
       attachment.value ->> 'name',
       attachment.value ->> 'mediaType',
       COALESCE(attachment.value ->> 'content', ''),
       attachment.value ->> 'source',
       CASE
           WHEN attachment.value ->> 'sizeBytes' ~ '^\d+$' THEN (attachment.value ->> 'sizeBytes')::BIGINT
           ELSE NULL
       END
FROM test_report report
CROSS JOIN LATERAL (
    SELECT step.value, step.ordinality::INTEGER AS step_number
    FROM jsonb_path_query(report.scenario, '$.steps.** ? (@.type() == "object" && exists(@.number))')
         WITH ORDINALITY AS step(value, ordinality)
) numbered
CROSS JOIN LATERAL jsonb_array_elements(COALESCE(numbered.value -> 'attachments', '[]'::JSONB)) attachment(value);

CREATE OR REPLACE FUNCTION report_strip_attachments(nodes JSONB)
RETURNS JSONB
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT COALESCE(
        jsonb_agg(
            (node - 'attachments' - 'stepNumber') ||
            jsonb_build_object('subSteps', report_strip_attachments(COALESCE(node -> 'subSteps', '[]'::JSONB)))
            ORDER BY ordinality
        ),
        '[]'::JSONB
    )
    FROM jsonb_array_elements(nodes) WITH ORDINALITY AS item(node, ordinality);
$$;

UPDATE test_report
SET scenario = jsonb_build_object(
    'steps', report_strip_attachments(COALESCE(scenario -> 'steps', '[]'::JSONB))
);

DROP FUNCTION report_strip_attachments(JSONB);
DROP FUNCTION report_parse_scenario(TEXT);
