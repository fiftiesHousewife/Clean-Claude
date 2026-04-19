#!/usr/bin/env bash
# Heartbeat for a running :sandbox:reworkCompare (or :reworkClass).
# Prints one line every INTERVAL seconds with elapsed time, log size, the
# current variant, and the most recent text/tool line from the agent. Exits
# when the log reports "=== EXIT" or "BUILD FAILED".
#
# Usage:
#   ./scripts/watch-rework-run.sh [LOG] [INTERVAL_SECONDS]
#
# LOG defaults to /tmp/cleanclaude/current-log (a tiny pointer file that the
# run should write with the absolute path of its actual log).
set -euo pipefail

POINTER="${1:-/tmp/cleanclaude/current-log}"
INTERVAL="${2:-30}"

if [ -f "$POINTER" ] && [ ! -s "$POINTER" ]; then
  echo "pointer file is empty: $POINTER" >&2
  exit 2
fi

if [ -f "$POINTER" ] && head -1 "$POINTER" | grep -q '^/'; then
  LOG=$(cat "$POINTER")
else
  LOG="$POINTER"
fi

if [ ! -f "$LOG" ]; then
  echo "log file not found: $LOG" >&2
  exit 2
fi

START=$(date +%s)
while true; do
  ELAPSED=$(( $(date +%s) - START ))
  MM=$(printf "%d:%02d" $((ELAPSED/60)) $((ELAPSED%60)))
  LINES=$(wc -l < "$LOG" 2>/dev/null | tr -d ' ')
  VARIANT=$(grep -E "^▶ run [0-9]+ of [0-9]+" "$LOG" 2>/dev/null | tail -1 | sed 's/^▶ //')
  LASTACT=$(tail -80 "$LOG" 2>/dev/null | grep -E "^    (text|tool):" | tail -1 | cut -c1-160)
  echo "[$MM] lines=$LINES | ${VARIANT:-init} | ${LASTACT:-(no recent activity line)}"
  if grep -qE "=== EXIT|BUILD FAILED" "$LOG" 2>/dev/null; then
    TAIL=$(grep -E "=== EXIT|BUILD SUCCESSFUL|BUILD FAILED" "$LOG" | tail -3 | tr '\n' ' | ')
    echo "TERMINAL: $TAIL"
    exit 0
  fi
  sleep "$INTERVAL"
done
