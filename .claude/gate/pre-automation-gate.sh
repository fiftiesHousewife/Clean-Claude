#!/usr/bin/env bash
# Usage: ./pre-automation-gate.sh --task <label> --budget <tokens> [--min-runs <n>]

set -euo pipefail

TASK=""
BUDGET=0
MIN_RUNS=5
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="${SCRIPT_DIR}/../session-log.jsonl"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task)     TASK="$2";     shift 2 ;;
    --budget)   BUDGET="$2";   shift 2 ;;
    --min-runs) MIN_RUNS="$2"; shift 2 ;;
    --log)      LOG_FILE="$2"; shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

[[ -z "$TASK" || "$BUDGET" -eq 0 ]] && {
  echo "Usage: $0 --task <label> --budget <tokens> [--min-runs <n>]" >&2; exit 2
}

[[ ! -f "$LOG_FILE" ]] && {
  echo "GATE BLOCKED [$TASK]: No session log at $LOG_FILE" >&2
  echo "Run the task manually at least $MIN_RUNS times first." >&2
  exit 1
}

STATS=$(jq -s \
  --arg task "$TASK" \
  --argjson min_runs "$MIN_RUNS" '
  map(select(.failed == false))
  | if ($task != "all") then map(select(.task == $task)) else . end
  | if length < $min_runs then
      {insufficient: true, runs: length, required: $min_runs}
    else
      sort_by(.total)
      | {
          insufficient: false,
          runs:        length,
          avg:         (map(.total) | add / length | floor),
          p95:         .[(length * 0.95 | floor)].total,
          max:         (map(.total) | max),
          avg_turns:   (map(.turns) | add / length | floor),
          cache_ratio: (
            (map(.cache_read) | add) /
            (map(.input) | add) * 100 | floor
          )
        }
    end
' "$LOG_FILE")

if [[ $(echo "$STATS" | jq -r '.insufficient') == "true" ]]; then
  RUNS=$(echo "$STATS" | jq -r '.runs')
  REQ=$(echo "$STATS"  | jq -r '.required')
  echo "GATE BLOCKED [$TASK]: $RUNS runs logged, need $REQ." >&2
  exit 1
fi

RUNS=$(echo "$STATS"        | jq -r '.runs')
AVG=$(echo "$STATS"         | jq -r '.avg')
P95=$(echo "$STATS"         | jq -r '.p95')
MAX=$(echo "$STATS"         | jq -r '.max')
AVG_TURNS=$(echo "$STATS"   | jq -r '.avg_turns')
CACHE_RATIO=$(echo "$STATS" | jq -r '.cache_ratio')

echo "──────────────────────────────────────────"
echo " Token gate: $TASK"
echo "──────────────────────────────────────────"
printf " %-14s %s\n"   "Runs:"         "$RUNS"
printf " %-14s %s\n"   "Avg:"          "$AVG"
printf " %-14s %s\n"   "p95:"          "$P95"
printf " %-14s %s\n"   "Max:"          "$MAX"
printf " %-14s %s\n"   "Avg turns:"    "$AVG_TURNS"
printf " %-14s %s%%\n" "Cache ratio:"  "$CACHE_RATIO"
printf " %-14s %s\n"   "Budget:"       "$BUDGET"
echo "──────────────────────────────────────────"

(( P95 > BUDGET )) && {
  echo "GATE BLOCKED: p95 ($P95) exceeds budget ($BUDGET)" >&2
  exit 1
}

echo "GATE PASSED"
exit 0
