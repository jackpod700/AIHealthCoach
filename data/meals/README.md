# 식단 더미데이터

`seed-meals.sql`은 개발용 식단 기록 더미데이터를 생성한다.

## 실행 순서

이 SQL은 `foods` 데이터가 먼저 적재된 뒤 실행되어야 한다. `meal_items.food_code`가 `foods.code`를 참조하기 때문이다.

Docker Compose 실행 시 `data-importer`가 아래 순서로 자동 실행한다.

1. `data/db/schema.sql`
2. `data/db/data.sql`
3. `data/foods/scripts/import-foods.sql`
4. `data/meals/seed-meals.sql`

## 생성 데이터

- `user_id = 1` 기준
- 오늘, 어제, 2일 전 식단 기록
- 아침, 점심, 저녁, 간식 일부 포함
- 총 `meals` 9건
- 총 `meal_items` 19건

## 중복 방지

`meals`는 `(user_id, meal_type, meal_date)` unique 제약으로 중복을 막는다.

`meal_items`는 `(meal_id, food_code)` primary key로 중복을 막는다.
