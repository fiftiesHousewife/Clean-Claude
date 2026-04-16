---
name: experiment-analyse
description: Analyse all experiment runs and produce a comparison report
disable-model-invocation: true
argument-hint: (no arguments)
allowed-tools: Bash(cat *) Bash(jq *) Bash(wc *) Bash(grep *) Bash(ls *)
---

# Analyse Experiment Results

## Step 1: Check available runs

List all `experiment/*-session.jsonl` files. Report which runs exist and which are missing. Proceed with whatever is available (minimum 1 of each approach to compare).

## Step 2: Session-level comparison

```bash
jq -s '
  group_by(.task)
  | map({
      task:       .[0].task,
      runs:       length,
      avg_tokens: (map(.total) | add / length | floor),
      avg_turns:  (map(.turns) | add / length | floor),
      p95_tokens: (sort_by(.total)[(length * 0.95 | floor)].total),
      cache_pct:  ((map(.cache_read)|add) / (map(.input)|add) * 100 | floor)
    })
' experiment/*-session.jsonl
```

## Step 3: Tool breakdown by approach

For each approach (manual, recipe), combine all tool logs and show:
```bash
cat experiment/<approach>-*-tools.jsonl | jq -s '
  group_by(.tool)
  | map({tool: .[0].tool, calls: length})
  | sort_by(-.calls)
'
```

## Step 4: Patch size comparison

For each `experiment/*.patch`:
- Total lines
- Lines added (`grep '^+[^+]' | wc -l`)
- Lines removed (`grep '^-[^-]' | wc -l`)
- Files changed

## Step 5: Repeated reads (efficiency signal)

For each approach, check for files read more than once within a session:
```bash
cat experiment/<approach>-*-tools.jsonl | jq -s '
  group_by(.session)
  | map({
      session: .[0].session,
      repeated: (
        map(select(.tool == "Read"))
        | group_by(.detail)
        | map(select(length > 1) | {file: .[0].detail, count: length})
      )
    })
  | map(select(.repeated | length > 0))
'
```

## Step 6: Write summary

Present a markdown comparison table:

| Metric | Manual (avg) | Recipe-Assisted (avg) | Delta |
|---|---|---|---|
| Total tokens | ... | ... | ...% |
| Turns | ... | ... | ...% |
| Tool calls | ... | ... | ...% |
| Edit calls | ... | ... | ...% |
| Cache hit % | ... | ... | ...pp |
| Patch lines added | ... | ... | ... |
| Repeated reads | ... | ... | ... |

Conclude with a recommendation on whether recipe-assisted fixing is measurably cheaper.

Write the full analysis to `experiment/analysis.md`.
