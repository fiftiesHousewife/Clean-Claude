#!/usr/bin/env bash
# Stop / StopFailure hook — records session totals.

set -euo pipefail

FAILED=false
[[ "${1:-}" == "--failed" ]] && FAILED=true

INPUT=$(cat)

TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

SESSION_ID=$(echo "$INPUT"    | jq -r '.session_id  // "unknown"')
TURNS=$(echo "$INPUT"         | jq -r '.num_turns   // .turns // 0')
MODEL=$(echo "$INPUT"         | jq -r '.model       // "unknown"')

# Token counts are NOT in hook payloads (confirmed by debug dump).
# Track transcript size as the best available proxy.
TRANSCRIPT=$(echo "$INPUT" | jq -r '.transcript_path // ""')
TRANSCRIPT_LINES=0
TRANSCRIPT_BYTES=0
if [ -n "$TRANSCRIPT" ] && [ -f "$TRANSCRIPT" ]; then
  TRANSCRIPT_LINES=$(wc -l < "$TRANSCRIPT" 2>/dev/null || echo 0)
  TRANSCRIPT_BYTES=$(wc -c < "$TRANSCRIPT" 2>/dev/null || echo 0)
fi

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
  --argjson turns   "$TURNS"         \
  --argjson tl      "$TRANSCRIPT_LINES" \
  --argjson tb      "$TRANSCRIPT_BYTES" \
  --argjson failed  "$FAILED"        \
  '{ts:$ts, session:$session, task:$task, model:$model,
    turns:$turns, transcript_lines:$tl, transcript_bytes:$tb,
    failed:$failed}' \
  >> "$LOG_FILE"

exit 0
