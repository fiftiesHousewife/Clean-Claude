#!/usr/bin/env bash
# PostToolUse hook — records every tool call in full detail.

set -euo pipefail

INPUT=$(cat)

TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

SESSION_ID=$(echo "$INPUT"     | jq -r '.session_id             // "unknown"')
TURN=$(echo "$INPUT"           | jq -r '.turn_number            // 0')
TOOL=$(echo "$INPUT"           | jq -r '.tool_name              // "unknown"')
INPUT_TOKENS=$(echo "$INPUT"   | jq -r '.usage.input_tokens     // 0')
OUTPUT_TOKENS=$(echo "$INPUT"  | jq -r '.usage.output_tokens    // 0')
EXIT_CODE=$(echo "$INPUT"      | jq -r '.tool_response.exit_code // null')
TOTAL=$(( INPUT_TOKENS + OUTPUT_TOKENS ))

TOOL_INPUT=$(echo "$INPUT" | jq -c '.tool_input // {}')

case "$TOOL" in
  Bash)
    DETAIL=$(echo "$TOOL_INPUT" | jq -r '.command   // ""') ;;
  Write)
    DETAIL=$(echo "$TOOL_INPUT" | jq -r '.file_path // ""') ;;
  Edit|MultiEdit)
    DETAIL=$(echo "$TOOL_INPUT" | jq -r '.file_path // ""') ;;
  Read)
    DETAIL=$(echo "$TOOL_INPUT" | jq -r '.file_path // ""') ;;
  Glob|Grep)
    DETAIL=$(echo "$TOOL_INPUT" | jq -r '.pattern   // ""') ;;
  Task)
    DETAIL=$(echo "$TOOL_INPUT" | jq -r '.description // ""') ;;
  *)
    DETAIL=$(echo "$TOOL_INPUT" | jq -r 'to_entries | map("\(.key)=\(.value)") | join(" ")') ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
LOG_FILE="${PROJECT_ROOT}/.claude/tool-log.jsonl"
mkdir -p "$(dirname "$LOG_FILE")"

TASK_LABEL="${CLAUDE_TASK_LABEL:-unknown}"

jq -nc \
  --arg  ts            "$TIMESTAMP"     \
  --arg  session       "$SESSION_ID"    \
  --arg  task          "$TASK_LABEL"    \
  --argjson turn       "$TURN"          \
  --arg  tool          "$TOOL"          \
  --arg  detail        "$DETAIL"        \
  --argjson input_c    "$INPUT_TOKENS"  \
  --argjson output_c   "$OUTPUT_TOKENS" \
  --argjson total_c    "$TOTAL"         \
  --argjson exit       "${EXIT_CODE:-null}" \
  --argjson tool_input "$TOOL_INPUT"    \
  '{
    ts:                $ts,
    session:           $session,
    task:              $task,
    turn:              $turn,
    tool:              $tool,
    detail:            $detail,
    input_cumulative:  $input_c,
    output_cumulative: $output_c,
    total_cumulative:  $total_c,
    exit_code:         $exit,
    tool_input:        $tool_input
  }' >> "$LOG_FILE"

exit 0
