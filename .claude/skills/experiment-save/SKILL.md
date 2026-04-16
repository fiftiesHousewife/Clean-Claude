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

## Step 3: Extract token usage from the transcript

Every `assistant` entry in `~/.claude/projects/<project-dir>/<session-id>.jsonl` has a `message.usage` object with `input_tokens`, `cache_creation_input_tokens`, `cache_read_input_tokens`, and `output_tokens`. Sum those across the whole session — that captures both driver and subagent token spend.

```bash
SESSION_ID=$(head -1 experiment/<approach>-<n>-tools.jsonl | jq -r '.session')
```

```python
python3 -c "
import json, re, sys, os, glob

session_id = sys.argv[1]
approach = sys.argv[2]
run = sys.argv[3]

pattern = os.path.expanduser(f'~/.claude/projects/*/{session_id}.jsonl')
matches = glob.glob(pattern)
if not matches:
    print('Transcript not found'); sys.exit(0)

totals = {'input': 0, 'cache_creation': 0, 'cache_read': 0, 'output': 0, 'turns': 0}
agents = []
transcript_bytes = 0

with open(matches[0]) as f:
    for line in f:
        transcript_bytes += len(line.encode())
        try:
            d = json.loads(line)
        except json.JSONDecodeError:
            continue
        msg = d.get('message')
        if d.get('type') == 'assistant' and isinstance(msg, dict):
            u = msg.get('usage') or {}
            totals['input']          += u.get('input_tokens', 0)
            totals['cache_creation'] += u.get('cache_creation_input_tokens', 0)
            totals['cache_read']     += u.get('cache_read_input_tokens', 0)
            totals['output']         += u.get('output_tokens', 0)
            totals['turns']          += 1

# Agent subagent usage blocks for per-agent breakdown (deduped; each appears twice)
with open(matches[0]) as f:
    full = f.read()
seen = set()
for m in re.finditer(
        r'<usage><total_tokens>(\d+)</total_tokens>'
        r'<tool_uses>(\d+)</tool_uses>'
        r'<duration_ms>(\d+)</duration_ms></usage>', full):
    key = (int(m.group(1)), int(m.group(2)), int(m.group(3)))
    if key not in seen:
        seen.add(key); agents.append(key)

total_tokens = totals['input'] + totals['cache_creation'] + totals['cache_read'] + totals['output']
billed_tokens = totals['input'] + totals['cache_creation'] + totals['output']   # cache reads are discounted
cache_hit_pct = (totals['cache_read'] / total_tokens * 100) if total_tokens else 0

summary = {
    'approach': approach,
    'run': int(run),
    'session_id': session_id,
    'transcript_bytes': transcript_bytes,
    'turns': totals['turns'],
    'total_tokens': total_tokens,
    'billed_tokens': billed_tokens,
    'input_tokens': totals['input'],
    'cache_creation_tokens': totals['cache_creation'],
    'cache_read_tokens': totals['cache_read'],
    'output_tokens': totals['output'],
    'cache_hit_pct': round(cache_hit_pct, 1),
    'agent_count': len(agents),
    'agent_tokens_sum': sum(t for t,_,_ in agents),
    'agents': [{'tokens': t, 'tool_uses': u, 'duration_ms': ms}
               for t,u,ms in sorted(agents, key=lambda x: -x[0])],
}

out = f'experiment/{approach}-{run}-usage.json'
with open(out, 'w') as f:
    json.dump(summary, f, indent=2)
print(json.dumps({k: summary[k] for k in
    ['turns', 'total_tokens', 'billed_tokens', 'cache_hit_pct', 'agent_count', 'agent_tokens_sum']},
    indent=2))
" "$SESSION_ID" "<approach>" "<n>"
```

## Step 4: Report summary

Show the user a table with:
- Patch stats: files changed, lines added/removed (`git diff --stat $(git merge-base HEAD main)..HEAD | tail -1`)
- Commit count: `git log --oneline $(git merge-base HEAD main)..HEAD | wc -l`
- Main tool calls: `wc -l < experiment/<approach>-<n>-tools.jsonl`
- Tool breakdown: group by `.tool` field in the tools JSONL
- Session token totals: `total_tokens`, `billed_tokens`, `cache_hit_pct` from the usage JSON
- Agent count and summed agent tokens (for delegation effort signal)
- Transcript size (in KB)

## Step 5: Prompt next action

Tell the user:
> Run saved. To return to main:
> ```bash
> git checkout main
> ```
> Then start the next run with `/experiment <approach> <next-n>`.

If this was run 3 of both approaches (i.e. all 6 experiment branches exist), suggest running `/experiment-analyse` instead.
