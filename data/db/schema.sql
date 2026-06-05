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
    code CHAR(19) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(255),
    serving_size NUMERIC(6,2) NOT NULL DEFAULT 100.00,
    serving_unit VARCHAR(10) NOT NULL DEFAULT 'g',
    calories NUMERIC(8,2) NOT NULL DEFAULT 0.00,
    carbohydrate NUMERIC(8,2) NOT NULL DEFAULT 0.00,
    protein NUMERIC(8,2) NOT NULL DEFAULT 0.00,
    nat NUMERIC(8,2),
    fat NUMERIC(8,2) NOT NULL DEFAULT 0.00,
    sugar NUMERIC(8,2),
    water NUMERIC(8,2),
    dietary_fiber NUMERIC(8,2),
    calcium NUMERIC(8,2),
    iron NUMERIC(8,2),
    phosphorus NUMERIC(8,2),
    potassium NUMERIC(8,2),
    vitamin_a NUMERIC(8,2),
    vitamin_c NUMERIC(8,2),
    vitamin_d NUMERIC(8,2),
    cholesterol NUMERIC(8,2),
    saturated_fat NUMERIC(8,2),
    trans_fat NUMERIC(8,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_foods_name_not_blank
        CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_foods_serving_size_positive
        CHECK (serving_size > 0),
    CONSTRAINT chk_foods_required_nutrients_non_negative
        CHECK (
            calories >= 0
            AND carbohydrate >= 0
            AND protein >= 0
            AND fat >= 0
        ),
    CONSTRAINT chk_foods_optional_nutrients_non_negative
        CHECK (
            (nat IS NULL OR nat >= 0)
            AND (sugar IS NULL OR sugar >= 0)
            AND (water IS NULL OR water >= 0)
            AND (dietary_fiber IS NULL OR dietary_fiber >= 0)
            AND (calcium IS NULL OR calcium >= 0)
            AND (iron IS NULL OR iron >= 0)
            AND (phosphorus IS NULL OR phosphorus >= 0)
            AND (potassium IS NULL OR potassium >= 0)
            AND (vitamin_a IS NULL OR vitamin_a >= 0)
            AND (vitamin_c IS NULL OR vitamin_c >= 0)
            AND (vitamin_d IS NULL OR vitamin_d >= 0)
            AND (cholesterol IS NULL OR cholesterol >= 0)
            AND (saturated_fat IS NULL OR saturated_fat >= 0)
            AND (trans_fat IS NULL OR trans_fat >= 0)
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
    food_code CHAR(19) NOT NULL,
    quantity NUMERIC(8,2) NOT NULL DEFAULT 1.00,

    CONSTRAINT pk_meal_items
        PRIMARY KEY (meal_id, food_code),
    CONSTRAINT chk_meal_items_quantity_positive
        CHECK (quantity > 0),
    CONSTRAINT fk_meal_items_meal
        FOREIGN KEY (meal_id)
        REFERENCES meals(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_meal_items_food
        FOREIGN KEY (food_code)
        REFERENCES foods(code)
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
