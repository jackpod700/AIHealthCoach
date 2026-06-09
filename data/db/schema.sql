CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,

    height_cm DECIMAL(5,2),
    current_weight_kg DECIMAL(5,2),
    target_weight_kg DECIMAL(5,2),
    goal_type VARCHAR(20),

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_chat_message_role
        CHECK (role IN ('USER', 'ASSISTANT')),

    CONSTRAINT fk_chat_message_users
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS foods (
    id BIGSERIAL PRIMARY KEY,
    source_key VARCHAR(40) NOT NULL,
    source_url TEXT NOT NULL,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(255),
    serving_description VARCHAR(100),
    serving_key VARCHAR(100) GENERATED ALWAYS AS (COALESCE(serving_description, '')) STORED,
    serving_size NUMERIC(8,2),
    serving_unit VARCHAR(20),
    calories NUMERIC(8,2),
    fat NUMERIC(8,2),
    carbohydrate NUMERIC(8,2),
    protein NUMERIC(8,2),
    content_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_foods_source_serving
        UNIQUE (source_key, serving_key),
    CONSTRAINT chk_foods_name_not_blank
        CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_foods_nutrients_non_negative
        CHECK (
            (calories IS NULL OR calories >= 0)
            AND (fat IS NULL OR fat >= 0)
            AND (carbohydrate IS NULL OR carbohydrate >= 0)
            AND (protein IS NULL OR protein >= 0)
        )
);

CREATE TABLE IF NOT EXISTS meals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    meal_type VARCHAR(10) NOT NULL,
    meal_date DATE NOT NULL DEFAULT CURRENT_DATE,

    CONSTRAINT chk_meals_meal_type
        CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')),
    CONSTRAINT uq_meals_user_type_date
        UNIQUE (user_id, meal_type, meal_date),
    CONSTRAINT fk_meals_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS meal_items (
    meal_id BIGINT NOT NULL,
    -- food_id CHAR(19) NOT NULL,
    food_id BIGINT NOT NULL,
    quantity NUMERIC(8,2) NOT NULL DEFAULT 1.00,

    CONSTRAINT pk_meal_items
        PRIMARY KEY (meal_id, food_id),
    CONSTRAINT chk_meal_items_quantity_positive
        CHECK (quantity > 0),
    CONSTRAINT fk_meal_items_meal
        FOREIGN KEY (meal_id)
        REFERENCES meals(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_meal_items_food
        FOREIGN KEY (food_id)
        REFERENCES foods(id)
);

CREATE INDEX IF NOT EXISTS idx_meals_user_date
    ON meals(user_id, meal_date);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_meals_user_type_date'
    ) THEN
        ALTER TABLE meals
            ADD CONSTRAINT uq_meals_user_type_date
            UNIQUE (user_id, meal_type, meal_date);
    END IF;
END $$;
