#!/usr/bin/env bash
#
# One-shot experiment runner. Creates the branch, clears logs, generates
# fresh per-file briefs, invokes Claude non-interactively with the fix
# prompt, and saves outputs. Safe to call from cron / a scheduled trigger.
#
# Usage: scripts/run-experiment.sh <manual|recipe> <run-number>
#
# Example: scripts/run-experiment.sh manual 1
#
# Requires: JAVA_HOME pointing at JDK 21, and a working `claude` CLI.

set -euo pipefail

usage() {
    echo "Usage: $0 <manual|recipe> <run-number>" >&2
    exit 1
}

[[ $# -eq 2 ]] || usage

APPROACH=$1
RUN=$2

case "$APPROACH" in
    manual|recipe) ;;
    *) usage ;;
esac

[[ "$RUN" =~ ^[0-9]+$ ]] || usage

REPO="$(git rev-parse --show-toplevel)"
cd "$REPO"

BRANCH="experiment/${APPROACH}-${RUN}"
LABEL="${APPROACH}-fix-${RUN}"
PROMPT_FILE="$REPO/scripts/experiment-${APPROACH}-prompt.txt"

[[ -f "$PROMPT_FILE" ]] || { echo "Missing prompt file: $PROMPT_FILE" >&2; exit 1; }

echo "=== Pre-flight checks ==="
if [[ -n "$(git status --porcelain)" ]]; then
    echo "Working tree is not clean. Commit or stash first." >&2
    exit 1
fi
if [[ "$(git branch --show-current)" != "main" ]]; then
    echo "Not on main. Checkout main first." >&2
    exit 1
fi
if git rev-parse --verify "$BRANCH" >/dev/null 2>&1; then
    echo "Branch $BRANCH already exists. Delete it or pick a different run number." >&2
    exit 1
fi
if ! command -v claude >/dev/null 2>&1; then
    echo "claude CLI not found on PATH." >&2
    exit 1
fi

BASELINE=$(git rev-parse HEAD)
echo "Baseline commit: $BASELINE"

echo "=== Creating branch $BRANCH ==="
git checkout -b "$BRANCH" "$BASELINE"

echo "=== Clearing token logs ==="
rm -f .claude/tool-log.jsonl .claude/session-log.jsonl

echo "=== Generating fresh per-file briefs ==="
./gradlew --quiet analyseCleanCode cleanCodeFixPlan

echo "=== Launching Claude (task=$LABEL) ==="
CLAUDE_TASK_LABEL="$LABEL" claude -p "$(<"$PROMPT_FILE")" --dangerously-skip-permissions

echo "=== Saving outputs ==="
mkdir -p experiment
git diff "$(git merge-base HEAD main)..HEAD" > "experiment/${APPROACH}-${RUN}.patch"
cp -f .claude/tool-log.jsonl    "experiment/${APPROACH}-${RUN}-tools.jsonl"    2>/dev/null || true
cp -f .claude/session-log.jsonl "experiment/${APPROACH}-${RUN}-session.jsonl"  2>/dev/null || true

SESSION_ID=""
if [[ -f "experiment/${APPROACH}-${RUN}-tools.jsonl" ]]; then
    SESSION_ID=$(head -1 "experiment/${APPROACH}-${RUN}-tools.jsonl" | jq -r '.session // empty')
fi

if [[ -n "$SESSION_ID" ]]; then
    python3 - "$SESSION_ID" "$APPROACH" "$RUN" <<'PY'
import json, re, sys, os, glob

session_id, approach, run = sys.argv[1], sys.argv[2], sys.argv[3]
pattern = os.path.expanduser(f"~/.claude/projects/*/{session_id}.jsonl")
matches = glob.glob(pattern)
if not matches:
    print(f"Transcript not found for session {session_id}")
    sys.exit(0)

totals = {"input": 0, "cache_creation": 0, "cache_read": 0, "output": 0, "turns": 0}
transcript_bytes = 0

with open(matches[0]) as f:
    for line in f:
        transcript_bytes += len(line.encode())
        try:
            d = json.loads(line)
        except json.JSONDecodeError:
            continue
        msg = d.get("message")
        if d.get("type") == "assistant" and isinstance(msg, dict):
            u = msg.get("usage") or {}
            totals["input"]          += u.get("input_tokens", 0)
            totals["cache_creation"] += u.get("cache_creation_input_tokens", 0)
            totals["cache_read"]     += u.get("cache_read_input_tokens", 0)
            totals["output"]         += u.get("output_tokens", 0)
            totals["turns"]          += 1

with open(matches[0]) as f:
    full = f.read()
seen, agents = set(), []
for m in re.finditer(
        r"<usage><total_tokens>(\d+)</total_tokens>"
        r"<tool_uses>(\d+)</tool_uses>"
        r"<duration_ms>(\d+)</duration_ms></usage>", full):
    key = (int(m.group(1)), int(m.group(2)), int(m.group(3)))
    if key not in seen:
        seen.add(key)
        agents.append(key)

total_tokens = sum(totals.values()) - totals["turns"]
billed_tokens = totals["input"] + totals["cache_creation"] + totals["output"]
cache_hit_pct = round(totals["cache_read"] / total_tokens * 100, 1) if total_tokens else 0

summary = {
    "approach": approach,
    "run": int(run),
    "session_id": session_id,
    "transcript_bytes": transcript_bytes,
    "turns": totals["turns"],
    "total_tokens": total_tokens,
    "billed_tokens": billed_tokens,
    "input_tokens": totals["input"],
    "cache_creation_tokens": totals["cache_creation"],
    "cache_read_tokens": totals["cache_read"],
    "output_tokens": totals["output"],
    "cache_hit_pct": cache_hit_pct,
    "agent_count": len(agents),
    "agent_tokens_sum": sum(t for t, _, _ in agents),
}
out = f"experiment/{approach}-{run}-usage.json"
with open(out, "w") as f:
    json.dump(summary, f, indent=2)
print(json.dumps({k: summary[k] for k in
    ["turns", "total_tokens", "billed_tokens", "cache_hit_pct", "agent_count"]}, indent=2))
PY
fi

echo "=== Done. Branch $BRANCH ready for review ==="
echo "Patch:       experiment/${APPROACH}-${RUN}.patch"
echo "Usage JSON:  experiment/${APPROACH}-${RUN}-usage.json"
