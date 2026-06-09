CREATE TEMP TABLE staging_foods (
    source_key VARCHAR(40),
    source_url TEXT,
    name VARCHAR(255),
    brand VARCHAR(255),
    serving_description VARCHAR(100),
    serving_size NUMERIC(8,2),
    serving_unit VARCHAR(20),
    calories NUMERIC(8,2),
    fat NUMERIC(8,2),
    carbohydrate NUMERIC(8,2),
    protein NUMERIC(8,2),
    content_hash VARCHAR(64)
);

\copy staging_foods (source_key, source_url, name, brand, serving_description, serving_size, serving_unit, calories, fat, carbohydrate, protein, content_hash) FROM '/tmp/foods.csv' WITH (FORMAT csv, HEADER true);

INSERT INTO foods (
    source_key,
    source_url,
    name,
    brand,
    serving_description,
    serving_size,
    serving_unit,
    calories,
    fat,
    carbohydrate,
    protein,
    content_hash
)
SELECT
    source_key,
    source_url,
    name,
    brand,
    serving_description,
    serving_size,
    serving_unit,
    calories,
    fat,
    carbohydrate,
    protein,
    content_hash
FROM (
    SELECT DISTINCT ON (source_key, COALESCE(serving_description, ''))
        source_key,
        source_url,
        name,
        brand,
        serving_description,
        serving_size,
        serving_unit,
        calories,
        fat,
        carbohydrate,
        protein,
        content_hash
    FROM staging_foods
    ORDER BY
        source_key,
        COALESCE(serving_description, ''),
        CASE WHEN source_url LIKE '%?%' THEN 1 ELSE 0 END,
        source_url
) deduped_foods
ON CONFLICT (source_key, serving_key) DO UPDATE SET
    source_url = EXCLUDED.source_url,
    name = EXCLUDED.name,
    brand = EXCLUDED.brand,
    serving_description = EXCLUDED.serving_description,
    serving_size = EXCLUDED.serving_size,
    serving_unit = EXCLUDED.serving_unit,
    calories = EXCLUDED.calories,
    fat = EXCLUDED.fat,
    carbohydrate = EXCLUDED.carbohydrate,
    protein = EXCLUDED.protein,
    content_hash = EXCLUDED.content_hash,
    updated_at = CURRENT_TIMESTAMP;
