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

CREATE TEMP TABLE staging_exercise_activity_options (
    activity_name_ko VARCHAR(100),
    major_heading VARCHAR(100),
    low_compendium_code VARCHAR(20),
    low_met_value NUMERIC(4,1),
    low_source_description TEXT,
    low_met_source VARCHAR(20),
    medium_compendium_code VARCHAR(20),
    medium_met_value NUMERIC(4,1),
    medium_source_description TEXT,
    medium_met_source VARCHAR(20),
    high_compendium_code VARCHAR(20),
    high_met_value NUMERIC(4,1),
    high_source_description TEXT,
    high_met_source VARCHAR(20)
);

\copy staging_exercise_activity_options (activity_name_ko, major_heading, low_compendium_code, low_met_value, low_source_description, low_met_source, medium_compendium_code, medium_met_value, medium_source_description, medium_met_source, high_compendium_code, high_met_value, high_source_description, high_met_source) FROM '/tmp/exercise-activity-options.csv' WITH (FORMAT csv, HEADER true);

INSERT INTO exercise_activity_options (
    activity_name_ko,
    major_heading,
    low_physical_activity_id,
    low_met_value,
    low_source_description,
    low_met_source,
    medium_physical_activity_id,
    medium_met_value,
    medium_source_description,
    medium_met_source,
    high_physical_activity_id,
    high_met_value,
    high_source_description,
    high_met_source,
    created_at,
    updated_at
)
SELECT
    options.activity_name_ko,
    options.major_heading,
    low_pa.id,
    options.low_met_value,
    options.low_source_description,
    options.low_met_source,
    medium_pa.id,
    options.medium_met_value,
    options.medium_source_description,
    options.medium_met_source,
    high_pa.id,
    options.high_met_value,
    options.high_source_description,
    options.high_met_source,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM staging_exercise_activity_options options
JOIN physical_activities low_pa ON low_pa.compendium_code = options.low_compendium_code
JOIN physical_activities medium_pa ON medium_pa.compendium_code = options.medium_compendium_code
JOIN physical_activities high_pa ON high_pa.compendium_code = options.high_compendium_code
ON CONFLICT (activity_name_ko) DO UPDATE SET
    major_heading = EXCLUDED.major_heading,
    low_physical_activity_id = EXCLUDED.low_physical_activity_id,
    low_met_value = EXCLUDED.low_met_value,
    low_source_description = EXCLUDED.low_source_description,
    low_met_source = EXCLUDED.low_met_source,
    medium_physical_activity_id = EXCLUDED.medium_physical_activity_id,
    medium_met_value = EXCLUDED.medium_met_value,
    medium_source_description = EXCLUDED.medium_source_description,
    medium_met_source = EXCLUDED.medium_met_source,
    high_physical_activity_id = EXCLUDED.high_physical_activity_id,
    high_met_value = EXCLUDED.high_met_value,
    high_source_description = EXCLUDED.high_source_description,
    high_met_source = EXCLUDED.high_met_source,
    updated_at = CURRENT_TIMESTAMP;

DELETE FROM exercise_activity_options stored_options
WHERE NOT EXISTS (
    SELECT 1
    FROM staging_exercise_activity_options options
    WHERE options.activity_name_ko = stored_options.activity_name_ko
);

SELECT setval('exercise_activity_options_id_seq', COALESCE((SELECT MAX(id) FROM exercise_activity_options), 1));
