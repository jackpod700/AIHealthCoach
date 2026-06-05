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
    compendium_code VARCHAR(20),
    compendium_version VARCHAR(20),
    major_heading VARCHAR(100),
    met_value NUMERIC(4,1),
    source_description TEXT,
    activity_name_ko VARCHAR(100),
    intensity_level VARCHAR(10),
    met_source VARCHAR(20)
);

\copy staging_exercise_activity_options (compendium_code, compendium_version, major_heading, met_value, source_description, activity_name_ko, intensity_level, met_source) FROM '/tmp/exercise-activity-options.csv' WITH (FORMAT csv, HEADER true);

INSERT INTO exercise_activity_options (
    physical_activity_id,
    compendium_code,
    compendium_version,
    major_heading,
    met_value,
    source_description,
    activity_name_ko,
    intensity_level,
    met_source,
    created_at,
    updated_at
)
SELECT
    pa.id,
    options.compendium_code,
    options.compendium_version,
    options.major_heading,
    options.met_value,
    options.source_description,
    options.activity_name_ko,
    options.intensity_level,
    options.met_source,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM staging_exercise_activity_options options
JOIN physical_activities pa ON pa.compendium_code = options.compendium_code
ON CONFLICT (activity_name_ko, intensity_level) DO UPDATE SET
    physical_activity_id = EXCLUDED.physical_activity_id,
    compendium_code = EXCLUDED.compendium_code,
    compendium_version = EXCLUDED.compendium_version,
    major_heading = EXCLUDED.major_heading,
    met_value = EXCLUDED.met_value,
    source_description = EXCLUDED.source_description,
    met_source = EXCLUDED.met_source,
    updated_at = CURRENT_TIMESTAMP;

DELETE FROM exercise_activity_options stored_options
WHERE NOT EXISTS (
    SELECT 1
    FROM staging_exercise_activity_options options
    WHERE options.activity_name_ko = stored_options.activity_name_ko
      AND options.intensity_level = stored_options.intensity_level
);

SELECT setval('exercise_activity_options_id_seq', COALESCE((SELECT MAX(id) FROM exercise_activity_options), 1));
