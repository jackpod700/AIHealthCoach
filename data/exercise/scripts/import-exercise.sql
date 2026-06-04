CREATE TEMP TABLE staging_physical_activities (
    compendium_code VARCHAR(20),
    compendium_version VARCHAR(20),
    major_heading VARCHAR(100),
    met_value NUMERIC(4,1),
    description TEXT
);

\copy staging_physical_activities (compendium_code, compendium_version, major_heading, met_value, description) FROM '/tmp/processed-exercise.csv' WITH (FORMAT csv, HEADER true);

INSERT INTO physical_activities (
    compendium_code,
    compendium_version,
    major_heading,
    met_value,
    description
)
SELECT
    compendium_code,
    compendium_version,
    major_heading,
    met_value,
    description
FROM staging_physical_activities
ON CONFLICT (compendium_code) DO UPDATE SET
    compendium_version = EXCLUDED.compendium_version,
    major_heading = EXCLUDED.major_heading,
    met_value = EXCLUDED.met_value,
    description = EXCLUDED.description;

SELECT setval('physical_activities_id_seq', COALESCE((SELECT MAX(id) FROM physical_activities), 1));
