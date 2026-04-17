# Experiment analysis plan

How to produce a comprehensive report from an experiment run. Used for `manual-N` and `recipe-N` runs alike. The goal is apples-to-apples comparisons across runs and approaches.

## Inputs

After `scripts/run-experiment.sh <approach> <n>` finishes:

- `experiment/<approach>-<n>.patch` — full git diff from main to branch tip
- `experiment/<approach>-<n>-tools.jsonl` — per-tool-call log (may span multiple sessions if the run was resumed)
- `experiment/<approach>-<n>-session.jsonl` — per-session-stop log
- `experiment/<approach>-<n>-usage.json` — token summary (already filtered by task label)
- Git branch `experiment/<approach>-<n>` with the commits
- Transcripts in `~/.claude/projects/<slug>/<session-id>.jsonl`

## Output

`experiment/<approach>-<n>-analysis.md` — a report with every section below. Save it; commit on the experiment branch.

## Hygiene: filter out unrelated sessions

Any tool log in `.claude/tool-log.jsonl` during the run may include unrelated Claude Code sessions (interactive dev, setup agents, hooks). Always filter by task label:

```bash
jq -c 'select(.task | startswith("<approach>-fix-<n>"))' \
  experiment/<approach>-<n>-tools.jsonl > /tmp/clean-tools.jsonl
```

All analysis below uses the filtered file.

## Sections to produce

### 1. Top-line summary

A single table:

| Metric | Value |
|---|---|
| Approach | manual \| recipe |
| Run | 1 \| 2 \| 3 |
| Sessions | count |
| Turns | from transcripts (assistant messages) |
| Total tokens | input + cache_creation + cache_read + output |
| Billed tokens | input + cache_creation + output (cache_read discounted) |
| Output tokens | |
| Cache hit % | cache_read / total |
| Tool calls | wc -l on filtered log |
| Commits | `git log --oneline main..HEAD \| wc -l` |
| Files changed | from `git diff --stat main..HEAD` tail |
| Lines added / removed | same |
| Wall clock | first-to-last tool-log timestamp |

### 2. Tool breakdown

```bash
jq -r '.tool' /tmp/clean-tools.jsonl | sort | uniq -c | sort -rn
```

And a derived ratio: `Edit + Write` vs `Read + Grep + Glob` — high ratio = less thrashing.

### 3. Agent usage

For each Agent call:
- Description (from `.detail`)
- Token cost and duration (from `<usage>` XML blocks in the transcript)
- Files touched in the resulting commit

Summary: number of agents, median / p95 tokens per agent, files-per-agent.

If an experiment was prompted to delegate one agent per file, measure how often it complied: `(agent_count / files_with_findings)` — 1.0 = full compliance.

### 4. Commit pattern

- Commits per heuristic code: parse commit messages `fix: <File> (<codes>)`
- Files touched by multiple commits — flag as potential rework
- Any `revert`, `fix test`, or `broken` in commit messages → quality signal
- Agent-to-commit ratio — agent output typically feeds exactly one commit

### 5. Finding delta

Before: `experiment/baseline/*.json` (captured at branch creation)
After: `<module>/build/reports/clean-code/findings.json` after the run

Delta by code and module:

```python
# pseudocode
for mod in modules:
    before = counts(baseline[mod])
    after  = counts(post_run[mod])
    delta  = before - after  # negative = regression
```

Produce a table with columns: code, before, after, delta, % change. Sort by absolute delta.

### 6. Efficiency signals

**Repeated reads** (same file read more than twice — context-building vs re-reading):
```bash
jq -r 'select(.tool == "Read") | .detail' /tmp/clean-tools.jsonl \
  | sort | uniq -c | sort -rn | awk '$1 > 2'
```

**Repeated Bash invocations** of the same command — often `./gradlew test` reruns. Break down by first word of the command.

**Long tool gaps**: diff consecutive timestamps; anything > 2 minutes usually means a gradle run.

### 7. Quality review (the hard part)

Random sample of 5 commits. For each:
- Open the diff
- Assess against the anti-goal: did the change make the code clearer, or did it squeeze under a threshold at a readability cost?
- Check for these red flags:
  - Methods collapsed onto one line to satisfy line-count
  - Blank lines removed between logically distinct operations
  - Renames that are mechanical ("fetch" → "get" when both were fine)
  - Comments deleted that explained non-obvious *why*
  - Dead-code deletions that removed active functionality
- Also check for these positive signals:
  - Genuine extractions (`extractFoo` with a meaningful name)
  - Suppressions with clear `reason=` when a fix would have hurt
  - Structural improvements (e.g. G1 classpath-resource extraction)

Per-commit verdict: **Clear improvement / Neutral / Readability regression**. Count them.

### 8. Test outcomes

```bash
# the experiment prompt asks Claude to run :<module>:test after batches
jq -r 'select(.tool == "Bash") | .detail' /tmp/clean-tools.jsonl \
  | grep -c "gradlew.*:.*test"
```

From the last `./gradlew test` in the tool log, capture stdout (via transcript) — all modules green?

Commits after a test failure should be investigated for whether the fix restored green.

### 9. Branch-level comparison (when multiple runs exist)

Once both `experiment/manual-*` and `experiment/recipe-*` exist, summary table:

| Metric | manual-1 | recipe-1 | Delta (recipe / manual) |
|---|---:|---:|---:|
| Billed tokens | | | |
| Turns | | | |
| Output tokens | | | |
| Commits | | | |
| Files changed | | | |
| Findings remaining | | | |
| Quality regressions | | | |

Conclude with a recommendation: is recipe-assisted fixing measurably cheaper / higher quality?

## Repeatability

This plan should stay stable across runs. When the experiment protocol changes (new task label format, new metric, new prompt), update this plan before running — analyses should use a single canonical template.

Generate the analysis by running a script that reads the inputs above and produces the full markdown. A future improvement is to script the whole analysis (`scripts/analyse-experiment.sh manual 1`), but for now a human walks through each section and writes it up.

## Storage

- Plan (this document): lives on `main` under `docs/`.
- Individual run analyses: live on their experiment branch under `experiment/<approach>-<n>-analysis.md`, so each branch is self-contained and reviewable.
- Cross-run comparison: once multiple runs exist, write `experiment/analysis.md` on main with the comparison table (already hinted at by the `/experiment-analyse` skill).
