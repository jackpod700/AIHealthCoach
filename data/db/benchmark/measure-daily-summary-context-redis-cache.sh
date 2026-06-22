#!/usr/bin/env bash
set -euo pipefail

DB_CONTAINER="${DB_CONTAINER:-ai-health-postgres}"
REDIS_CONTAINER="${REDIS_CONTAINER:-ai-health-redis}"
DB_NAME="${DB_NAME:-ai_health_coach}"
DB_USER="${DB_USER:-postgres}"
USER_ID="${USER_ID:-920500}"
CHANGE_RATE="${CHANGE_RATE:-1}"
ITERATIONS="${ITERATIONS:-100}"
SEED="${SEED:-1}"
REDIS_TTL_SECONDS="${REDIS_TTL_SECONDS:-300}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SEED_SQL="$ROOT_DIR/data/db/benchmark/seed-daily-summary-context-cache-benchmark.sql"

FROM_DATE="$(date -d "6 days ago" +%F)"
TO_DATE="$(date -d "1 day ago" +%F)"
REDIS_KEY="ai:chat:summary-context:${USER_ID}:${FROM_DATE}:${TO_DATE}"
USER_KEYS_KEY="ai:chat:summary-context-keys:${USER_ID}"

psql_exec() {
  docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" "$@"
}

psql_query() {
  local sql="$1"
  docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" -At -c "$sql" >/dev/null
}

redis_query() {
  docker exec -i "$REDIS_CONTAINER" redis-cli "$@" >/dev/null
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

elapsed_ms_for_command() {
  local start_ns
  local end_ns
  start_ns="$(date +%s%N)"
  "$@" >/dev/null
  end_ns="$(date +%s%N)"
  awk -v start="$start_ns" -v end="$end_ns" 'BEGIN { printf "%.3f\n", (end - start) / 1000000 }'
}

write_container_file() {
  local container="$1"
  local path="$2"
  docker exec -i "$container" sh -c "cat > '$path'"
}

measure_psql_inside_container() {
  local sql_file="$1"
  local output_file="$2"
docker exec -i "$DB_CONTAINER" sh -s "$DB_USER" "$DB_NAME" "$ITERATIONS" "$sql_file" > "$output_file" <<'EOS'
set -eu
db_user="$1"
db_name="$2"
iterations="$3"
sql_file="$4"
now_ms() {
  awk '{printf "%.3f", $1 * 1000}' /proc/uptime
}
for i in $(seq 1 "$iterations"); do
  start_ms="$(now_ms)"
  psql -v ON_ERROR_STOP=1 -U "$db_user" -d "$db_name" -At -f "$sql_file" >/dev/null
  end_ms="$(now_ms)"
  awk -v start="$start_ms" -v end="$end_ms" 'BEGIN { printf "%.3f\n", end - start }'
done
EOS
}

measure_redis_get_inside_container() {
  local output_file="$1"
docker exec -i "$REDIS_CONTAINER" sh -s "$ITERATIONS" "$REDIS_KEY" > "$output_file" <<'EOS'
set -eu
iterations="$1"
key="$2"
now_ms() {
  awk '{printf "%.3f", $1 * 1000}' /proc/uptime
}
for i in $(seq 1 "$iterations"); do
  start_ms="$(now_ms)"
  redis-cli GET "$key" >/dev/null
  end_ms="$(now_ms)"
  awk -v start="$start_ms" -v end="$end_ms" 'BEGIN { printf "%.3f\n", end - start }'
done
EOS
}

measure_redis_set_inside_container() {
  local output_file="$1"
  docker exec -i "$REDIS_CONTAINER" sh -s "$ITERATIONS" "$REDIS_KEY" "$REDIS_TTL_SECONDS" > "$output_file" <<'EOS'
set -eu
iterations="$1"
key="$2"
ttl="$3"
payload="$(cat /tmp/daily-summary-context-payload.json)"
now_ms() {
  awk '{printf "%.3f", $1 * 1000}' /proc/uptime
}
for i in $(seq 1 "$iterations"); do
  start_ms="$(now_ms)"
  redis-cli SET "$key" "$payload" EX "$ttl" >/dev/null
  end_ms="$(now_ms)"
  awk -v start="$start_ms" -v end="$end_ms" 'BEGIN { printf "%.3f\n", end - start }'
done
EOS
}

measure_redis_evict_inside_container() {
  local output_file="$1"
  docker exec -i "$REDIS_CONTAINER" sh -s "$ITERATIONS" "$REDIS_KEY" "$USER_KEYS_KEY" "$REDIS_TTL_SECONDS" > "$output_file" <<'EOS'
set -eu
iterations="$1"
key="$2"
user_keys_key="$3"
ttl="$4"
payload="$(cat /tmp/daily-summary-context-payload.json)"
now_ms() {
  awk '{printf "%.3f", $1 * 1000}' /proc/uptime
}
for i in $(seq 1 "$iterations"); do
  redis-cli SET "$key" "$payload" EX "$ttl" >/dev/null
  redis-cli SADD "$user_keys_key" "$key" >/dev/null
  start_ms="$(now_ms)"
  redis-cli DEL "$key" "$user_keys_key" >/dev/null
  end_ms="$(now_ms)"
  awk -v start="$start_ms" -v end="$end_ms" 'BEGIN { printf "%.3f\n", end - start }'
done
EOS
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

redis_payload_query="
SELECT json_build_object(
  'summaries',
  COALESCE(
    json_agg(
      json_build_object(
        'summaryDate', summaries.summary_date,
        'content', summaries.content
      )
      ORDER BY summaries.summary_date ASC
    ),
    '[]'::json
  ),
  'sourceVersions',
  '{}'::json
)::text
FROM daily_chat_summaries summaries
INNER JOIN daily_chat_summary_states states
    ON states.user_id = summaries.user_id
   AND states.summary_date = summaries.summary_date
   AND states.source_version = summaries.source_version
   AND states.status = 'FRESH'
WHERE summaries.user_id = ${USER_ID}
  AND summaries.summary_date >= CURRENT_DATE - INTERVAL '6 days'
  AND summaries.summary_date <= CURRENT_DATE - INTERVAL '1 day';
"

if [[ "$SEED" == "1" ]]; then
  echo "## seed daily summary context benchmark data / change_rate=${CHANGE_RATE}%"
  psql_exec -v CHANGE_RATE="$CHANGE_RATE" < "$SEED_SQL" >/dev/null
fi

payload="$(docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" -At -c "$redis_payload_query")"
printf "%s" "$db_direct_query" | write_container_file "$DB_CONTAINER" /tmp/daily-summary-context-db-direct.sql
printf "%s" "$redis_payload_query" | write_container_file "$DB_CONTAINER" /tmp/daily-summary-context-redis-payload.sql
printf "%s" "$payload" | write_container_file "$REDIS_CONTAINER" /tmp/daily-summary-context-payload.json
redis_query SET "$REDIS_KEY" "$payload" EX "$REDIS_TTL_SECONDS"
redis_query SADD "$USER_KEYS_KEY" "$REDIS_KEY"
redis_query EXPIRE "$USER_KEYS_KEY" "$((REDIS_TTL_SECONDS + 86400))"

echo "## benchmark settings / user_id=$USER_ID change_rate=${CHANGE_RATE}% iterations=$ITERATIONS"
echo "## redis key / $REDIS_KEY"
echo "## measurement mode / loops run inside target containers to exclude per-iteration docker exec overhead"
echo "## note / psql and redis-cli process overhead still remains; timer source is /proc/uptime"

db_times="$(mktemp)"
db_json_payload_times="$(mktemp)"
redis_hit_times="$(mktemp)"
redis_set_times="$(mktemp)"
redis_evict_times="$(mktemp)"
trap 'rm -f "$db_times" "$db_json_payload_times" "$redis_hit_times" "$redis_set_times" "$redis_evict_times"' EXIT

measure_psql_inside_container /tmp/daily-summary-context-db-direct.sql "$db_times"
measure_psql_inside_container /tmp/daily-summary-context-redis-payload.sql "$db_json_payload_times"
measure_redis_get_inside_container "$redis_hit_times"
measure_redis_set_inside_container "$redis_set_times"
measure_redis_evict_inside_container "$redis_evict_times"

summarize "DB direct fresh summary lookup" "$db_times"
summarize "DB JSON payload rebuild for Redis miss" "$db_json_payload_times"
summarize "Redis cache hit GET" "$redis_hit_times"
summarize "Redis cache SET payload" "$redis_set_times"
summarize "Redis evict DEL key + user key set" "$redis_evict_times"
