\timing on

-- DB direct path: current task11 query shape.
SELECT summaries.summary_date,
       summaries.content
FROM daily_chat_summaries summaries
INNER JOIN daily_chat_summary_states states
    ON states.user_id = summaries.user_id
   AND states.summary_date = summaries.summary_date
   AND states.source_version = summaries.source_version
   AND states.status = 'FRESH'
WHERE summaries.user_id = 920002
  AND summaries.summary_date >= CURRENT_DATE - INTERVAL '6 days'
  AND summaries.summary_date <= CURRENT_DATE - INTERVAL '1 day'
ORDER BY summaries.summary_date ASC;

-- Version marker path: lightweight check required before trusting a cache hit.
SELECT states.summary_date,
       states.source_version
FROM daily_chat_summary_states states
INNER JOIN daily_chat_summaries summaries
    ON summaries.user_id = states.user_id
   AND summaries.summary_date = states.summary_date
   AND summaries.source_version = states.source_version
WHERE states.user_id = 920002
  AND states.summary_date >= CURRENT_DATE - INTERVAL '6 days'
  AND states.summary_date <= CURRENT_DATE - INTERVAL '1 day'
  AND states.status = 'FRESH'
ORDER BY states.summary_date ASC;

-- Safety path: stale, failed, and version mismatch rows must be excluded.
SELECT summaries.summary_date,
       summaries.content,
       summaries.source_version
FROM daily_chat_summaries summaries
INNER JOIN daily_chat_summary_states states
    ON states.user_id = summaries.user_id
   AND states.summary_date = summaries.summary_date
   AND states.source_version = summaries.source_version
   AND states.status = 'FRESH'
WHERE summaries.user_id = 920001
  AND summaries.summary_date >= CURRENT_DATE - INTERVAL '6 days'
  AND summaries.summary_date <= CURRENT_DATE - INTERVAL '1 day'
ORDER BY summaries.summary_date ASC;

-- Raw source full lookup path: what summary/cache avoids for the recent completed 6-day window.
SELECT id, role, content, created_at
FROM chat_messages
WHERE user_id = 920002
  AND created_at >= (CURRENT_DATE - INTERVAL '6 days')::TIMESTAMP
  AND created_at < CURRENT_DATE::TIMESTAMP
ORDER BY created_at ASC, id ASC;

SELECT meals.id,
       meals.meal_type,
       meals.meal_date,
       meal_items.food_id,
       meal_items.quantity,
       foods.name,
       foods.calories,
       foods.carbohydrate,
       foods.protein,
       foods.fat
FROM meals
LEFT JOIN meal_items
    ON meal_items.meal_id = meals.id
LEFT JOIN foods
    ON foods.id = meal_items.food_id
WHERE meals.user_id = 920002
  AND meals.meal_date >= CURRENT_DATE - INTERVAL '6 days'
  AND meals.meal_date < CURRENT_DATE
ORDER BY meals.meal_date ASC, meals.id ASC, meal_items.food_id ASC;

SELECT id,
       exercise_activity_option_id,
       intensity_level,
       exercise_date,
       duration_minutes,
       calories_burned,
       memo
FROM exercise_records
WHERE user_id = 920002
  AND exercise_date >= CURRENT_DATE - INTERVAL '6 days'
  AND exercise_date < CURRENT_DATE
ORDER BY exercise_date ASC, id ASC;

SELECT id,
       record_date,
       weight_kg
FROM weight_records
WHERE user_id = 920002
  AND record_date >= CURRENT_DATE - INTERVAL '6 days'
  AND record_date < CURRENT_DATE
ORDER BY record_date ASC;

SELECT id,
       goal_type,
       calorie_intake_goal,
       exercise_calorie_goal,
       updated_at
FROM daily_goals
WHERE user_id = 920002;
