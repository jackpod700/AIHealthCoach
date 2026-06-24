#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
DATASET="${DATASET:-data/chat-stream-latency/utterances.jsonl}"
OUTPUT_DIR="${OUTPUT_DIR:-data/chat-stream-latency/results}"
EMAIL="${EMAIL:-benchmark+medium@example.com}"
PASSWORD="${PASSWORD:-password}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"
TOTAL_REQUESTS="${TOTAL_REQUESTS:-${REQUESTS:-50}}"
CONCURRENCY="${CONCURRENCY:-5}"

extract_json_string() {
  local key="$1"
  sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p"
}

json_escape() {
  sed \
    -e 's/\\/\\\\/g' \
    -e 's/"/\\"/g' \
    -e 's/	/\\t/g'
}

extract_field() {
  local key="$1"
  sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p"
}

login() {
  local login_response_file
  login_response_file="$(mktemp)"
  local login_status
  login_status="$(
    curl -sS -o "$login_response_file" \
      -w "%{http_code}" \
      -X POST "$BASE_URL/api/user/login" \
      -H "Content-Type: application/json" \
      --data "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
  )"

  if [[ "$login_status" != "200" ]]; then
    echo "Login failed status=$login_status response=$(cat "$login_response_file")" >&2
    rm -f "$login_response_file"
    exit 1
  fi

  ACCESS_TOKEN="$(extract_json_string accessToken < "$login_response_file")"
  rm -f "$login_response_file"

  if [[ -z "$ACCESS_TOKEN" ]]; then
    echo "Failed to extract accessToken from login response" >&2
    exit 1
  fi
}

detect_actual_tool() {
  local response_file="$1"
  local tool_json
  tool_json="$(
    awk '
      /^event:/ {
        event = $0
        sub(/^event:[[:space:]]*/, "", event)
      }
      /^data:/ && event == "tool_result" {
        data = $0
        sub(/^data:[[:space:]]*/, "", data)
        print data
        exit
      }
    ' "$response_file"
  )"

  if [[ -z "$tool_json" ]]; then
    echo "none"
    return
  fi

  local tools=()
  if printf "%s" "$tool_json" | grep -q '"mealProposal"[[:space:]]*:[[:space:]]*{'; then
    tools+=("meal")
  fi
  if printf "%s" "$tool_json" | grep -q '"exerciseProposal"[[:space:]]*:[[:space:]]*{'; then
    tools+=("exercise")
  fi
  if printf "%s" "$tool_json" | grep -q '"weightProposal"[[:space:]]*:[[:space:]]*{'; then
    tools+=("weight")
  fi
  if printf "%s" "$tool_json" | grep -q '"memorySave"[[:space:]]*:[[:space:]]*{[^}]*"status"[[:space:]]*:[[:space:]]*"\(SAVED\|FAILED\)"'; then
    tools+=("memory")
  fi

  if [[ "${#tools[@]}" -eq 0 ]]; then
    echo "none"
    return
  fi

  local joined
  joined="$(IFS=+; echo "${tools[*]}")"
  echo "$joined"
}

sample_prometheus() {
  local output_file="$1"
  curl -sS "$BASE_URL/actuator/prometheus" \
    | grep -E '^(hikaricp_connections|http_server_requests|jvm_threads|executor_)' \
    > "$output_file" || true
}

measure_one() {
  local sequence="$1"
  local id="$2"
  local category="$3"
  local expected_tool="$4"
  local message="$5"
  local row_file="$6"

  local escaped_message
  escaped_message="$(printf "%s" "$message" | json_escape)"
  local body
  body="{\"content\":\"$escaped_message\"}"

  local response_file
  response_file="$(mktemp)"
  local result
  result="$(
    curl -sS -N -o "$response_file" \
      -w "%{http_code} %{time_starttransfer} %{time_total}" \
      -X POST "$BASE_URL/api/chat/messages/stream" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -H "Content-Type: application/json" \
      -H "Accept: text/event-stream" \
      --data "$body" || true
  )"

  local http_status
  local time_starttransfer
  local time_total
  http_status="$(printf "%s" "$result" | awk '{print $1}')"
  time_starttransfer="$(printf "%s" "$result" | awk '{print $2}')"
  time_total="$(printf "%s" "$result" | awk '{print $3}')"

  local actual_tool
  local has_error
  actual_tool="$(detect_actual_tool "$response_file")"
  has_error="$(grep -q '^event:[[:space:]]*error' "$response_file" && echo true || echo false)"

  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
    "$sequence" "$id" "$category" "$expected_tool" "$actual_tool" "$http_status" "$has_error" \
    "$time_starttransfer" "$time_total" > "$row_file"

  rm -f "$response_file"
}

percentile_index() {
  local count="$1"
  local percentile="$2"
  awk -v n="$count" -v p="$percentile" 'BEGIN {
    idx = int((n * p + 99) / 100)
    if (idx < 1) idx = 1
    if (idx > n) idx = n
    print idx
  }'
}

summarize_tsv() {
  local result_file="$1"
  awk -F '\t' '
    NR == 1 { next }
    {
      n++
      status[$6]++
      if ($7 == "true") error_events++
      start[n] = $8 + 0
      total[n] = $9 + 0
    }
    END {
      if (n == 0) {
        print "no rows"
        exit
      }
      for (code in status) {
        printf "status_%s=%d\n", code, status[code]
      }
      printf "error_events=%d\n", error_events + 0
      printf "rows=%d\n", n
    }
  ' "$result_file"

  for metric in time_starttransfer_seconds time_total_seconds; do
    local column
    if [[ "$metric" == "time_starttransfer_seconds" ]]; then
      column=8
    else
      column=9
    fi

    local sorted_file
    sorted_file="$(mktemp)"
    awk -F '\t' -v column="$column" 'NR > 1 { print $column + 0 }' "$result_file" | sort -n > "$sorted_file"

    local count
    count="$(wc -l < "$sorted_file" | tr -d ' ')"
    local p50_idx p90_idx p95_idx p99_idx
    p50_idx="$(percentile_index "$count" 50)"
    p90_idx="$(percentile_index "$count" 90)"
    p95_idx="$(percentile_index "$count" 95)"
    p99_idx="$(percentile_index "$count" 99)"

    awk -v metric="$metric" \
      -v p50="$p50_idx" \
      -v p90="$p90_idx" \
      -v p95="$p95_idx" \
      -v p99="$p99_idx" '
        NR == 1 { min = $1 }
        { values[NR] = $1; max = $1; sum += $1 }
        END {
          printf "%s_min=%.6f\n", metric, min
          printf "%s_avg=%.6f\n", metric, sum / NR
          printf "%s_p50=%.6f\n", metric, values[p50]
          printf "%s_p90=%.6f\n", metric, values[p90]
          printf "%s_p95=%.6f\n", metric, values[p95]
          printf "%s_p99=%.6f\n", metric, values[p99]
          printf "%s_max=%.6f\n", metric, max
        }
      ' "$sorted_file"
    rm -f "$sorted_file"
  done
}

if [[ ! -f "$DATASET" ]]; then
  echo "Dataset not found: $DATASET" >&2
  exit 1
fi

echo "## precheck / backend health"
curl -sS --retry 30 --retry-delay 1 --retry-all-errors -f "$BASE_URL/api/health" >/dev/null

if [[ -z "$ACCESS_TOKEN" ]]; then
  echo "## login / $EMAIL"
  login
else
  echo "## using ACCESS_TOKEN from environment"
fi

mkdir -p "$OUTPUT_DIR"
timestamp="$(date +%Y%m%d-%H%M%S)"
result_file="$OUTPUT_DIR/chat-stream-load-$timestamp.tsv"
summary_file="$OUTPUT_DIR/chat-stream-load-$timestamp.summary"
before_metrics_file="$OUTPUT_DIR/chat-stream-load-$timestamp.prometheus.before"
after_metrics_file="$OUTPUT_DIR/chat-stream-load-$timestamp.prometheus.after"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

mapfile -t dataset_lines < "$DATASET"
if [[ "${#dataset_lines[@]}" -eq 0 ]]; then
  echo "Dataset is empty: $DATASET" >&2
  exit 1
fi

printf "sequence\tid\tcategory\texpected_tool\tactual_tool\thttp_status\thas_error_event\ttime_starttransfer_seconds\ttime_total_seconds\n" > "$result_file"

echo "## load run / dataset=$DATASET / requests=$TOTAL_REQUESTS / concurrency=$CONCURRENCY"
sample_prometheus "$before_metrics_file"

running=0
for sequence in $(seq 1 "$TOTAL_REQUESTS"); do
  line="${dataset_lines[$(((sequence - 1) % ${#dataset_lines[@]}))]}"
  id="$(printf "%s" "$line" | extract_field id)"
  category="$(printf "%s" "$line" | extract_field category)"
  expected_tool="$(printf "%s" "$line" | extract_field expectedTool)"
  message="$(printf "%s" "$line" | extract_field message)"
  row_file="$tmp_dir/row-$sequence.tsv"

  measure_one "$sequence" "$id" "$category" "$expected_tool" "$message" "$row_file" &
  running=$((running + 1))

  if [[ "$running" -ge "$CONCURRENCY" ]]; then
    wait -n
    running=$((running - 1))
  fi
done

wait
sample_prometheus "$after_metrics_file"

for sequence in $(seq 1 "$TOTAL_REQUESTS"); do
  cat "$tmp_dir/row-$sequence.tsv" >> "$result_file"
done

{
  echo "requests=$TOTAL_REQUESTS"
  echo "concurrency=$CONCURRENCY"
  echo "dataset=$DATASET"
  summarize_tsv "$result_file"
  echo "prometheus_before=$before_metrics_file"
  echo "prometheus_after=$after_metrics_file"
} > "$summary_file"

echo "## result"
echo "$result_file"
echo "$summary_file"
echo "$before_metrics_file"
echo "$after_metrics_file"
echo
cat "$summary_file"
