#!/usr/bin/env bash
set -euo pipefail

BACKEND_CONTAINER="${BACKEND_CONTAINER:-ai-health-backend}"
OUTPUT_DIR="${OUTPUT_DIR:-data/chat-stream-latency/results}"
SINCE="${SINCE:-}"

mkdir -p "$OUTPUT_DIR"
timestamp="$(date +%Y%m%d-%H%M%S)"
stream_csv="$OUTPUT_DIR/chat-stream-timing-$timestamp.csv"
context_csv="$OUTPUT_DIR/chat-context-build-timing-$timestamp.csv"

docker_logs() {
  if [[ -n "$SINCE" ]]; then
    docker logs --since "$SINCE" "$BACKEND_CONTAINER" 2>&1
    return
  fi

  docker logs "$BACKEND_CONTAINER" 2>&1
}

docker_logs | awk '
  /chat_stream_timing/ {
    row = ""
    keys = "user_id user_save_ms context_wait_ms assistant_prompt_ms first_delta_ms assistant_stream_ms assistant_save_ms tool_total_ms tool_join_wait_ms total_ms"
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
    if (!headerPrinted) {
      print "timestamp,user_id,user_save_ms,context_wait_ms,assistant_prompt_ms,first_delta_ms,assistant_stream_ms,assistant_save_ms,tool_total_ms,tool_join_wait_ms,total_ms"
      headerPrinted = 1
    }
    print $1 "," values["user_id"] "," values["user_save_ms"] "," values["context_wait_ms"] "," values["assistant_prompt_ms"] "," values["first_delta_ms"] "," values["assistant_stream_ms"] "," values["assistant_save_ms"] "," values["tool_total_ms"] "," values["tool_join_wait_ms"] "," values["total_ms"]
  }
' > "$stream_csv"

docker_logs | awk '
  /chat_context_build_timing/ {
    keys = "user_id refresh_ms profile_ms daily_goal_ms daily_meals_ms daily_exercises_ms daily_summaries_ms recent_turns_ms active_memories_ms total_ms"
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
    if (!headerPrinted) {
      print "timestamp,user_id,refresh_ms,profile_ms,daily_goal_ms,daily_meals_ms,daily_exercises_ms,daily_summaries_ms,recent_turns_ms,active_memories_ms,total_ms"
      headerPrinted = 1
    }
    print $1 "," values["user_id"] "," values["refresh_ms"] "," values["profile_ms"] "," values["daily_goal_ms"] "," values["daily_meals_ms"] "," values["daily_exercises_ms"] "," values["daily_summaries_ms"] "," values["recent_turns_ms"] "," values["active_memories_ms"] "," values["total_ms"]
  }
' > "$context_csv"

echo "$stream_csv"
echo "$context_csv"
