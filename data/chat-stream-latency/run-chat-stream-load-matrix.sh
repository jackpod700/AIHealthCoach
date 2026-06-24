#!/usr/bin/env bash
set -euo pipefail

REQUESTS="${REQUESTS:-100}"
CONCURRENCIES="${CONCURRENCIES:-1 3 5 10}"

echo "## chat stream load matrix / requests=$REQUESTS / concurrencies=$CONCURRENCIES"

for concurrency in $CONCURRENCIES; do
  echo
  echo "## concurrency=$concurrency"
  REQUESTS="$REQUESTS" CONCURRENCY="$concurrency" data/chat-stream-latency/run-chat-stream-load.sh
done
