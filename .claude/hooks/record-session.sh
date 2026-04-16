#!/usr/bin/env bash
# Stop / StopFailure hook — records session totals.

set -euo pipefail

FAILED=false
[[ "${1:-}" == "--failed" ]] && FAILED=true

INPUT=$(cat)

TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

SESSION_ID=$(echo "$INPUT"    | jq -r '.session_id                      // "unknown"')
INPUT_TOKENS=$(echo "$INPUT"  | jq -r '.usage.input_tokens              // 0')
OUTPUT_TOKENS=$(echo "$INPUT" | jq -r '.usage.output_tokens             // 0')
CACHE_READ=$(echo "$INPUT"    | jq -r '.usage.cache_read_input_tokens   // 0')
CACHE_WRITE=$(echo "$INPUT"   | jq -r '.usage.cache_write_input_tokens  // 0')
TURNS=$(echo "$INPUT"         | jq -r '.turns                           // 0')
MODEL=$(echo "$INPUT"         | jq -r '.model                           // "unknown"')
TOTAL=$(( INPUT_TOKENS + OUTPUT_TOKENS ))

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
LOG_FILE="${PROJECT_ROOT}/.claude/session-log.jsonl"
mkdir -p "$(dirname "$LOG_FILE")"

TASK_LABEL="${CLAUDE_TASK_LABEL:-unknown}"

jq -nc \
  --arg  ts         "$TIMESTAMP"     \
  --arg  session    "$SESSION_ID"    \
  --arg  task       "$TASK_LABEL"    \
  --arg  model      "$MODEL"         \
  --argjson input   "$INPUT_TOKENS"  \
  --argjson output  "$OUTPUT_TOKENS" \
  --argjson cache_r "$CACHE_READ"    \
  --argjson cache_w "$CACHE_WRITE"   \
  --argjson total   "$TOTAL"         \
  --argjson turns   "$TURNS"         \
  --argjson failed  "$FAILED"        \
  '{ts:$ts, session:$session, task:$task, model:$model,
    input:$input, output:$output,
    cache_read:$cache_r, cache_write:$cache_w,
    total:$total, turns:$turns, failed:$failed}' \
  >> "$LOG_FILE"

exit 0
