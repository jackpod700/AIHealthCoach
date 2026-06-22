BEGIN;

DELETE FROM meal_items
WHERE meal_id IN (
    SELECT id
    FROM meals
    WHERE user_id BETWEEN 920001 AND 921000
);

DELETE FROM meal_items
WHERE food_id BETWEEN 920001 AND 920004;

DELETE FROM meals
WHERE user_id BETWEEN 920001 AND 921000;

DELETE FROM exercise_records
WHERE user_id BETWEEN 920001 AND 921000;

DELETE FROM exercise_records
WHERE exercise_activity_option_id = 920001;

DELETE FROM weight_records
WHERE user_id BETWEEN 920001 AND 921000;

DELETE FROM daily_goals
WHERE user_id BETWEEN 920001 AND 921000;

DELETE FROM chat_messages
WHERE user_id BETWEEN 920001 AND 921000;

DELETE FROM daily_chat_summaries
WHERE user_id BETWEEN 920001 AND 921000;

DELETE FROM daily_chat_summary_states
WHERE user_id BETWEEN 920001 AND 921000;

DELETE FROM users
WHERE id BETWEEN 920001 AND 921000;

DELETE FROM exercise_activity_options
WHERE id = 920001;

DELETE FROM physical_activities
WHERE id BETWEEN 920001 AND 920003;

DELETE FROM foods
WHERE id BETWEEN 920001 AND 920004;

INSERT INTO users (
    id,
    email,
    password,
    nickname,
    role,
    created_at,
    updated_at
)
SELECT id,
       'benchmark-summary-' || id || '@example.com',
       'benchmark-password',
       'summary-benchmark-' || id,
       'USER',
       NOW(),
       NOW()
FROM generate_series(920001, 921000) AS id;

INSERT INTO foods (
    id,
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
    content_hash,
    created_at,
    updated_at
)
VALUES
    (920001, 'benchmark-summary-food-1', 'benchmark://summary/food/1', '벤치마크 닭가슴살', 'benchmark', '100g', 100, 'g', 165, 3.6, 0, 31, 'benchmark-summary-food-hash-1', NOW(), NOW()),
    (920002, 'benchmark-summary-food-2', 'benchmark://summary/food/2', '벤치마크 현미밥', 'benchmark', '1공기', 210, 'g', 320, 2.4, 70, 6, 'benchmark-summary-food-hash-2', NOW(), NOW()),
    (920003, 'benchmark-summary-food-3', 'benchmark://summary/food/3', '벤치마크 샐러드', 'benchmark', '1접시', 180, 'g', 120, 7, 12, 5, 'benchmark-summary-food-hash-3', NOW(), NOW()),
    (920004, 'benchmark-summary-food-4', 'benchmark://summary/food/4', '벤치마크 요거트', 'benchmark', '1개', 150, 'g', 140, 4, 18, 9, 'benchmark-summary-food-hash-4', NOW(), NOW());

INSERT INTO physical_activities (
    id,
    compendium_code,
    compendium_version,
    major_heading,
    met_value,
    description
)
VALUES
    (920001, 'BM-SUM-LOW', '2024', 'benchmark', 2.5, 'summary benchmark low activity'),
    (920002, 'BM-SUM-MED', '2024', 'benchmark', 5.0, 'summary benchmark medium activity'),
    (920003, 'BM-SUM-HIGH', '2024', 'benchmark', 8.0, 'summary benchmark high activity');

INSERT INTO exercise_activity_options (
    id,
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
VALUES (
    920001,
    '벤치마크 서머리 운동',
    'benchmark',
    920001,
    2.5,
    'summary benchmark low',
    'ESTIMATED',
    920002,
    5.0,
    'summary benchmark medium',
    'ESTIMATED',
    920003,
    8.0,
    'summary benchmark high',
    'ESTIMATED',
    NOW(),
    NOW()
);

WITH users AS (
    SELECT id AS user_id
    FROM generate_series(920001, 921000) AS id
)
INSERT INTO daily_goals (
    user_id,
    goal_type,
    calorie_intake_goal,
    exercise_calorie_goal,
    created_at,
    updated_at
)
SELECT user_id,
       'WEIGHT_LOSS',
       1900,
       300,
       NOW(),
       NOW()
FROM users;

WITH user_days AS (
    SELECT users.id AS user_id,
           days.day_offset,
           CURRENT_DATE - days.day_offset AS record_date
    FROM generate_series(920001, 921000) AS users(id)
    CROSS JOIN generate_series(1, 6) AS days(day_offset)
),
chat_rows AS (
    SELECT user_id,
           record_date,
           message_no,
           CASE WHEN message_no % 2 = 1 THEN 'USER' ELSE 'ASSISTANT' END AS role
    FROM user_days
    CROSS JOIN generate_series(1, 6) AS message_no
)
INSERT INTO chat_messages (
    user_id,
    role,
    content,
    created_at
)
SELECT user_id,
       role,
       'benchmark raw chat message user=' || user_id || ', date=' || record_date || ', message=' || message_no,
       record_date::TIMESTAMP + ((8 + message_no) || ' hours')::INTERVAL
FROM chat_rows;

WITH user_days AS (
    SELECT users.id AS user_id,
           days.day_offset,
           CURRENT_DATE - days.day_offset AS meal_date
    FROM generate_series(920001, 921000) AS users(id)
    CROSS JOIN generate_series(1, 6) AS days(day_offset)
),
meal_rows AS (
    SELECT user_id,
           meal_date,
           meal_type
    FROM user_days
    CROSS JOIN (
        VALUES ('BREAKFAST'), ('LUNCH'), ('DINNER'), ('SNACK')
    ) AS types(meal_type)
)
INSERT INTO meals (
    user_id,
    meal_type,
    meal_date
)
SELECT user_id,
       meal_type,
       meal_date
FROM meal_rows;

INSERT INTO meal_items (
    meal_id,
    food_id,
    quantity
)
SELECT meals.id,
       CASE meals.meal_type
           WHEN 'BREAKFAST' THEN 920004
           WHEN 'LUNCH' THEN 920002
           WHEN 'DINNER' THEN 920001
           ELSE 920003
       END AS food_id,
       1.00
FROM meals
WHERE meals.user_id BETWEEN 920001 AND 921000;

WITH user_days AS (
    SELECT users.id AS user_id,
           days.day_offset,
           CURRENT_DATE - days.day_offset AS exercise_date
    FROM generate_series(920001, 921000) AS users(id)
    CROSS JOIN generate_series(0, 6) AS days(day_offset)
),
exercise_rows AS (
    SELECT user_id,
           exercise_date,
           exercise_no
    FROM user_days
    CROSS JOIN generate_series(1, 2) AS exercise_no
)
INSERT INTO exercise_records (
    user_id,
    exercise_activity_option_id,
    intensity_level,
    exercise_date,
    duration_minutes,
    calories_burned,
    memo,
    created_at,
    updated_at
)
SELECT user_id,
       920001,
       CASE WHEN exercise_no = 1 THEN 'MEDIUM' ELSE 'HIGH' END,
       exercise_date,
       CASE WHEN exercise_no = 1 THEN 35 ELSE 20 END,
       CASE WHEN exercise_no = 1 THEN 210 ELSE 180 END,
       'benchmark raw exercise user=' || user_id || ', date=' || exercise_date || ', no=' || exercise_no,
       NOW(),
       NOW()
FROM exercise_rows;

WITH user_days AS (
    SELECT users.id AS user_id,
           days.day_offset,
           CURRENT_DATE - days.day_offset AS record_date
    FROM generate_series(920001, 921000) AS users(id)
    CROSS JOIN generate_series(0, 6) AS days(day_offset)
)
INSERT INTO weight_records (
    user_id,
    record_date,
    weight_kg,
    created_at,
    updated_at
)
SELECT user_id,
       record_date,
       70.00 - (day_offset * 0.03),
       NOW(),
       NOW()
FROM user_days;

WITH user_days AS (
    SELECT users.id AS user_id,
           days.day_offset,
           CURRENT_DATE - days.day_offset AS summary_date,
           1000 + days.day_offset AS source_version
    FROM generate_series(920001, 921000) AS users(id)
    CROSS JOIN generate_series(0, 6) AS days(day_offset)
)
INSERT INTO daily_chat_summary_states (
    user_id,
    summary_date,
    source_version,
    source_updated_at,
    status,
    retry_count,
    changed_sources,
    daily_goal_snapshot_payload,
    created_at,
    updated_at
)
SELECT user_id,
       summary_date,
       source_version,
       NOW() - (day_offset || ' days')::INTERVAL,
       'FRESH',
       0,
       'CHAT,MEAL,EXERCISE,WEIGHT,DAILY_GOAL',
       '{"goalType":"WEIGHT_LOSS","targetCalories":1900,"targetExerciseCalories":300}',
       NOW(),
       NOW()
FROM user_days;

WITH user_days AS (
    SELECT users.id AS user_id,
           days.day_offset,
           CURRENT_DATE - days.day_offset AS summary_date,
           1000 + days.day_offset AS source_version
    FROM generate_series(920001, 921000) AS users(id)
    CROSS JOIN generate_series(0, 6) AS days(day_offset)
)
INSERT INTO daily_chat_summaries (
    user_id,
    summary_date,
    content,
    source_version,
    created_at,
    updated_at
)
SELECT user_id,
       summary_date,
       '사용자는 아침과 점심을 비교적 균형 있게 기록했고, 저녁에는 단백질 위주의 식사를 했다. '
       || '운동은 중강도 활동을 완료했으며 체중 기록도 함께 남겼다. '
       || '섭취 목표와 운동 목표의 진행률을 확인했고, 다음 날에는 야식과 간식량을 조금 더 조절하면 좋다. '
       || 'benchmark_user=' || user_id || ', day_offset=' || day_offset AS content,
       source_version,
       NOW(),
       NOW()
FROM user_days;

WITH ranked_recent_window AS (
    SELECT user_id,
           summary_date,
           ROW_NUMBER() OVER (ORDER BY user_id, summary_date) AS row_number,
           COUNT(*) OVER () AS total_count
    FROM daily_chat_summary_states
    WHERE user_id BETWEEN 920001 AND 921000
      AND summary_date >= CURRENT_DATE - INTERVAL '6 days'
      AND summary_date <= CURRENT_DATE - INTERVAL '1 day'
),
changed_targets AS (
    SELECT user_id,
           summary_date
    FROM ranked_recent_window
    WHERE row_number <= FLOOR(total_count * (:CHANGE_RATE::NUMERIC / 100.0))
)
UPDATE daily_chat_summary_states states
SET status = 'STALE',
    source_version = states.source_version + 1,
    source_updated_at = NOW(),
    changed_sources = 'MEAL',
    updated_at = NOW()
FROM changed_targets targets
WHERE states.user_id = targets.user_id
  AND states.summary_date = targets.summary_date;

COMMIT;

ANALYZE users;
ANALYZE chat_messages;
ANALYZE meals;
ANALYZE meal_items;
ANALYZE foods;
ANALYZE exercise_records;
ANALYZE exercise_activity_options;
ANALYZE weight_records;
ANALYZE daily_goals;
ANALYZE daily_chat_summaries;
ANALYZE daily_chat_summary_states;
