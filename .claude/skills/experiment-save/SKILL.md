---
name: experiment-save
description: Save outputs after an experiment run completes. Run from the experiment branch after the fix session finishes.
disable-model-invocation: true
argument-hint: (no arguments — detects approach and run number from branch name)
allowed-tools: Bash(git *) Bash(cp *) Bash(mkdir *) Bash(cat *) Bash(jq *) Bash(wc *)
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

## Step 3: Report summary

Show the user:
- Patch stats: number of files changed, lines added/removed (`git diff --stat $(git merge-base HEAD main)..HEAD`)
- Session token totals from the session log (`jq . experiment/<approach>-<n>-session.jsonl`)
- Tool call count (`wc -l < experiment/<approach>-<n>-tools.jsonl`)
- Tool breakdown (`jq -s 'group_by(.tool) | map({tool:.[0].tool, calls:length}) | sort_by(-.calls)' experiment/<approach>-<n>-tools.jsonl`)

## Step 4: Prompt next action

Tell the user:
> Run saved. To return to main:
> ```bash
> git checkout main
> ```
> Then start the next run with `/experiment <approach> <next-n>`.

If this was run 3 of both approaches (i.e. all 6 experiment branches exist), suggest running `/experiment-analyse` instead.
