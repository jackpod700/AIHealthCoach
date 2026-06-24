#!/usr/bin/env bash
set -euo pipefail

DB_CONTAINER="${DB_CONTAINER:-ai-health-postgres}"
DB_NAME="${DB_NAME:-ai_health_coach}"
DB_USER="${DB_USER:-postgres}"
USER_ID="${USER_ID:-920500}"
CHANGE_RATE="${CHANGE_RATE:-1}"
ITERATIONS="${ITERATIONS:-100}"
SEED="${SEED:-1}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SEED_SQL="$ROOT_DIR/data/db/benchmark/seed-daily-summary-context-cache-benchmark.sql"

psql_exec() {
  docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" "$@"
}

psql_query() {
  local sql="$1"
  docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" -At -c "$sql" >/dev/null
}

summarize() {
  local label="$1"
  local file="$2"

  awk -v label="$label" '
    function percentile(sorted, count, p) {
      idx = int(p * count)
      if (idx < 1) {
        idx = 1
      }
      if (idx > count) {
        idx = count
      }
      return sorted[idx]
    }
    {
      values[NR] = $1
      sum += values[NR]
    }
    END {
      n = NR
      for (i = 1; i <= n; i++) {
        sorted[i] = values[i]
      }
      for (i = 1; i <= n; i++) {
        for (j = i + 1; j <= n; j++) {
          if (sorted[i] > sorted[j]) {
            temp = sorted[i]
            sorted[i] = sorted[j]
            sorted[j] = temp
          }
        }
      }
      printf "%s count=%d min=%.3fms avg=%.3fms p50=%.3fms p95=%.3fms p99=%.3fms max=%.3fms\n",
        label, n, sorted[1], sum / n, percentile(sorted, n, 0.50), percentile(sorted, n, 0.95), percentile(sorted, n, 0.99), sorted[n]
    }
  ' "$file"
}

elapsed_ms_for_query() {
  local sql="$1"
  local start_ns
  local end_ns
  start_ns="$(date +%s%N)"
  psql_query "$sql"
  end_ns="$(date +%s%N)"
  awk -v start="$start_ns" -v end="$end_ns" 'BEGIN { printf "%.3f\n", (end - start) / 1000000 }'
}

elapsed_ms_for_cached_payload_read() {
  local payload="$1"
  local start_ns
  local end_ns
  start_ns="$(date +%s%N)"
  printf "%s" "$payload" >/dev/null
  end_ns="$(date +%s%N)"
  awk -v start="$start_ns" -v end="$end_ns" 'BEGIN { printf "%.3f\n", (end - start) / 1000000 }'
}

db_direct_query="
SELECT summaries.summary_date, summaries.content
FROM daily_chat_summaries summaries
INNER JOIN daily_chat_summary_states states
    ON states.user_id = summaries.user_id
   AND states.summary_date = summaries.summary_date
   AND states.source_version = summaries.source_version
   AND states.status = 'FRESH'
WHERE summaries.user_id = ${USER_ID}
  AND summaries.summary_date >= CURRENT_DATE - INTERVAL '6 days'
  AND summaries.summary_date <= CURRENT_DATE - INTERVAL '1 day'
ORDER BY summaries.summary_date ASC;
"

version_marker_query="
SELECT states.summary_date, states.source_version
FROM daily_chat_summary_states states
INNER JOIN daily_chat_summaries summaries
    ON summaries.user_id = states.user_id
   AND summaries.summary_date = states.summary_date
   AND summaries.source_version = states.source_version
WHERE states.user_id = ${USER_ID}
  AND states.summary_date >= CURRENT_DATE - INTERVAL '6 days'
  AND states.summary_date <= CURRENT_DATE - INTERVAL '1 day'
  AND states.status = 'FRESH'
ORDER BY states.summary_date ASC;
"

miss_rebuild_query="
${version_marker_query}
${db_direct_query}
"

raw_source_full_lookup_query="
SELECT id, role, content, created_at
FROM chat_messages
WHERE user_id = ${USER_ID}
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
WHERE meals.user_id = ${USER_ID}
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
WHERE user_id = ${USER_ID}
  AND exercise_date >= CURRENT_DATE - INTERVAL '6 days'
  AND exercise_date < CURRENT_DATE
ORDER BY exercise_date ASC, id ASC;

SELECT id,
       record_date,
       weight_kg
FROM weight_records
WHERE user_id = ${USER_ID}
  AND record_date >= CURRENT_DATE - INTERVAL '6 days'
  AND record_date < CURRENT_DATE
ORDER BY record_date ASC;

SELECT id,
       goal_type,
       calorie_intake_goal,
       exercise_calorie_goal,
       updated_at
FROM daily_goals
WHERE user_id = ${USER_ID};
"

change_distribution_query="
SELECT status || '=' || COUNT(*)
FROM daily_chat_summary_states
WHERE user_id BETWEEN 920001 AND 921000
  AND summary_date >= CURRENT_DATE - INTERVAL '6 days'
  AND summary_date <= CURRENT_DATE - INTERVAL '1 day'
GROUP BY status
ORDER BY status;
"

if [[ "$SEED" == "1" ]]; then
  echo "## seed daily summary context benchmark data / change_rate=${CHANGE_RATE}%"
  psql_exec -v CHANGE_RATE="$CHANGE_RATE" < "$SEED_SQL" >/dev/null
fi

echo "## benchmark settings / user_id=$USER_ID change_rate=${CHANGE_RATE}% iterations=$ITERATIONS"

db_times="$(mktemp)"
marker_times="$(mktemp)"
miss_times="$(mktemp)"
raw_source_times="$(mktemp)"
cache_hit_lower_bound_times="$(mktemp)"
trap 'rm -f "$db_times" "$marker_times" "$miss_times" "$raw_source_times" "$cache_hit_lower_bound_times"' EXIT

cached_payload="$(docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" -At -c "$db_direct_query")"

for i in $(seq 1 "$ITERATIONS"); do
  elapsed_ms_for_query "$db_direct_query" >> "$db_times"
  elapsed_ms_for_query "$version_marker_query" >> "$marker_times"
  elapsed_ms_for_query "$miss_rebuild_query" >> "$miss_times"
  elapsed_ms_for_query "$raw_source_full_lookup_query" >> "$raw_source_times"
  elapsed_ms_for_cached_payload_read "$cached_payload" >> "$cache_hit_lower_bound_times"
done

summarize "DB direct fresh summary lookup" "$db_times"
summarize "Version marker lookup" "$marker_times"
summarize "Cache miss rebuild candidate" "$miss_times"
summarize "Raw source full lookup" "$raw_source_times"
summarize "Cache hit payload read lower-bound" "$cache_hit_lower_bound_times"

change_distribution="$(docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" -At -c "$change_distribution_query")"
echo "Change distribution in recent completed 6-day window:"
echo "$change_distribution"
