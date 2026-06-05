# 식단 기록 캘린더 뷰 구현 계획

## Summary
- 사용자가 자신의 식단 기록을 월간 캘린더로 볼 수 있게 한다.
- 백엔드는 월별 식단 요약 API를 새로 추가한다.
- 프론트는 기존 `chat/profile` 뷰에 `calendar` 뷰를 추가한다.
- 날짜를 클릭하면 기존 `GET /api/meals/daily`를 재사용해 하루 전체 식단 상세를 보여준다.
- 신규 테이블은 만들지 않고 기존 `meals`, `meal_items`, `foods`만 사용한다.

## Key Changes
- 새 API:
  ```http
  GET /api/meals/monthly?year=2026&month=6
  Authorization: Bearer {token}
  ```
- 응답 형태:
  ```json
  {
    "year": 2026,
    "month": 6,
    "days": [
      {
        "date": "2026-06-02",
        "mealCount": 2,
        "mealTypes": ["BREAKFAST", "DINNER"],
        "totalCalories": 820,
        "totalCarbohydrate": 90,
        "totalProtein": 45,
        "totalFat": 28
      }
    ]
  }
  ```
- 날짜 상세는 기존 API 유지:
  ```http
  GET /api/meals/daily?date=2026-06-02
  ```

## Implementation Changes
- 백엔드:
  - `MealDto`에 `MonthlyMealResponse`, `MonthlyMealDayResponse` 추가.
  - `MealController`에 `GET /api/meals/monthly` 추가.
  - `MealService`/`MealServiceImpl`에 `findMonthlyMeals(userId, year, month)` 추가.
  - `MealMapper`/XML에 월 범위 조회 추가.
  - 월 조회는 `LocalDate.of(year, month, 1)`부터 다음 달 1일 전까지 조회한다.
  - 영양소 합산은 현재 daily 조회와 동일하게 `foods` 영양소 값에 `meal_items.quantity`를 곱한다.
  - `month`는 `1~12`만 허용한다.

- 프론트:
  - 상단 네비게이션에 `캘린더` 버튼 추가.
  - `healthStore`에 월별 식단 상태 추가:
    - `mealCalendar`
    - `selectedCalendarMonth`
    - `selectedMealDate`
    - `selectedDailyMeal`
    - loading/error 상태
  - `loadMonthlyMeals(year, month)`와 `loadDailyMeal(date)` 액션 추가.
  - 캘린더 셀에는 날짜, 총 kcal, 기록된 끼니 타입 배지를 표시한다.
  - 날짜 클릭 시 오른쪽/하단 상세 패널에 끼니별 음식, 수량, 칼로리, 탄단지 합계를 표시한다.
  - AI 식단 확정 저장 후 현재 보고 있는 월/날짜에 해당하면 캘린더와 상세를 새로고침한다.

## Test Plan
- 백엔드 성공 케이스:
  - 월별 식단 요약 조회 성공.
  - 여러 끼니가 같은 날짜에 있을 때 날짜 단위로 합산됨.
  - `quantity` 배수가 영양소 합산에 반영됨.
  - 기록이 없는 월은 `days: []`를 반환함.

- 백엔드 실패 케이스:
  - `month < 1` 또는 `month > 12` 요청 실패.
  - 비인증 요청 실패.

- 프론트 검증:
  - 캘린더 월 이동 버튼이 월별 API를 다시 호출함.
  - 날짜 클릭 시 daily API로 상세가 표시됨.
  - 기록 없는 날짜 클릭 시 빈 상태가 표시됨.
  - 식단 확정 저장 후 캘린더 요약이 갱신됨.

- 검증 명령:
  ```powershell
  docker run --rm -v ${PWD}\backend:/app -v ${env:USERPROFILE}\.m2:/root/.m2 -w /app maven:3.9.11-eclipse-temurin-21 mvn test
  npm.cmd run build
  ```

## Assumptions
- 캘린더 1차 범위에는 식단 수정/삭제 기능을 포함하지 않는다.
- 월별 API는 상세 음식 목록을 내려주지 않고 날짜별 요약만 내려준다.
- 날짜 상세는 기존 `DailyMealResponse`를 그대로 사용한다.
- 캘린더는 로그인한 사용자 본인의 식단만 조회한다.
- 기록된 날짜만 월별 응답 `days`에 포함하고, 빈 날짜 셀은 프론트에서 채운다.
