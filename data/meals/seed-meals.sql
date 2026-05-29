INSERT INTO meals (user_id, meal_type, meal_date)
VALUES
    (1, 'BREAKFAST', CURRENT_DATE),
    (1, 'LUNCH', CURRENT_DATE),
    (1, 'DINNER', CURRENT_DATE),
    (1, 'SNACK', CURRENT_DATE),
    (1, 'BREAKFAST', CURRENT_DATE - INTERVAL '1 day'),
    (1, 'LUNCH', CURRENT_DATE - INTERVAL '1 day'),
    (1, 'DINNER', CURRENT_DATE - INTERVAL '1 day'),
    (1, 'BREAKFAST', CURRENT_DATE - INTERVAL '2 days'),
    (1, 'DINNER', CURRENT_DATE - INTERVAL '2 days')
ON CONFLICT (user_id, meal_type, meal_date) DO NOTHING;

WITH meal_foods(meal_type, meal_date, food_pattern, quantity) AS (
    VALUES
        ('BREAKFAST', CURRENT_DATE, '%밥%', 1.00),
        ('BREAKFAST', CURRENT_DATE, '%달걀%', 1.00),
        ('BREAKFAST', CURRENT_DATE, '%우유%', 1.00),
        ('LUNCH', CURRENT_DATE, '%김치찌개%', 1.00),
        ('LUNCH', CURRENT_DATE, '%밥%', 1.00),
        ('LUNCH', CURRENT_DATE, '%닭가슴살%', 1.00),
        ('DINNER', CURRENT_DATE, '%피자%', 1.00),
        ('DINNER', CURRENT_DATE, '%샐러드%', 1.00),
        ('SNACK', CURRENT_DATE, '%젤리%', 1.00),
        ('BREAKFAST', CURRENT_DATE - INTERVAL '1 day', '%그릭요거트%', 1.00),
        ('BREAKFAST', CURRENT_DATE - INTERVAL '1 day', '%블루베리%', 1.00),
        ('LUNCH', CURRENT_DATE - INTERVAL '1 day', '%비빔밥%', 1.00),
        ('LUNCH', CURRENT_DATE - INTERVAL '1 day', '%두부%', 1.00),
        ('DINNER', CURRENT_DATE - INTERVAL '1 day', '%연어%', 1.00),
        ('DINNER', CURRENT_DATE - INTERVAL '1 day', '%샐러드%', 1.00),
        ('BREAKFAST', CURRENT_DATE - INTERVAL '2 days', '%바나나%', 1.00),
        ('BREAKFAST', CURRENT_DATE - INTERVAL '2 days', '%우유%', 1.00),
        ('DINNER', CURRENT_DATE - INTERVAL '2 days', '%김치볶음밥%', 1.00),
        ('DINNER', CURRENT_DATE - INTERVAL '2 days', '%계란%', 1.00)
)
INSERT INTO meal_items (meal_id, food_code, quantity)
SELECT meal.id, food.code, meal_foods.quantity
FROM meal_foods
JOIN meals meal
    ON meal.user_id = 1
    AND meal.meal_type = meal_foods.meal_type
    AND meal.meal_date = meal_foods.meal_date::DATE
JOIN LATERAL (
    SELECT code
    FROM foods
    WHERE name LIKE meal_foods.food_pattern
    ORDER BY name
    LIMIT 1
) food ON TRUE
ON CONFLICT (meal_id, food_code) DO NOTHING;

SELECT COUNT(*) AS meal_count
FROM meals;

SELECT COUNT(*) AS meal_item_count
FROM meal_items;
