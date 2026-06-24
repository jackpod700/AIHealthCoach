#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
DATASET="${DATASET:-data/chat-stream-latency/utterances.jsonl}"
OUTPUT_DIR="${OUTPUT_DIR:-data/chat-stream-latency/results}"
EMAIL="${EMAIL:-benchmark+medium@example.com}"
PASSWORD="${PASSWORD:-password}"
ITERATIONS="${ITERATIONS:-${N:-5}}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"
BACKEND_CONTAINER="${BACKEND_CONTAINER:-ai-health-backend}"

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

measure_stream() {
  local id="$1"
  local category="$2"
  local expected_tool="$3"
  local iteration="$4"
  local message="$5"
  local output_file="$6"

  local escaped_message
  escaped_message="$(printf "%s" "$message" | json_escape)"
  local body
  body="{\"content\":\"$escaped_message\"}"

  local response_file
  response_file="$(mktemp)"
  local result
  result="$(
    curl -sS -N -o "$response_file" \
      -w "%{http_code} %{time_total}" \
      -X POST "$BASE_URL/api/chat/messages/stream" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -H "Content-Type: application/json" \
      -H "Accept: text/event-stream" \
      --data "$body"
  )"

  local status="${result%% *}"
  local time_total="${result##* }"
  local actual_tool
  local has_error
  local timing
  has_error="$(grep -q '^event:[[:space:]]*error' "$response_file" && echo true || echo false)"
  actual_tool="$(detect_actual_tool "$response_file")"
  timing="$(last_stream_timing)"

  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
    "$id" "$category" "$expected_tool" "$actual_tool" "$iteration" "$status" "$timing" "$time_total" >> "$output_file"

  if [[ "$status" != "200" ]]; then
    echo "Stream failed id=$id iteration=$iteration status=$status response=$(cat "$response_file")" >&2
    rm -f "$response_file"
    exit 1
  fi

  if [[ "$has_error" == "true" ]]; then
    echo "Stream returned error event id=$id iteration=$iteration response=$(cat "$response_file")" >&2
    rm -f "$response_file"
    exit 1
  fi

  rm -f "$response_file"
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

last_stream_timing() {
  docker logs --tail 300 "$BACKEND_CONTAINER" 2>&1 | awk '
    /chat_stream_timing/ {
      keys = "user_save_ms context_wait_ms assistant_prompt_ms first_delta_ms tool_total_ms"
      split(keys, keyArray, " ")
      for (i in keyArray) {
        values[keyArray[i]] = ""
      }
      for (i = 1; i <= NF; i++) {
        split($i, pair, "=")
        if (pair[1] in values) {
          values[pair[1]] = pair[2]
        }
      }
    }
    END {
      printf "%s\t%s\t%s\t%s\t%s",
        values["user_save_ms"],
        values["context_wait_ms"],
        values["assistant_prompt_ms"],
        values["first_delta_ms"],
        values["tool_total_ms"]
    }
  '
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
output_file="$OUTPUT_DIR/chat-stream-latency-$timestamp.tsv"
printf "id\tcategory\texpected_tool\tactual_tool\titeration\thttp_status\tuser_save_ms\tcontext_wait_ms\tassistant_prompt_ms\tfirst_delta_ms\ttool_total_ms\ttime_total_seconds\n" > "$output_file"

echo "## stream latency run / dataset=$DATASET / iterations=$ITERATIONS"
while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  id="$(printf "%s" "$line" | extract_field id)"
  category="$(printf "%s" "$line" | extract_field category)"
  expected_tool="$(printf "%s" "$line" | extract_field expectedTool)"
  message="$(printf "%s" "$line" | extract_field message)"

  if [[ -z "$id" || -z "$message" ]]; then
    echo "Skipping malformed dataset row: $line" >&2
    continue
  fi

  for iteration in $(seq 1 "$ITERATIONS"); do
    echo "request id=$id category=$category iteration=$iteration"
    measure_stream "$id" "$category" "$expected_tool" "$iteration" "$message" "$output_file"
  done
done < "$DATASET"

echo "## result"
echo "$output_file"
echo
echo "## backend timing logs"
echo "docker logs ai-health-backend --tail 1000 | grep chat_stream_timing"
