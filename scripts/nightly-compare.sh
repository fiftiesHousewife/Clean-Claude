#!/usr/bin/env bash
# One-shot 4-way reworkCompare against the canonical 10-file batch.
#
# Replaces the manual handoff sequence. Does four things:
#   1. Publishes ALL modules to mavenLocal (NOT just the ones you think
#      changed — the 2026-04-19 run was broken by a stale recipes jar
#      because the handoff only published refactoring+plugin).
#   2. Runs :sandbox:reworkCompare with the standard batch baked in.
#   3. Streams a 30s heartbeat via watch-rework-run.sh so you can
#      watch progress in the same terminal.
#   4. Archives the raw comparison markdown under docs/sessions/ and
#      prints a headline-numbers diff against the previous archive.
#
# Usage: ./scripts/nightly-compare.sh [run-id-suffix]
# Default run-id is a timestamp; pass an explicit suffix for a named run.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$PROJECT_ROOT"

STANDARD_BATCH=(
  sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
  sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
  sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
  sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
  sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
  sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
  sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
  sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
  sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
  sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
)
FILES=$(IFS=,; echo "${STANDARD_BATCH[*]}")

TS=$(date +%Y%m%d-%H%M%S)
LOG_DIR="/tmp/cleanclaude"
LOG="$LOG_DIR/rework-4way-$TS.log"
mkdir -p "$LOG_DIR"
echo "$LOG" > "$LOG_DIR/current-log"

echo "[1/4] publishToMavenLocal (ALL modules — prevents the stale-jar trap)"
./gradlew publishToMavenLocal -q

echo "[2/4] reworkCompare on the standard 10-file batch — 30s heartbeat below"
{
  date
  echo "command: reworkCompare 4-way, standard 10-file batch"
  ./gradlew -PcleanCodeSelfApply=true :sandbox:reworkCompare -Pfiles="$FILES"
  echo "=== EXIT $? ==="
  date
} >> "$LOG" 2>&1 &
GRADLE_PID=$!

bash "$SCRIPT_DIR/watch-rework-run.sh" "$LOG" 30 &
WATCH_PID=$!

set +e
wait "$GRADLE_PID"
GRADLE_EXIT=$?
set -e
kill "$WATCH_PID" 2>/dev/null || true
wait "$WATCH_PID" 2>/dev/null || true

if [ "$GRADLE_EXIT" -ne 0 ]; then
  echo "reworkCompare failed with exit $GRADLE_EXIT — see $LOG"
  exit "$GRADLE_EXIT"
fi

echo "[3/4] archive raw comparison to docs/sessions/"
SUFFIX=${1:-}
DATE=$(date +%Y-%m-%d)
SEQ=1
while [ -f "docs/sessions/$DATE-rework-run$SEQ${SUFFIX:+-$SUFFIX}-raw.md" ]; do
  SEQ=$((SEQ+1))
done
RAW="docs/sessions/$DATE-rework-run$SEQ${SUFFIX:+-$SUFFIX}-raw.md"
cp sandbox/build/reports/clean-code/batch-10-comparison.md "$RAW"
echo "  archived: $RAW"

echo "[4/4] summary"
echo ""
echo "  lifecycle lines:"
grep -E "^\s+(VANILLA|MCP_GRADLE_ONLY|MCP_RECIPES|HARNESS)" "$LOG" | sed 's/^/    /'

PREV=$(ls -t docs/sessions/*-rework-run*-raw.md 2>/dev/null \
       | grep -Fv "$RAW" | head -1 || true)
if [ -n "$PREV" ] && [ -f "$SCRIPT_DIR/compare-runs.py" ]; then
  echo ""
  echo "  diff vs previous archive ($PREV):"
  python3 "$SCRIPT_DIR/compare-runs.py" "$PREV" "$RAW" | sed 's/^/    /'
fi

echo ""
echo "done. log: $LOG"
