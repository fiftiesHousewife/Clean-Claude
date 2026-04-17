#!/usr/bin/env bash
#
# Experiment runner. Creates (or resumes) the branch, clears logs, generates
# fresh per-file briefs, invokes Claude non-interactively with the fix
# prompt, and saves outputs. Safe to call from cron / a scheduled trigger.
#
# Usage: scripts/run-experiment.sh <manual|recipe> <run-number>
#
# Behaviour:
#   - First invocation: create branch experiment/<approach>-<n> from main.
#   - Re-invocation with the same approach + run: the branch already exists,
#     so the script resumes. Existing logs are archived as
#     .claude/tool-log-part<k>.jsonl / session-log-part<k>.jsonl before the
#     new session starts. Final outputs concatenate every part.
#
# Requires: JAVA_HOME pointing at JDK 21, and a working `claude` CLI on PATH.

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
PROMPT_FILE="$REPO/scripts/experiment-${APPROACH}-prompt.txt"

[[ -f "$PROMPT_FILE" ]] || { echo "Missing prompt file: $PROMPT_FILE" >&2; exit 1; }

echo "=== Pre-flight checks ==="
if [[ -n "$(git status --porcelain)" ]]; then
    echo "Working tree is not clean. Commit or stash first." >&2
    exit 1
fi
if ! command -v claude >/dev/null 2>&1; then
    echo "claude CLI not found on PATH." >&2
    exit 1
fi

# Decide fresh vs resume.
MODE="fresh"
if git rev-parse --verify "$BRANCH" >/dev/null 2>&1; then
    MODE="resume"
fi

if [[ "$MODE" = "fresh" ]]; then
    if [[ "$(git branch --show-current)" != "main" ]]; then
        echo "Not on main. Checkout main first." >&2
        exit 1
    fi
    BASELINE=$(git rev-parse HEAD)
    echo "Baseline commit: $BASELINE"
    echo "=== Creating branch $BRANCH ==="
    git checkout -b "$BRANCH" "$BASELINE"
    rm -f .claude/tool-log.jsonl .claude/session-log.jsonl
    LABEL="${APPROACH}-fix-${RUN}"
else
    if [[ "$(git branch --show-current)" != "$BRANCH" ]]; then
        echo "=== Switching to existing $BRANCH to resume ==="
        git checkout "$BRANCH"
    else
        echo "=== Resuming on $BRANCH ==="
    fi
    # Next archive slot: part1, part2, ...
    PART=1
    while [[ -e ".claude/tool-log-part${PART}.jsonl" \
             || -e ".claude/session-log-part${PART}.jsonl" ]]; do
        PART=$((PART + 1))
    done
    [[ -e .claude/tool-log.jsonl    ]] && mv .claude/tool-log.jsonl    ".claude/tool-log-part${PART}.jsonl"
    [[ -e .claude/session-log.jsonl ]] && mv .claude/session-log.jsonl ".claude/session-log-part${PART}.jsonl"
    echo "Archived prior logs as part${PART}. Fresh logs start for this session."
    LABEL="${APPROACH}-fix-${RUN}-resume${PART}"
fi

echo "=== Publishing plugin + deps to mavenLocal ==="
./gradlew --quiet publishToMavenLocal

echo "=== Generating fresh per-file briefs ==="
./gradlew --quiet \
    --init-script "$REPO/scripts/cleancode-dogfood.init.gradle.kts" \
    analyseCleanCode cleanCodeFixPlan

echo "=== Launching Claude (task=$LABEL, mode=$MODE) ==="
if [[ "$MODE" = "resume" ]]; then
    PROMPT_HEADER=$'This is a RESUME of an earlier run that was interrupted. The branch already has commits from prior agents. Briefs at build/reports/clean-code/fix-briefs/ reflect the CURRENT state - only files that still have findings appear. Continue the same protocol as below.\n\n'
else
    PROMPT_HEADER=""
fi

PROMPT="${PROMPT_HEADER}$(<"$PROMPT_FILE")"
CLAUDE_TASK_LABEL="$LABEL" claude -p "$PROMPT" --dangerously-skip-permissions

echo "=== Saving outputs ==="
mkdir -p experiment

# Patch: everything on the branch since diverging from main
git diff "$(git merge-base HEAD main)..HEAD" > "experiment/${APPROACH}-${RUN}.patch"

# Combine every part + the current session's logs, in order
TOOLS_OUT="experiment/${APPROACH}-${RUN}-tools.jsonl"
SESSION_OUT="experiment/${APPROACH}-${RUN}-session.jsonl"
: > "$TOOLS_OUT"
: > "$SESSION_OUT"
for part_log in $(ls -v .claude/tool-log-part*.jsonl 2>/dev/null); do
    cat "$part_log" >> "$TOOLS_OUT"
done
[[ -e .claude/tool-log.jsonl ]] && cat .claude/tool-log.jsonl >> "$TOOLS_OUT"
for part_log in $(ls -v .claude/session-log-part*.jsonl 2>/dev/null); do
    cat "$part_log" >> "$SESSION_OUT"
done
[[ -e .claude/session-log.jsonl ]] && cat .claude/session-log.jsonl >> "$SESSION_OUT"

# Token usage summary across sessions tagged with this experiment's task label.
# Filtering by task prevents pollution from unrelated Claude Code sessions
# (e.g. interactive dev work) whose tool calls landed in the same tool log.
python3 - "$APPROACH" "$RUN" "$TOOLS_OUT" <<'PY'
import json, re, sys, os, glob

approach, run, tools_path = sys.argv[1], sys.argv[2], sys.argv[3]
task_prefix = f"{approach}-fix-{run}"

session_ids = []
seen_ids = set()
if os.path.exists(tools_path):
    with open(tools_path) as f:
        for line in f:
            try:
                d = json.loads(line)
            except json.JSONDecodeError:
                continue
            sid = d.get("session")
            task = d.get("task") or ""
            if not sid or sid in seen_ids:
                continue
            if not task.startswith(task_prefix):
                continue
            seen_ids.add(sid)
            session_ids.append(sid)

totals = {"input": 0, "cache_creation": 0, "cache_read": 0, "output": 0, "turns": 0}
transcript_bytes = 0
agents = []
agent_seen = set()

for sid in session_ids:
    matches = glob.glob(os.path.expanduser(f"~/.claude/projects/*/{sid}.jsonl"))
    if not matches:
        continue
    path = matches[0]
    with open(path) as f:
        for line in f:
            transcript_bytes += len(line.encode())
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
    with open(path) as f:
        full = f.read()
    for m in re.finditer(
            r"<usage><total_tokens>(\d+)</total_tokens>"
            r"<tool_uses>(\d+)</tool_uses>"
            r"<duration_ms>(\d+)</duration_ms></usage>", full):
        key = (int(m.group(1)), int(m.group(2)), int(m.group(3)))
        if key not in agent_seen:
            agent_seen.add(key)
            agents.append(key)

total_tokens  = totals["input"] + totals["cache_creation"] + totals["cache_read"] + totals["output"]
billed_tokens = totals["input"] + totals["cache_creation"] + totals["output"]
cache_hit_pct = round(totals["cache_read"] / total_tokens * 100, 1) if total_tokens else 0

summary = {
    "approach": approach,
    "run": int(run),
    "session_ids": session_ids,
    "sessions": len(session_ids),
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
    ["sessions", "turns", "total_tokens", "billed_tokens", "cache_hit_pct", "agent_count"]},
    indent=2))
PY

echo "=== Done. Branch $BRANCH ready for review ==="
echo "Patch:       experiment/${APPROACH}-${RUN}.patch"
echo "Usage JSON:  experiment/${APPROACH}-${RUN}-usage.json"
echo
echo "To resume later, re-run the same command:"
echo "  scripts/run-experiment.sh ${APPROACH} ${RUN}"
