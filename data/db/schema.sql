CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    nickname VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_users_role
        CHECK (role IN ('USER', 'ADMIN'))
);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_users_role'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,

    height_cm DECIMAL(5,2),
    current_weight_kg DECIMAL(5,2),
    target_weight_kg DECIMAL(5,2),
    goal_type VARCHAR(20),
    gender VARCHAR(20),
    age INTEGER,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_user_profiles_gender
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE')),

    CONSTRAINT chk_user_profiles_age_positive
        CHECK (age IS NULL OR age > 0),

    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS gender VARCHAR(20),
    ADD COLUMN IF NOT EXISTS age INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_user_profiles_gender'
    ) THEN
        ALTER TABLE user_profiles
            ADD CONSTRAINT chk_user_profiles_gender
            CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_user_profiles_age_positive'
    ) THEN
        ALTER TABLE user_profiles
            ADD CONSTRAINT chk_user_profiles_age_positive
            CHECK (age IS NULL OR age > 0);
    END IF;
END $$;

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

CREATE TABLE IF NOT EXISTS user_memories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_memories_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_user_memories_content_not_blank
        CHECK (length(trim(content)) > 0),

    CONSTRAINT chk_user_memories_content_length
        CHECK (char_length(content) <= 500)
);

CREATE INDEX IF NOT EXISTS idx_user_memories_active
    ON user_memories(user_id, updated_at DESC)
    WHERE is_active = TRUE;

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

CREATE TABLE IF NOT EXISTS food_submission_requests (
    id BIGSERIAL PRIMARY KEY,
    submitted_by_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(255),
    serving_description VARCHAR(100),
    serving_size NUMERIC(8,2),
    serving_unit VARCHAR(20),
    calories NUMERIC(8,2),
    carbohydrate NUMERIC(8,2),
    protein NUMERIC(8,2),
    fat NUMERIC(8,2),
    admin_note TEXT,
    rejection_reason TEXT,
    approved_food_id BIGINT,
    reviewed_by_admin_id BIGINT,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_food_submission_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_food_submission_requests_name_not_blank
        CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_food_submission_requests_serving_required
        CHECK (
            serving_description IS NOT NULL
            OR (serving_size IS NOT NULL AND serving_unit IS NOT NULL)
        ),
    CONSTRAINT chk_food_submission_requests_nutrients_non_negative
        CHECK (
            (calories IS NULL OR calories >= 0)
            AND (carbohydrate IS NULL OR carbohydrate >= 0)
            AND (protein IS NULL OR protein >= 0)
            AND (fat IS NULL OR fat >= 0)
        ),
    CONSTRAINT fk_food_submission_requests_submitter
        FOREIGN KEY (submitted_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_food_submission_requests_approved_food
        FOREIGN KEY (approved_food_id) REFERENCES foods(id),
    CONSTRAINT fk_food_submission_requests_reviewer
        FOREIGN KEY (reviewed_by_admin_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_food_submission_requests_submitter
    ON food_submission_requests(submitted_by_user_id, submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_food_submission_requests_status
    ON food_submission_requests(status, submitted_at DESC);

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

CREATE TABLE IF NOT EXISTS physical_activities (
    id BIGSERIAL PRIMARY KEY,
    compendium_code VARCHAR(20) NOT NULL UNIQUE,
    compendium_version VARCHAR(20) NOT NULL,
    major_heading VARCHAR(100) NOT NULL,
    met_value DECIMAL(4,1) NOT NULL,
    description TEXT NOT NULL
);

ALTER TABLE physical_activities
    ADD COLUMN IF NOT EXISTS major_heading VARCHAR(100);

CREATE TABLE IF NOT EXISTS exercise_activity_options (
    id BIGSERIAL PRIMARY KEY,
    activity_name_ko VARCHAR(100) NOT NULL,
    major_heading VARCHAR(100) NOT NULL,
    low_physical_activity_id BIGINT NOT NULL,
    low_met_value DECIMAL(4,1) NOT NULL,
    low_source_description TEXT NOT NULL,
    low_met_source VARCHAR(20) NOT NULL,
    medium_physical_activity_id BIGINT NOT NULL,
    medium_met_value DECIMAL(4,1) NOT NULL,
    medium_source_description TEXT NOT NULL,
    medium_met_source VARCHAR(20) NOT NULL,
    high_physical_activity_id BIGINT NOT NULL,
    high_met_value DECIMAL(4,1) NOT NULL,
    high_source_description TEXT NOT NULL,
    high_met_source VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_exercise_activity_options_name
        UNIQUE (activity_name_ko),

    CONSTRAINT fk_exercise_activity_options_low_physical_activity
        FOREIGN KEY (low_physical_activity_id)
        REFERENCES physical_activities(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_exercise_activity_options_medium_physical_activity
        FOREIGN KEY (medium_physical_activity_id)
        REFERENCES physical_activities(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_exercise_activity_options_high_physical_activity
        FOREIGN KEY (high_physical_activity_id)
        REFERENCES physical_activities(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_exercise_activity_options_met_source
        CHECK (
            low_met_source IN ('COMPENDIUM', 'ESTIMATED')
            AND medium_met_source IN ('COMPENDIUM', 'ESTIMATED')
            AND high_met_source IN ('COMPENDIUM', 'ESTIMATED')
        ),

    CONSTRAINT chk_exercise_activity_options_met_positive
        CHECK (
            low_met_value >= 0
            AND medium_met_value >= 0
            AND high_met_value >= 0
        )
);

CREATE INDEX IF NOT EXISTS idx_exercise_activity_options_search
    ON exercise_activity_options(activity_name_ko);

CREATE TABLE IF NOT EXISTS exercise_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exercise_activity_option_id BIGINT NOT NULL,
    intensity_level VARCHAR(10) NOT NULL,
    exercise_date DATE NOT NULL,
    duration_minutes INTEGER NOT NULL,
    calories_burned INTEGER NOT NULL,
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_exercise_records_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_exercise_records_activity_option
        FOREIGN KEY (exercise_activity_option_id)
        REFERENCES exercise_activity_options(id),

    CONSTRAINT chk_exercise_records_intensity
        CHECK (intensity_level IN ('LOW', 'MEDIUM', 'HIGH')),

    CONSTRAINT chk_exercise_duration_positive
        CHECK (duration_minutes > 0),

    CONSTRAINT chk_exercise_calories_non_negative
        CHECK (calories_burned >= 0)
);

CREATE INDEX IF NOT EXISTS idx_exercise_records_user_date
    ON exercise_records(user_id, exercise_date);

CREATE INDEX IF NOT EXISTS idx_exercise_records_activity_option
    ON exercise_records(exercise_activity_option_id);

CREATE TABLE IF NOT EXISTS daily_goals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    goal_type VARCHAR(20) NOT NULL,
    calorie_intake_goal INTEGER NOT NULL,
    exercise_calorie_goal INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_daily_goals_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_daily_goals_goal_type
        CHECK (goal_type IN ('WEIGHT_LOSS', 'MAINTENANCE', 'MUSCLE_GAIN')),

    CONSTRAINT chk_daily_goals_calorie_intake_positive
        CHECK (calorie_intake_goal > 0),

    CONSTRAINT chk_daily_goals_exercise_calorie_non_negative
        CHECK (exercise_calorie_goal >= 0)
);

CREATE TABLE IF NOT EXISTS weight_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    weight_kg NUMERIC(5, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_weight_records_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_weight_records_user_date
        UNIQUE (user_id, record_date),

    CONSTRAINT chk_weight_records_weight_positive
        CHECK (weight_kg > 0),

    CONSTRAINT chk_weight_records_weight_max
        CHECK (weight_kg <= 500)
);

CREATE INDEX IF NOT EXISTS idx_weight_records_user_date
    ON weight_records(user_id, record_date);
CREATE TABLE IF NOT EXISTS oauth_accounts (
    id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    email VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_oauth_accounts PRIMARY KEY (id),
    CONSTRAINT fk_oauth_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_oauth_accounts_provider_provider_user_id
        UNIQUE (provider, provider_user_id)
);
ALTER TABLE users
ALTER COLUMN password DROP NOT NULL;
