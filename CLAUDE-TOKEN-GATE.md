# Token Usage Hook & Pre-Automation Gate

## Purpose

Record every Claude Code tool execution in full detail, plus session-level summaries.
Use the tool-level log to identify automation candidates and understand cost drivers.
Use the session-level log to gate automated task runs against a token budget.

Designed for a Java 21 / Gradle plugin project. All hook scripts are plain Bash.

---

## Two-Level Logging Strategy

| Level | Hook | Record | Use |
|---|---|---|---|
| Tool | `PostToolUse` | One record per tool call, full input captured | Automation discovery, pattern analysis |
| Session | `Stop` / `StopFailure` | One record per completed session | Gate budget checks, cost trending |

**Important: token counts in `PostToolUse` are cumulative**, not marginal.
They reflect the total context window size at the point that tool was called —
not the cost of that tool call alone. A `sed` at turn 3 and the same `sed` at
turn 12 will show very different token counts because the conversation history
has grown. Use them to understand *where* context bloats, not to sum individual
operation costs.

---

## Repository Layout

```
<project-root>/
├── .claude/
│   ├── settings.json                   # Hook wiring (committed)
│   ├── settings.local.json             # Personal overrides — gitignored
│   ├── hooks/
│   │   ├── record-tool-use.sh          # PostToolUse hook → tool-log.jsonl
│   │   └── record-session.sh           # Stop/StopFailure hook → session-log.jsonl
│   ├── gate/
│   │   └── pre-automation-gate.sh      # Gate — reads session-log.jsonl
│   ├── tool-log.jsonl                  # Per-tool-call log — gitignored
│   └── session-log.jsonl              # Per-session log — gitignored
└── build.gradle.kts
```

Add to `.gitignore`:
```
.claude/tool-log.jsonl
.claude/session-log.jsonl
.claude/settings.local.json
```

---

## 1. Hook Configuration

### `.claude/settings.json`

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "${PROJECT_ROOT}/.claude/hooks/record-tool-use.sh",
            "timeout": 10,
            "async": true
          }
        ]
      }
    ],
    "Stop": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "${PROJECT_ROOT}/.claude/hooks/record-session.sh",
            "timeout": 10
          }
        ]
      }
    ],
    "StopFailure": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "${PROJECT_ROOT}/.claude/hooks/record-session.sh --failed",
            "timeout": 10
          }
        ]
      }
    ]
  }
}
```

`PostToolUse` runs with `async: true` — logging must never block Claude's execution.
`Stop`/`StopFailure` are synchronous because the session is already finished.

---

## 2. PostToolUse Hook — Tool-Level Log

### `.claude/hooks/record-tool-use.sh`

```bash
#!/usr/bin/env bash
# PostToolUse hook — records every tool call in full detail.
# Claude Code sends JSON on stdin after each tool execution completes.

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

# ── Preserve complete tool_input; extract human-readable detail field ─────────
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

# ── Resolve log path ──────────────────────────────────────────────────────────
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
```

---

## 3. Stop Hook — Session-Level Log

### `.claude/hooks/record-session.sh`

```bash
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
```

---

## 4. What the Logs Contain

### `tool-log.jsonl` — one record per tool call

```jsonl
{"ts":"2026-04-15T09:12:03Z","session":"cc_a1b2","task":"insight-orchestrator","turn":1,"tool":"Read","detail":"src/main/java/com/citi/bi/insight/ColumnProfile.java","input_cumulative":4820,"output_cumulative":48,"total_cumulative":4868,"exit_code":null,"tool_input":{"file_path":"src/main/java/com/citi/bi/insight/ColumnProfile.java"}}
{"ts":"2026-04-15T09:12:09Z","session":"cc_a1b2","task":"insight-orchestrator","turn":2,"tool":"Read","detail":"src/main/java/com/citi/bi/insight/ColumnProfile.java","input_cumulative":6920,"output_cumulative":51,"total_cumulative":6971,"exit_code":null,"tool_input":{"file_path":"src/main/java/com/citi/bi/insight/ColumnProfile.java"}}
{"ts":"2026-04-15T09:12:18Z","session":"cc_a1b2","task":"insight-orchestrator","turn":3,"tool":"Bash","detail":"sed -i 's/StatusSemantics.UNKNOWN/StatusSemantics.INFERRED/g' src/main/java/com/citi/bi/inference/InferenceStage.java","input_cumulative":9100,"output_cumulative":312,"total_cumulative":9412,"exit_code":0,"tool_input":{"command":"sed -i 's/StatusSemantics.UNKNOWN/StatusSemantics.INFERRED/g' src/main/java/com/citi/bi/inference/InferenceStage.java"}}
{"ts":"2026-04-15T09:12:31Z","session":"cc_a1b2","task":"insight-orchestrator","turn":4,"tool":"Bash","detail":"./gradlew test --tests '*InferenceStageTest'","input_cumulative":9800,"output_cumulative":890,"total_cumulative":10690,"exit_code":0,"tool_input":{"command":"./gradlew test --tests '*InferenceStageTest'"}}
{"ts":"2026-04-15T09:12:44Z","session":"cc_a1b2","task":"insight-orchestrator","turn":5,"tool":"Edit","detail":"src/main/java/com/citi/bi/insight/InsightOrchestrator.java","input_cumulative":11200,"output_cumulative":1240,"total_cumulative":12440,"exit_code":null,"tool_input":{"file_path":"src/main/java/com/citi/bi/insight/InsightOrchestrator.java","old_str":"// TODO post-pipeline","new_str":"orchestrator.run(profiledColumns);"}}
```

The second `Read` of `ColumnProfile.java` at turn 2 is immediately visible —
same file Claude read at turn 1. That is a direct efficiency signal.

For `Edit` and `MultiEdit` records, `tool_input` contains the full `old_str`
and `new_str` so you can see exactly what was changed and replay it as a
standalone `sed` or patch if the operation is a candidate for automation.

### `session-log.jsonl` — one record per session

```jsonl
{"ts":"2026-04-15T09:14:02Z","session":"cc_a1b2","task":"insight-orchestrator","model":"claude-sonnet-4-6","input":18420,"output":3210,"cache_read":12400,"cache_write":4200,"total":21630,"turns":8,"failed":false}
```

---

## 5. Querying the Tool Log

### All Bash commands across all sessions, in order

```bash
jq -r 'select(.tool == "Bash") | [.ts, .task, .session, .turn, .detail] | @tsv' \
  .claude/tool-log.jsonl
```

### Files read more than once within a session

```bash
jq -s '
  group_by(.session)
  | map({
      session: .[0].session,
      task:    .[0].task,
      repeated_reads: (
        map(select(.tool == "Read"))
        | group_by(.detail)
        | map(select(length > 1) | {file: .[0].detail, reads: length})
      )
    })
  | map(select(.repeated_reads | length > 0))
' .claude/tool-log.jsonl
```

### Where context grows fastest (top 10 largest single-turn deltas)

```bash
jq -s '
  group_by(.session)
  | map(
      sort_by(.turn)
      | to_entries
      | map(select(.key > 0) | {
          session: .value.session,
          turn:    .value.turn,
          tool:    .value.tool,
          detail:  .value.detail,
          delta:   (.value.total_cumulative - .[((.key) - 1)].value.total_cumulative)
        })
    )
  | flatten
  | sort_by(-.delta)
  | .[0:10]
' .claude/tool-log.jsonl
```

### All edits made by Claude — extractable as patches

```bash
jq -r '
  select(.tool == "Edit" or .tool == "MultiEdit")
  | [.ts, .task, .tool_input.file_path, .tool_input.old_str, .tool_input.new_str]
  | @json
' .claude/tool-log.jsonl
```

---

## 6. Pre-Automation Gate Script

Gate operates on `session-log.jsonl` — session totals are the right unit for
budget decisions.

### `.claude/gate/pre-automation-gate.sh`

```bash
#!/usr/bin/env bash
# Usage:
#   ./pre-automation-gate.sh --task <label> --budget <tokens> [--min-runs <n>]

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
```

---

## 7. Gradle Integration

```kotlin
// build.gradle.kts

tasks.register<Exec>("tokenGate") {
    group = "claude-code"
    description = "Pre-automation gate: validates session token p95 against budget"
    val ccTask   = project.findProperty("ccTask")?.toString()   ?: error("Pass -PccTask=<label>")
    val ccBudget = project.findProperty("ccBudget")?.toString() ?: error("Pass -PccBudget=<tokens>")
    val minRuns  = project.findProperty("ccMinRuns")?.toString() ?: "5"
    commandLine(
        "bash", "${rootDir}/.claude/gate/pre-automation-gate.sh",
        "--task", ccTask, "--budget", ccBudget, "--min-runs", minRuns
    )
}

tasks.register<Exec>("tokenStats") {
    group = "claude-code"
    description = "Session-level cost summary"
    val ccTask  = project.findProperty("ccTask")?.toString() ?: "all"
    val logFile = "${rootDir}/.claude/session-log.jsonl"
    commandLine("bash", "-c", """
        jq -s '
          map(select(.failed == false))
          | if ("$ccTask" != "all") then map(select(.task == "$ccTask")) else . end
          | group_by(.task)
          | map({
              task:       .[0].task,
              runs:       length,
              avg:        (map(.total) | add / length | floor),
              p95:        (sort_by(.total)[(length * 0.95 | floor)].total),
              max:        (map(.total) | max),
              avg_turns:  (map(.turns) | add / length | floor),
              cache_pct:  ((map(.cache_read)|add) / (map(.input)|add) * 100 | floor)
            })
        ' $logFile
    """.trimIndent())
}

tasks.register<Exec>("tokenToolBreakdown") {
    group = "claude-code"
    description = "Per-tool-type call counts from tool log"
    val ccTask  = project.findProperty("ccTask")?.toString() ?: "all"
    val logFile = "${rootDir}/.claude/tool-log.jsonl"
    commandLine("bash", "-c", """
        jq -s '
          if ("$ccTask" != "all") then map(select(.task == "$ccTask")) else . end
          | group_by(.tool)
          | map({
              tool:           .[0].tool,
              calls:          length,
              unique_details: (map(.detail) | unique | length)
            })
          | sort_by(-.calls)
        ' $logFile
    """.trimIndent())
}

tasks.register<Exec>("tokenRepeatedReads") {
    group = "claude-code"
    description = "Files read more than once within a session"
    val logFile = "${rootDir}/.claude/tool-log.jsonl"
    commandLine("bash", "-c", """
        jq -rs '
          group_by(.session)
          | map({
              session: .[0].session,
              task:    .[0].task,
              repeated: (
                map(select(.tool == "Read"))
                | group_by(.detail)
                | map(select(length > 1) | {file: .[0].detail, count: length})
              )
            })
          | map(select(.repeated | length > 0))
        ' $logFile
    """.trimIndent())
}

// Example: gate wired as a dependency
tasks.register<Exec>("runInsightPipeline") {
    group = "claude-code"
    description = "Runs insight orchestrator via Claude Code (gated)"
    dependsOn("tokenGate")
    environment("CLAUDE_TASK_LABEL", "insight-orchestrator")
    commandLine(
        "claude", "--output-format", "json", "--max-turns", "20",
        "run the insight orchestrator on the test dataset"
    )
}
```

---

## 8. Task Labelling

`CLAUDE_TASK_LABEL` is inherited by both hooks from the environment of the
Claude Code process. Set it before invoking Claude:

```bash
# Direct
CLAUDE_TASK_LABEL=insight-orchestrator claude "run the insight orchestrator..."

# Helper script: run-cc-task.sh <label> <prompt...>
export CLAUDE_TASK_LABEL="$1"; shift
claude "$@"
```

Gradle `Exec` tasks set it via `environment("CLAUDE_TASK_LABEL", "...")`.

---

## 9. Automation Candidate Signals

| Signal | Source | Strong candidate | Investigate first |
|---|---|---|---|
| Low p95 variance | `tokenStats` | p95 < 120% of avg | p95 > 150% of avg |
| High cache ratio | `tokenStats` | cache_pct > 70 | cache_pct < 40 |
| Low avg turns | `tokenStats` | ≤ 4 turns | ≥ 10 turns |
| No repeated reads | `tokenRepeatedReads` | Zero | Same file 3+ times |
| Stable Bash commands | `tool-log.jsonl` | Identical commands every run | Commands vary per run |
| Edit old_str stable | `tool-log.jsonl` | Same old_str every run | old_str differs — context-dependent |

---

## 10. Quick-Start Checklist

```
[ ] Create .claude/hooks/record-tool-use.sh    (chmod +x)
[ ] Create .claude/hooks/record-session.sh     (chmod +x)
[ ] Create .claude/gate/pre-automation-gate.sh (chmod +x)
[ ] Wire all three hooks in .claude/settings.json
[ ] Add tool-log.jsonl, session-log.jsonl, settings.local.json to .gitignore
[ ] Add Gradle tasks to build.gradle.kts
[ ] Run target task manually 5+ times with CLAUDE_TASK_LABEL set
[ ] ./gradlew tokenStats            — verify session log is populating
[ ] ./gradlew tokenToolBreakdown    — check tool call distribution
[ ] ./gradlew tokenRepeatedReads    — check for redundant file reads
[ ] ./gradlew tokenGate -PccTask=<label> -PccBudget=<n>
[ ] Wire tokenGate as dependsOn into any automated task
```
