# 2026-04-19 — Four-way rework comparison, analysis

Paired run against a 10-file sandbox batch, all four variants started from the
same commit (`d27e71c` + the two fixes landed this session: `b6d623d`
findings-measurement + `7d93e26` prompt stylistic-findings unblock). Raw
markdown with per-variant diffs, commit bodies, and full cost/findings tables
is at `2026-04-19-four-way-comparison-raw.md` in this directory.

## The variants

| Variant | Tool surface | Pre-agent pass |
|---|---|---|
| VANILLA | agent only (Read / Edit / Write / Bash) | none |
| MCP_GRADLE_ONLY | + `verify_build`, `run_tests`, `format` MCP tools | none |
| MCP_RECIPES | MCP_GRADLE_ONLY + `extract_method`, `extract_methods` | none |
| HARNESS_RECIPES_THEN_AGENT | same tools as MCP_RECIPES | harness applies 10 deterministic recipes first |

Harness recipe pass recipes: `MakeMethodStaticRecipe`, `CollapseSiblingGuardsRecipe`,
`ChainConsecutiveBuilderCallsRecipe`, `DeleteSectionCommentsRecipe`, `MathMinCapRecipe`,
`ReplaceForAddNCopiesRecipe`, `RestoreInterruptFlagRecipe`, `AddFinalRecipe`,
`InvertNegativeConditionalRecipe`, `ShortenFullyQualifiedReferencesRecipe`.

## Results

### Cost

| | vanilla | mcp gradle only | mcp + recipes | harness + agent |
|---|---:|---:|---:|---:|
| input tokens | 59 | 58 | 58 | 53 |
| cache creation | 118,024 | 116,634 | 113,411 | 114,921 |
| cache read | 3,142,879 | 2,354,121 | 3,116,559 | 2,632,773 |
| total input | 3,260,962 | 2,470,813 | 3,230,028 | 2,747,747 |
| output tokens | 45,724 | 42,720 | 35,754 | 45,322 |
| cache hit rate | 96.4% | 95.3% | 96.5% | 95.8% |
| turns | 50 | 39 | 49 | 39 |
| duration (s) | 588.7 | 527.6 | **435.8** | 535.3 |
| cost (USD) | 3.4558 | **2.9777** | 3.1652 | 3.1716 |
| actions | 10 | 10 | 10 | 17 (incl. 8 recipe-pass pseudo-actions) |
| rejected | 4 | 5 | 2 | **1** |
| cost per action | 0.3456 | 0.2978 | 0.3165 | 0.1866 |

### Findings

| | vanilla | mcp gradle only | mcp + recipes | harness + agent |
|---|---:|---:|---:|---:|
| baseline | 43 | 43 | 43 | 43 |
| fixed | **30** | 27 | 28 | 27 |
| introduced | 10 | 7 | 9 | 7 |
| final | **23** | **23** | 24 | **23** |

Full run time: 35m 20s wall.

## Hypotheses — all three falsified

Hypotheses were set in `docs/plan-next-session.md` before the run:

1. **HARNESS beats MCP_RECIPES on cost by 20-40%.** **False** — cost is
   identical ($3.17 vs $3.17). The recipe pre-pass did not reduce agent
   input tokens meaningfully.
2. **Quality matches.** **True, loosely** — HARNESS ties VANILLA and
   MCP_GRADLE_ONLY at 23 final; MCP_RECIPES is one worse at 24. The spread
   is a single G19 nested-ternary finding on one file.
3. **Duration drops more than cost.** **False** — HARNESS wall-time
   (535s) was longer than MCP_RECIPES (436s).

## Why the harness variant didn't deliver

**The agent is still reading the original findings list** even though the
harness has already fixed ~20 of them. Commit body from the HARNESS run
gives it away:

> `G31 findings are stale — chained fluent-builder calls were already applied
> by the recipe pre-processing, and Map.Entry is not a fully-qualified type
> reference smell.`

That's the agent spending turns reasoning about work that no longer needs
doing. Every "G18 make-static" finding the recipe pass already fixed still
shows up in the agent's prompt as a finding to evaluate; the agent has to
re-read the file, see the method is already static, and reject the finding
as "already done." Even rejections cost tokens and turns.

**Cost-per-agent-action (excluding the 8 cheap recipe-pass pseudo-actions)**
is 0.35 — identical to VANILLA. The 0.19 cost-per-action in the table is
misleading because the harness-pass actions are free.

## Other observations

- **MCP_GRADLE_ONLY is the cheapest variant**, not by a lot ($2.98 vs
  $3.17 for MCP_RECIPES) but consistently. Wrapping Gradle invocations as
  MCP tools saves real tokens vs shelling `./gradlew` via Bash because the
  daemon connection is reused across calls — the agent never pays per-call
  wrapper JVM startup.
- **VANILLA fixed the most findings (30) but also introduced the most (10)**.
  The big-move edits (new `Channel` enum, `System.Logger` migration, chained
  StringBuilder threading through ReportTemplate) land more value and more
  regressions in equal measure. Net final count ties the other variants.
- **MCP_RECIPES had the lowest wall-clock** (7m16s) and the lowest rejected
  count (2). The agent's `extract_method` call on `CsvParser.parseRow` was
  correctly rejected by the recipe (multiple output locals across phases);
  the agent did not spelunk, fell back to a pointed Edit, moved on. The
  prompt fix from earlier today (stylistic findings don't block extraction)
  is visible in this variant's willingness to call the tool.
- **False-positive recognition is consistent across variants.** All four
  correctly rejected G18 findings on methods that read instance state
  (`parseRow`, `execute`, `userIdFor`, etc.) with the same reasoning. This
  is table-stakes, not differentiation.

## Next: fix the stale-findings bug in HARNESS

The obvious next experiment is: after `HarnessRecipePass` runs, re-analyse
the sandbox and rebuild the agent's findings list from the post-recipe
state. The agent should only see findings that still exist after the
deterministic pass. Prediction: rejected count drops to 0 and cost drops
meaningfully (fewer turns spent reasoning about stale findings).

Implementation: thread a `SandboxAnalysis`-backed re-analyser callback into
`ReworkOrchestrator.reworkClasses`; invoke it for the HARNESS variant only,
between `HarnessRecipePass.apply` and `PromptBuilder.build`. Everywhere
else keeps today's behaviour.

## Data provenance

- Commit at run start: `7d93e26` (fresh from `b6d623d`).
- Plugin + refactoring republished to mavenLocal immediately before the
  run (fixing the stale-jar failure from the 2026-04-19 first attempt).
- Raw markdown: `2026-04-19-four-way-comparison-raw.md` in this directory.
- Run log preserved at `/tmp/rework-compare-latest.log` during session;
  not archived (reconstructable from the markdown).
