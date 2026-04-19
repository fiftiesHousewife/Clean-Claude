#!/usr/bin/env bash
#
# Single-file experiment harness (A3 from docs/plan-next-session.md).
#
# Usage: scripts/run-single-file.sh <relative/path/to/File.java>
#
# What it does:
#   1. Creates a scratch branch `debug/single-<timestamp>-<ClassName>` from main.
#   2. Publishes the plugin to mavenLocal and runs analyseCleanCode +
#      cleanCodeFixPlan so the per-file brief at
#      build/reports/clean-code/fix-briefs/<ClassName>.md reflects the
#      current state of the target.
#   3. Invokes `claude -p` non-interactively with ONLY that brief as the
#      prompt. The agent is scoped to a single file; no other briefs, no
#      _INDEX.md, no multi-file orchestration.
#   4. Captures: commits made on the branch, any new .java files, module
#      test status, and token usage for the session.
#   5. Writes a sub-minute summary to
#      experiment/single/<timestamp>-<ClassName>.md and prints the
#      highlights to stdout.
#
# Use cases:
#   - Iterating on fix-brief wording (A1) without paying for a full run.
#   - Testing a new recipe against a known target.
#   - Reproducing pathological agent behaviour observed in a larger run.
#   - Piloting A1/A2 changes against one file before shipping them.
#
# Requirements: clean working tree, currently on main, `claude` on PATH.

set -euo pipefail

usage() {
    echo "Usage: $0 <relative/path/to/File.java>" >&2
    exit 1
}

[[ $# -eq 1 ]] || usage

TARGET="$1"
[[ "$TARGET" == *.java ]] || { echo "Target must be a .java file: $TARGET" >&2; exit 1; }

REPO="$(git rev-parse --show-toplevel)"
cd "$REPO"

[[ -f "$TARGET" ]] || { echo "File not found: $TARGET" >&2; exit 1; }

if [[ -n "$(git status --porcelain)" ]]; then
    echo "Working tree is not clean. Commit or stash first." >&2
    exit 1
fi
if [[ "$(git branch --show-current)" != "main" ]]; then
    echo "Not on main. Checkout main first." >&2
    exit 1
fi
command -v claude >/dev/null 2>&1 || { echo "claude CLI not found on PATH." >&2; exit 1; }

CLASS_NAME="$(basename "$TARGET" .java)"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
BRANCH="debug/single-${TIMESTAMP}-${CLASS_NAME}"
BASELINE="$(git rev-parse HEAD)"
MODULE="${TARGET%%/*}"
SUMMARY_DIR="experiment/single"
SUMMARY_FILE="${SUMMARY_DIR}/${TIMESTAMP}-${CLASS_NAME}.md"
LABEL="single-${TIMESTAMP}-${CLASS_NAME}"

mkdir -p "$SUMMARY_DIR"

echo "=== Pre-flight ==="
echo "  target:   $TARGET"
echo "  module:   $MODULE"
echo "  branch:   $BRANCH"
echo "  baseline: $BASELINE"
echo "  summary:  $SUMMARY_FILE"

echo "=== Creating branch ==="
git checkout -b "$BRANCH" "$BASELINE"

echo "=== Publishing plugin + deps to mavenLocal ==="
./gradlew --quiet publishToMavenLocal

echo "=== Running analyseCleanCode + cleanCodeFixPlan ==="
./gradlew --quiet \
    --init-script "$REPO/scripts/cleancode-dogfood.init.gradle.kts" \
    analyseCleanCode cleanCodeFixPlan

BRIEF="${MODULE}/build/reports/clean-code/fix-briefs/${CLASS_NAME}.md"
if [[ ! -f "$BRIEF" ]]; then
    echo "No brief generated for $TARGET — the analyser did not produce findings on this file."
    echo "Rolling back to main."
    git checkout main
    git branch -D "$BRANCH"
    exit 0
fi

FINDINGS_COUNT="$(grep -c '^- L' "$BRIEF" || true)"
echo "Brief: $BRIEF ($FINDINGS_COUNT finding(s))"

SCRATCH_BEFORE_AGENT=$(git log --format=%H "$BASELINE..HEAD" | wc -l | tr -d ' ')

PROMPT_HEADER=$'You are fixing Clean Code findings on a SINGLE file. The brief below is complete — no other files are in scope, no index to consult, no other briefs to read. Read the file, apply the skills cited in the brief, make the fix, run the module tests.\n\nRules:\n- Stay inside the target file. Do not Read/Edit/Write under `refactoring/`, `mcp/`, `plugin/`, `build-logic/`, or any module other than the target.\n- Commit after the fix using the format `fix: <ClassName> (<code>:<choice>, …)` with a short body explaining each non-trivial remediation choice.\n- Run `./gradlew :<module>:test` before you exit.\n\nBrief follows:\n---\n\n'

PROMPT="${PROMPT_HEADER}$(<"$BRIEF")"

echo "=== Invoking claude (label=$LABEL) ==="
CLAUDE_TASK_LABEL="$LABEL" claude -p "$PROMPT" --dangerously-skip-permissions

echo "=== Post-run snapshot ==="
NEW_COMMITS=()
while IFS= read -r sha; do
    NEW_COMMITS+=("$sha")
done < <(git log --format="%H %s" "$BASELINE..HEAD")

NEW_JAVA_FILES=()
while IFS= read -r f; do
    [[ -n "$f" ]] && NEW_JAVA_FILES+=("$f")
done < <(git diff --name-only --diff-filter=A "$BASELINE..HEAD" -- '*.java')

TEST_STATUS="not-run"
if [[ -n "$MODULE" && -d "$MODULE" ]]; then
    if ./gradlew --quiet ":${MODULE}:test" 2>/dev/null; then
        TEST_STATUS="pass"
    else
        TEST_STATUS="fail"
    fi
fi

TOKENS_JSON="$(python3 - <<PY
import glob, json, os, re

label = "$LABEL"
tool_log = ".claude/tool-log.jsonl"
session_ids = []
seen = set()
if os.path.exists(tool_log):
    with open(tool_log) as f:
        for line in f:
            try:
                d = json.loads(line)
            except json.JSONDecodeError:
                continue
            sid = d.get("session")
            task = d.get("task") or ""
            if sid and sid not in seen and task == label:
                seen.add(sid)
                session_ids.append(sid)

totals = {"input": 0, "cache_creation": 0, "cache_read": 0, "output": 0, "turns": 0}
for sid in session_ids:
    for path in glob.glob(os.path.expanduser(f"~/.claude/projects/*/{sid}.jsonl")):
        with open(path) as f:
            for line in f:
                try:
                    d = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if d.get("type") == "assistant" and isinstance(d.get("message"), dict):
                    u = d["message"].get("usage") or {}
                    totals["input"]          += u.get("input_tokens", 0)
                    totals["cache_creation"] += u.get("cache_creation_input_tokens", 0)
                    totals["cache_read"]     += u.get("cache_read_input_tokens", 0)
                    totals["output"]         += u.get("output_tokens", 0)
                    totals["turns"]          += 1

total_tokens = sum(totals.values()) - totals["turns"]
print(json.dumps({**totals, "session_count": len(session_ids), "total_tokens": total_tokens}))
PY
)"

{
    echo "# Single-file experiment: ${CLASS_NAME}"
    echo
    echo "- target:    \`${TARGET}\`"
    echo "- branch:    \`${BRANCH}\`"
    echo "- baseline:  \`${BASELINE}\`"
    echo "- timestamp: ${TIMESTAMP}"
    echo "- brief:     \`${BRIEF}\` (${FINDINGS_COUNT} finding(s))"
    echo "- module tests: ${TEST_STATUS}"
    echo
    echo "## Commits (${#NEW_COMMITS[@]})"
    if [[ ${#NEW_COMMITS[@]} -eq 0 ]]; then
        echo "_(none — agent made no commits)_"
    else
        for c in "${NEW_COMMITS[@]}"; do
            echo "- ${c}"
        done
    fi
    echo
    echo "## New .java files (${#NEW_JAVA_FILES[@]})"
    if [[ ${#NEW_JAVA_FILES[@]} -eq 0 ]]; then
        echo "_(none)_"
    else
        for f in "${NEW_JAVA_FILES[@]}"; do
            echo "- \`${f}\`"
        done
    fi
    echo
    echo "## Token usage"
    echo '```json'
    echo "${TOKENS_JSON}"
    echo '```'
    echo
    echo "## Brief that was sent"
    echo '```markdown'
    cat "$BRIEF"
    echo '```'
} > "$SUMMARY_FILE"

echo "=== Summary ==="
echo "  commits:       ${#NEW_COMMITS[@]}"
echo "  new .java:     ${#NEW_JAVA_FILES[@]}"
echo "  module tests:  ${TEST_STATUS}"
echo "  tokens:        ${TOKENS_JSON}"
echo
echo "Full write-up: ${SUMMARY_FILE}"
echo "Branch kept for review: ${BRANCH} (delete with \`git branch -D ${BRANCH}\` when done)"
