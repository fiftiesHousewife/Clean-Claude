---
name: experiment-save
description: Save outputs after an experiment run completes. Run from the experiment branch after the fix session finishes.
disable-model-invocation: true
argument-hint: (no arguments — detects approach and run number from branch name)
allowed-tools: Bash(git *) Bash(cp *) Bash(mkdir *) Bash(cat *) Bash(jq *) Bash(wc *) Bash(python3 *)
---

# Save Experiment Run Outputs

## Step 1: Detect run from branch name

```bash
BRANCH=$(git branch --show-current)
```

The branch should be `experiment/manual-<n>` or `experiment/recipe-<n>`. Parse approach and run number from it. If the branch doesn't match that pattern, abort with a clear message.

## Step 2: Save outputs

```bash
mkdir -p experiment

# Save the patch (all commits since diverging from main)
git diff $(git merge-base HEAD main)..HEAD > experiment/<approach>-<n>.patch

# Save token logs
cp .claude/tool-log.jsonl experiment/<approach>-<n>-tools.jsonl
cp .claude/session-log.jsonl experiment/<approach>-<n>-session.jsonl
```

## Step 3: Extract agent token usage from transcript

Claude Code does NOT expose token counts in hook payloads or via `/cost` on Max subscription. The only reliable token data comes from subagent task notifications in the transcript, which contain `<usage><total_tokens>N</total_tokens><tool_uses>N</tool_uses><duration_ms>N</duration_ms></usage>` blocks.

Extract the session ID from the tool log, find the transcript, and parse agent usage:

```bash
SESSION_ID=$(head -1 experiment/<approach>-<n>-tools.jsonl | jq -r '.session')
```

```python
python3 -c "
import json, re, sys, os, glob

session_id = sys.argv[1]
approach = sys.argv[2]
run = sys.argv[3]

# Find transcript
pattern = os.path.expanduser(f'~/.claude/projects/*/{session_id}.jsonl')
matches = glob.glob(pattern)
if not matches:
    print('Transcript not found')
    sys.exit(0)

with open(matches[0]) as f:
    full = f.read()

transcript_bytes = len(full.encode())

# Extract <usage> blocks — each appears twice (notification + result), so deduplicate
seen = set()
agents = []
for m in re.finditer(
    r'<usage><total_tokens>(\d+)</total_tokens>'
    r'<tool_uses>(\d+)</tool_uses>'
    r'<duration_ms>(\d+)</duration_ms></usage>', full):
    key = (int(m.group(1)), int(m.group(2)), int(m.group(3)))
    if key not in seen:
        seen.add(key)
        agents.append(key)

agent_tokens = sum(t for t,_,_ in agents)
agent_tool_uses = sum(u for _,u,_ in agents)
agent_duration_ms = sum(ms for _,_,ms in agents)

summary = {
    'approach': approach,
    'run': int(run),
    'session_id': session_id,
    'transcript_bytes': transcript_bytes,
    'agent_count': len(agents),
    'agent_tokens': agent_tokens,
    'agent_tool_uses': agent_tool_uses,
    'agent_duration_ms': agent_duration_ms,
    'agents': [{'tokens': t, 'tool_uses': u, 'duration_ms': ms}
               for t,u,ms in sorted(agents, key=lambda x: -x[0])],
}

out = f'experiment/{approach}-{run}-usage.json'
with open(out, 'w') as f:
    json.dump(summary, f, indent=2)
print(json.dumps(summary, indent=2))
" "$SESSION_ID" "<approach>" "<n>"
```

## Step 4: Report summary

Show the user a table with:
- Patch stats: files changed, lines added/removed (`git diff --stat $(git merge-base HEAD main)..HEAD | tail -1`)
- Commit count: `git log --oneline $(git merge-base HEAD main)..HEAD | wc -l`
- Main tool calls: `wc -l < experiment/<approach>-<n>-tools.jsonl`
- Tool breakdown: group by `.tool` field in the tools JSONL
- Agent tokens: from the usage JSON written in step 3
- Agent tool calls: from the usage JSON
- Transcript size: from the usage JSON (in KB)

## Step 5: Prompt next action

Tell the user:
> Run saved. To return to main:
> ```bash
> git checkout main
> ```
> Then start the next run with `/experiment <approach> <next-n>`.

If this was run 3 of both approaches (i.e. all 6 experiment branches exist), suggest running `/experiment-analyse` instead.
