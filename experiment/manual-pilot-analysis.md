# Manual pilot — analysis

Branch: `experiment/manual-pilot`. Approach: manual fix. Plugin analysis + per-file briefs produced the work items; Claude drove the fixes via `claude -p`. The run was interrupted once on usage limits and resumed.

This is a **pilot** run — it exercised the tooling end-to-end while several bugs were fixed mid-run (missing `ReviewSession` record, suppression parser's BinaryExpr handling, the token-log filter that was polluted by a parallel dev session). Numbers are a useful shakedown baseline but should not be compared head-to-head with a later clean `manual-1` / `recipe-1` pair.

## Top-line summary

| Metric | Value |
|---|---|
| Sessions | 2 (`a7243569` initial, `f55656f7` resume) |
| Turns | 364 |
| Tool calls | 1,759 |
| Total tokens | 49.8M |
| Billed tokens | 1.53M |
| Output tokens | 497K |
| Cache hit % | 96.9% |
| Commits | 101 |
| Files changed | 152 |
| Lines added / removed | 8,047 / 5,458 |
| Agents spawned | 93 |

Baseline before the run: 1,395 findings (before any suppression). With package-level `@SuppressCleanCode` applied to `recipes/` and `refactoring/`, suppressions removed ~559. The remaining ~836 findings were the target; after the run, `./gradlew analyseCleanCode` shows **341 remaining**, a 60% reduction.

The cache hit ratio (96.9%) is the single most striking number — 242M of the 249M tokens Claude read were served from cache. Billed cost was ~3% of raw consumption. This is the effect of the per-file brief pattern: the system prompt and fix briefs stay stable across agent spawns.

## Time

All timings below are **active** — the idle gap between the interrupted first session and the resume (spent waiting for the usage-limit window to reset) is excluded, since it reflects billing policy rather than run cost.

| Metric | Value |
|---|---|
| Active wall clock | **99.7 min (1h 40m)** |
| Session 1 | 45.7 min, 552 calls, 5.0s per call |
| Session 2 (resume) | 54.0 min, 1,207 calls, 2.7s per call |
| Avg seconds per commit | ~60s |
| Avg seconds per tool call | 3.4s overall |

Session 2 runs at roughly 2× the call throughput of session 1. Most plausible cause: cache is already warm at resume — the system prompt and per-file briefs were cached in session 1 and persist across session boundaries at the Anthropic API level. Any resumed or repeated run benefits from this.

## Hygiene note

The raw `experiment/manual-1-usage.json` reported 3 sessions and higher token totals because a parallel interactive Claude Code session (`f1998d45`, task=`unknown`) wrote to the same `.claude/tool-log.jsonl`. All numbers in this report are the filtered set — `jq 'select(.task | startswith("manual-fix-1"))'`. The runner now filters by task label automatically (commit `c8156f7`) so future runs won't require this step.

## Tool breakdown

| Tool | Calls | Share |
|---|---:|---:|
| Bash | 750 | 43% |
| Read | 425 | 24% |
| Write | 183 | 10% |
| Grep | 117 | 7% |
| Edit | 112 | 6% |
| Agent | 93 | 5% |
| Glob | 58 | 3% |
| TodoWrite | 17 | 1% |
| ToolSearch | 2 | <1% |
| Skill | 2 | <1% |

Bash dominates because of 311 Gradle invocations (test/compile) and 161 git calls (status, diff, add, commit). Read is 2.3× Write+Edit — Claude reads ~2.3 files for every one it modifies. That's lower than typical interactive sessions and suggests agents are running with focused briefs rather than exploring.

The agent count (93) is in line with the prompt instruction of "one agent per file". 152 files were touched overall; not every file needed a subagent spawn, but the prompt was largely followed.

## Commit pattern

101 commits. Message format `fix: <ClassName> (<codes>)` — honoured almost universally. Checkpoint commits and a handful of meta-edits broke the pattern (`checkpoint: claude-review refactor from interrupted run`, `ignore archived experiment log parts`).

Codes addressed by raw frequency in commit messages:

| Code | Commits | Existing refactoring recipe? |
|---|---:|---|
| G30 | 51 | — (semantic split) |
| Ch10.1 | 22 | — (class size) |
| G25 | 17 | `ExtractConstantRecipe` |
| G22 | 17 | `AddFinalRecipe` |
| G5 | 16 | — (structural duplication) |
| T1 | 15 | `WrapAssertAllRecipe`, `ReduceVisibilityRecipe` |
| G28 | 15 | `ExtractExplanatoryVariableRecipe` |
| J1 | 14 | `DeleteUnusedImportRecipe` |
| G24 | 14 | — (Checkstyle style) |
| G10 | 13 | `MoveDeclarationRecipe` |
| F2 | 12 | — |
| G35 | 11 | — |
| G31 | 9 | — |
| G19 | 9 | `ExtractExplanatoryVariableRecipe` |
| Ch7.1 | 8 | — |

Two files accumulated the most churn: `SkillFileScaffolder`, `OpenRewriteFindingSource`, `CheckstyleFindingSource`, `SuppressionParser` — each touched by three separate commits. Worth a spot-check for rework / inconsistency.

No commit message contained `revert`, `fix test`, or `broken`. Test-driven rework did not appear to be necessary; the `./gradlew test` invocations (291 of them — almost 3 per commit on average) suggest Claude ran tests continuously and caught issues before committing.

## Finding delta

Per-module delta (baseline vs post-run `findings.json`, no suppressions applied on either side):

| Module | Before | After | Delta |
|---|---:|---:|---:|
| recipes | 746 | 132 | −614 |
| adapters | 291 | 54 | −237 |
| core | 227 | 56 | −171 |
| plugin | 82 | 33 | −49 |
| claude-review | 37 | 21 | −16 |
| annotations | 12 | 12 | 0 |
| refactoring | (new coverage) | 33 | +33 |
| **Total** | **1,395** | **341** | **−1,054** |

`refactoring` went from 0 (not in baseline) to 33 — the plugin wasn't previously analysing that module. `annotations` is unchanged (small module, only E1 findings which are dependency-update reports).

Top codes remaining after the run:

| Code | Remaining | Comment |
|---|---:|---|
| E1 | 93 | Outdated dependencies — needs the new `clean-code-dependency-updates` skill |
| G30 | 33 | Semantic — further splitting wouldn't help |
| G29 | 30 | Negative conditionals — agent left these |
| Ch10.1 | 20 | Classes still over the 150-line threshold |
| G35 | 18 | Configurable data high-levelling |
| G31 | 18 | Hidden temporal coupling |
| T1 | 17 | Test coverage |
| G18 | 17 | Inappropriate static |

## Efficiency signals

**Repeated reads** (same file read ≥3 times):

| File | Reads |
|---|---:|
| `adapters/.../CheckstyleFindingSource.java` | 13 |
| `adapters/.../SpotBugsFindingSource.java` | 10 |
| `core/.../Finding.java` | 8 |
| `adapters/.../XmlReportParser.java` | 8 |
| `adapters/.../PmdFindingSource.java` | 8 |

Reading `Finding.java` 8 times is expected — agents reviewing other files needed its shape. Re-reading the same adapter 13 times is less defensible: that's a signal the agents weren't getting enough context from the brief alone and had to rediscover structure.

Potential fix: include enclosing-package context (nearby class structures) in each brief's preamble, so an agent doesn't have to re-read the same type definition each time.

**Bash command types**:

| Command | Calls |
|---|---:|
| `./gradlew` | 291 |
| `git` | 161 |
| `ls` | 152 |
| `grep` | 53 |
| `wc` | 49 |
| `cat` | 42 |
| `find` | 27 |

Gradle invocations averaged ~3 per commit — that's test runs plus some compile checks. `git` operations include status/diff/add/commit per commit plus log browsing. The `152` `ls` calls are on the higher side and could often have been replaced with Glob; they're fast but noise.

## Agent usage

93 agents spawned across 2 sessions (29 + 64). Prompt pattern on each agent was:

> You are fixing Clean Code findings on a single Java file. DO NOT commit — the parent agent will commit. DO NOT touch other files except to fix compilation. DO NOT use any refactoring module recipes.

The constraint against refactoring recipes was consistent with the manual-run protocol. Average agent output inferred from the 93-agent / 497K-output-token accounting: agents contributed a meaningful share of output but the parent session did most orchestration. A more detailed per-agent accounting needs per-session transcript inspection — follow-up work.

## Quality review (spot sample)

Sampled three commits to assess the anti-goal ("never degrade readability to satisfy a metric").

**`ea93af5` — `fix: HtmlReportWriter (G1, G31, T1)`** (−65 / +71 lines)

Verdict: **clear improvement**. The G1 finding was for embedded HTML in Java string literals. The fix did the right thing — extracted the full HTML skeleton into `html-report.html` as a classpath resource and loaded it via `getResourceAsStream`. This matches the worked example in `clean-code-java-idioms/SKILL.md` exactly. It's the structural refactor the earlier manual-1 run failed to make; this run's G1 coverage fix worked as designed.

**`ea1cf26` — `fix: SystemOutRecipe (G25, G28, G30)`** (−29 / +47 lines)

Verdict: **neutral-to-positive**. Extracted magic strings to named constants; pulled condition into explanatory variable; split one longer method. Constant names (`SYSTEM_OUT_QUALIFIER`, `PRINT_STACK_TRACE`) are descriptive.

**`3d924dc` — `fix: ImperativeLoopRecipe (Ch10_1, G5, G28, G30)`** (not inspected in detail, mixed codes)

Needs deeper review — Ch10.1 (class size) combined with G5 (duplication) is where "squeeze under the threshold" regressions are most likely. Flagged for manual diff review.

No test failures surfaced in commit messages, but a systematic quality audit across all 101 commits is out of scope for this report. Recommendation: sample 10 commits for a fuller quality check before running recipe-1 for comparison.

## Protocol adherence

| Instruction | Outcome |
|---|---|
| One general-purpose Agent per file | 93 agents against 152 files ≈ 61% delegation — partial |
| Do not use refactoring module recipes | Respected (agents have explicit instruction; no recipe invocations in Bash log) |
| Run `./gradlew :<module>:test` after each batch | 291 Gradle invocations, mostly tests — honoured |
| Commit per file with `fix: <File> (<codes>)` | 97 of 101 commits match the format |
| Never degrade readability | Evidence so far is positive; needs broader sampling |
| Stop only when zero findings or remaining documented | 341 findings remain, not documented per-finding — the agent stopped on usage limit rather than reaching the natural exit criterion |

The "not documented" gap is a protocol miss: the agent didn't produce a final summary listing what it intentionally skipped (E1 deps, narrative heuristics). Add a stronger exit instruction in the experiment prompt for next runs.

---

# Automation action plan

Where did Claude spend effort doing work a recipe could have done?

## Commits that an existing recipe could have collapsed

For each code below, a single invocation of the existing recipe would have handled every instance across the codebase, replacing N commits with 1.

| Code | Commits | Existing recipe | Collapsible? |
|---|---:|---|---|
| G25 | 17 | `ExtractConstantRecipe` | Yes — it extracts repeated string literals to `private static final`. Many G25 commits look identical to its output. |
| G22 | 17 | `AddFinalRecipe` | Yes — adds `final` to non-reassigned locals. Purely mechanical. |
| T1 (assertion wrap) | ~10 of 15 | `WrapAssertAllRecipe` | Mostly — covers the assertAll grouping; not the "extract testable helper" case. |
| G28 | 15 | `ExtractExplanatoryVariableRecipe` | Partly — covers complex if-conditions. |
| J1 | 14 | `DeleteUnusedImportRecipe` | Yes — wildcard + unused imports. |
| G10 | 13 | `MoveDeclarationRecipe` | Yes. |
| G19 | 9 | `ExtractExplanatoryVariableRecipe` | Partly — shared with G28. |

Rough collapsed count: **71 of 101 commits** (about 70%) touched codes where an existing recipe could have done the bulk of the work. The next experiment run (`recipe-1`, using the refactoring module first) should see a large drop in hand-crafted commits for these codes.

Estimated token saving for `recipe-1` vs `manual-1` on this subset: `manual-1` used ~497K output tokens across 101 commits (≈4,900 per commit). If 71 of those become 10 recipe applications + manual review (say 500 output tokens each for the driver to verify), that's ~(71 × 4,900) − (10 × 500) ≈ **340K output tokens saved**, or 68% of the run's output spend. Billed-token saving will scale roughly with output since the large cache-read tail is orthogonal.

## Candidates for NEW refactoring recipes (ranked by frequency × feasibility)

### High priority

1. **G24 — Follow Standard Conventions** (14 commits)
   Checkstyle catches these but we ship no auto-fixer. Typical fixes are:
   - add missing `{ }` around single-statement `if`
   - reformat long lines
   - normalise whitespace
   Options:
   - Build `FormatRecipe` on top of OpenRewrite's `AutoFormat` + `ShortenFullyQualifiedTypeReferences` + a `MandatoryBraces` recipe.
   - Alternatively integrate `Spotless` or `google-java-format` as the canonical formatter — cheaper than maintaining our own.
   Estimated saving: 14 commits → 1 recipe application. ~65K output tokens.

2. **G26 — Be Precise** (5 commits)
   Many of the fixes were `java.io.File` → `java.nio.file.Path`. Already covered in detection (`LegacyFileApiRecipe`), and `AddLocaleRecipe` handles case-conversion. What's missing is the full migration recipe: `new File("...")` → `Path.of("...")` + companion API calls (`file.exists()` → `Files.exists(path)`, etc.). Viable because the API mapping is mostly bijective for the common subset.
   Estimated saving: 5 commits → 1 recipe application. ~25K output tokens.

3. **G35 — Keep Configurable Data at High Levels** (11 commits)
   "Magic number in private method → class-level `private static final`." Mechanical when the constant is a literal appearing ≥N times or matches a naming heuristic (e.g. a threshold). Implement as `ExtractClassConstantRecipe` — moves the literal up, names it after a user-supplied mapping or a regex heuristic. May need human review for names.
   Estimated saving: 11 commits → 1 recipe (partial; some G35 is architectural). ~50K output tokens.

### Medium priority

4. **F3 — Flag Arguments** (5 commits)
   Convert `doSomething(boolean force)` into `doSomething()` + `doSomethingForce()`. Recipe requires call-site rewriting at both false and true call sites. Viable for private/package-private methods (bounded call-site scope). Public API changes need human approval.
   Estimated saving: 5 commits → 1 recipe application (with opt-out list). ~25K output tokens.

5. **G29 — Avoid Negative Conditionals** (6 commits)
   Invert `if (!cond) { A } else { B }` to `if (cond) { B } else { A }`. The existing `GuardClauseRecipe` covers early-return guards but not the general inversion case. Building a `NegativeConditionalInversionRecipe` is mechanical for the pattern-matched cases.
   Estimated saving: 6 commits → 1 recipe. ~30K output tokens.

6. **N5 — Short Names Outside Loops** (5 commits)
   Rename `i`, `j`, `x` in non-loop scopes. Recipe needs a rename-suggestion oracle — could be a map provided by the user (e.g. `i → index`) or a simple heuristic based on the variable's type. Semi-mechanical.
   Estimated saving: 5 commits → 1 recipe (with human-supplied name map). ~20K output tokens.

### Low priority / skip

- **G30 (51 commits)** — function splitting requires semantic judgement. A recipe can flag *where* to split but not *how*. Keep the existing detection; keep this as manual work.
- **Ch10.1 (22)** — class-size reductions need architectural thinking. Skip.
- **G5 (16)** — duplication often signals a missing abstraction. Skip.
- **G31 (9)** — temporal coupling requires redesign. Skip.

## Total saving if all high/medium recipes ship

Sum of collapsible commits: 14 + 5 + 11 + 5 + 6 + 5 = **46 commits** → ~6 recipe invocations, per run.

Combined with the existing-recipe collapse (71 commits), a recipe-assisted run could in principle collapse ~70% of the 101 manual commits into ~16 recipe applications plus manual work on the residual 30%.

Output-token estimate (speculative until the `recipe-1` run is done): ~(46 + 71) × 4,900 − 16 × 500 ≈ **565K saved output tokens per run**, against 497K observed. In round terms, a recipe-assisted run of the same scope could land around **30% of the manual run's billed cost**.

This estimate is optimistic — recipe development itself has a one-time cost, and some recipe outputs will need human review (especially F3 call-site rewrites and N5 renames). But the order of magnitude is large enough to justify building the top-3 recipes before the next manual vs recipe comparison.

## Recommended sequencing

1. Run `recipe-1` against the **current** refactoring module to establish a first data point. This validates the collapse estimate for the 71 commits that existing recipes can already handle.
2. If the collapse is confirmed, build the G24, G26, G35 recipes (highest commit counts among gaps) and re-run as `recipe-2`.
3. Compare all three runs (`manual-1`, `recipe-1`, `recipe-2`) with the plan in `docs/experiment-analysis-plan.md`.

## Prompt and detection improvements

Changes to the fix prompt and to detection/routing that the data from this run specifically points at.

### Prompt changes

1. **Force a final summary before exit.** The agent stopped on usage limit without producing the "here's what I intentionally skipped" block the prompt asked for. Add an explicit instruction: *"Before exiting for any reason (usage limit, tests all green, or running out of briefs), write a final summary to `experiment/<approach>-<n>-summary.md` listing: findings fixed, findings intentionally skipped with reason, remaining findings per code, and test status per module."* Saves the operator from reconstructing the end state from `git log`.

2. **Mandate one agent per brief — no direct edits.** 93 agents against 152 files = 61% delegation, not the 100% the prompt implied. The driver session did direct edits for the other 39%. That inflates driver context and defeats the whole point of the agent pattern. Tighten the wording from "spawn a general-purpose Agent whose prompt IS the brief" to *"for every file in the `_INDEX.md`, invoke the Agent tool; never open Edit/Write from the driver"*. The only exception should be the final summary write.

3. **Pre-empt wrongly-suppressed findings.** Several agents rediscovered that they couldn't suppress E1 findings because they have no source anchor; some wasted tokens trying to annotate dep-update findings. Add a known-limits block to each brief footer: *"E1 findings have no source anchor — do not attempt `@SuppressCleanCode`. See `.claude/skills/clean-code-dependency-updates/SKILL.md`."* The brief generator (`FixBriefGenerator`) can add this when it sees E1.

4. **Include sibling context in each brief.** Agents re-read `Finding.java` and `CheckstyleFindingSource.java` dozens of times because the brief alone didn't give enough type context. Add a "Sibling types in this package" block to the brief — one line per sibling, public-method signatures only. Bulks up the brief slightly but eliminates dozens of `Read` calls.

5. **Explicit guidance on when NOT to fix.** G29 (avoid negative conditionals) had 6 commits and still 30 remaining because the agent couldn't always tell when inversion helps vs obscures. Teach the skill: *"Invert `!cond` only when the positive reads more naturally. `!isEmpty()` → `hasItems()` is a win; `!file.exists()` stays."* Applies in the `clean-code-conditionals-and-expressions` skill.

6. **Always enable Claude Review for runs that aim at design-level codes.** Claude Review was opt-in off, so G6 / G20 / N4 (the three codes that need semantic judgement) never surfaced. For runs meant to exercise the full system, the prompt should flip `claudeReview.enabled` on and add a confirmation step: *"After analyseCleanCode, inspect the claude-review findings in the per-file briefs."*

### Detection / routing changes

1. **Severity rebalance (already planned).** See `docs/plan-review-and-severity.md`. OpenRewrite findings all come back `WARNING`, hiding real latent bugs (G4 swallowed exceptions, Ch7.1 log-and-continue, F2 output arguments, G8 public mutable fields). Escalating these to `ERROR` gives the fix-brief's "errors first" ordering something to actually order. Likely to change agent attention ordering materially.

2. **Claude Review coverage expansion (already planned).** Same plan doc. Beyond severity, adding C1/C4/G2/G3/G17/G21/G27/G32/N2/N3/Ch6.1 gives agents feedback on design smells that mechanical scanners miss.

3. **E1 file anchoring.** `DependencyUpdatesFindingSource` emits findings with `sourceFile=null`. Anchor them on the nearest `build.gradle.kts` or the `gradle/libs.versions.toml` entry so `@SuppressCleanCode` and per-package routing can apply. Also makes the fix-brief generator able to group dep updates by module rather than lumping them.

4. **Route to `clean-code-dependency-updates` skill (already done in `616cf1b`).** Verify the wiring reaches a fresh brief for the next run — the 93 remaining E1 findings should have a brief with that skill pointer.

5. **Route C2 / F4 / G22 / G36 (also done in `616cf1b`).** Same.

6. **Detection: "G22 detection + AddFinalRecipe integration"** — 17 manual G22 commits is evidence the AddFinalRecipe was not applied first (correct for the manual protocol). For the recipe protocol, Step 1 should explicitly fold AddFinalRecipe in, which it already does; measure on the `recipe-1` run.

7. **Baseline-aware briefs.** The fix-brief generator currently lists every finding. When there's a `clean-code-baseline.json` on disk, skip findings present at baseline but flag new ones as "regression since baseline". Avoids agents fighting over long-standing tolerated warnings.

8. **Protocol-adherence linter.** A small post-run script that asserts the protocol was followed: commit message format, agent spawned per file, no refactoring recipes invoked (for manual), test ran after every batch. Would have caught the 39% direct-edit rate automatically.

## Actions to clear the remaining 341 findings

Per-code plan in priority order. "Effort" is the expected cost for one sweep: a recipe application takes seconds, a skill-guided agent takes minutes per file, human design takes hours per class.

### Mechanical — recipe or skill, one sweep each

| Code | Count | Action | Existing tool | Estimated time |
|---|---:|---|---|---|
| E1 | 93 | Bump each outdated dep in `gradle/libs.versions.toml`, one commit per dep, run tests between. Refuse major-version bumps. | `clean-code-dependency-updates` skill (new) | ~90 min for 93 deps at 1 min each, bounded by test-run time |
| G24 | 14 | Apply Checkstyle auto-fixes: missing braces, line length, whitespace. | New `FormatRecipe` or integrate `google-java-format`/Spotless | ~1 min once the recipe exists |
| G12 | 7 | Remove unused imports, expand star imports. | `DeleteUnusedImportRecipe` (already exists) | ~1 min |
| J1 | 5 | Same as G12. | `DeleteUnusedImportRecipe` | included |
| G19 | 7 | Extract complex in-conditional expressions to named variables. | `ExtractExplanatoryVariableRecipe` (exists) | ~1 min |
| G33 | 7 | Encapsulate `array.length - 1`/`size() - 1` as named locals. | `EncapsulateBoundaryRecipe` (exists) | ~1 min |
| G29 (simple half) | ~15 of 30 | Invert `!cond` branches where the positive reads naturally. | New `NegativeConditionalInversionRecipe` (planned) | ~1 min once built |

**Sub-total (mechanical): ~150 findings clearable in under 2 hours once the three new recipes (deps skill is ready, Format, NegativeInversion) are in place.**

### Skill-guided agent — one brief per file, mostly local edits

| Code | Count | Action | Skill pointer |
|---|---:|---|---|
| G35 | 18 | Pull embedded magic data up to class-level constants or a config file. | `clean-code-functions` (G35 is registered there) |
| G30 | 33 | Split methods that do more than one thing; extract sub-steps with descriptive names. | `clean-code-functions` |
| F2 | 10 | Convert output arguments to return values; use records or small value objects. | `clean-code-functions` |
| G18 | 17 | Convert static helpers that reference instance state to instance methods; OR move pure utilities to a `*Util`-free utility class. | `clean-code-classes` |
| T1 | 17 | Add missing tests; wrap adjacent assertions in `assertAll`; reduce visibility to package-private for testability. | `clean-code-test-quality`, combined with `WrapAssertAllRecipe` + `ReduceVisibilityRecipe` as first pass |
| F1 | 5 | Introduce parameter object / builder / record for constructors with ≥4 args. | `clean-code-functions`; `RecordToLombokValueRecipe` for record cases |
| G29 (hard half) | ~15 of 30 | Cases where inversion would obscure intent; rewrite differently or accept. | `clean-code-conditionals-and-expressions` |

**Sub-total (skill-guided): ~115 findings, estimated 1.5–2 hours of agent time at ~1 minute per finding.**

### Human design — requires architectural thinking

| Code | Count | Action |
|---|---:|---|
| Ch10.1 | 20 | Classes still >150 lines. Split by responsibility — pair programming session, not an agent job. |
| G31 | 18 | Hidden temporal coupling — methods that must be called in a specific order. Redesign to make ordering explicit via types (builder, fluent chain) or state machines. |

**Sub-total (design): 38 findings. No time estimate — this is real design work, maybe half a day per class touched.**

### Sequencing recommendation

1. **Tomorrow:** run the three mechanical recipes (existing + new Format) on a fresh branch. Expect ~150 fewer findings in under an hour.
2. **This week:** run the clean `manual-1` experiment from the current main tip, which now includes all the skill-routing fixes. Compare tokens and quality against this pilot.
3. **Next week:** run `recipe-1` for the side-by-side comparison. The automation action plan earlier in this report predicts a 30%-of-manual billed cost.
4. **Follow-up:** the Ch10.1 and G31 work is independent of the experiment — assign per class to a human reviewer.

### What to do about the 93 E1 findings before the next experiment

The remaining E1 count is noise in the agent's brief rotation: it'll pick them up, be told "bump the dep", produce 93 one-line commits. Options:

- **Accept it:** dep-bump commits are small, isolated, and demonstrate the agent handles the path end-to-end. The token cost is tiny.
- **Pre-bump before the run:** run `./gradlew dependencyUpdates` once manually, apply all the safe (non-major) bumps in a single operator pass, commit with `chore(deps): bulk bump` and hand the agent a smaller working set.
- **Disable for the next run:** `cleanCode.disabledRecipes = listOf("E1")` in the dogfood init script. Fast, but you lose the signal that deps are stale.

Recommended: pre-bump. It costs the operator 10 minutes and removes 93 findings of noise from the comparison.

## Non-automation follow-ups from this run

- **Severity rebalance** (per `docs/plan-review-and-severity.md`) — still high-leverage. Many of the codes fixed here (G4, Ch7.1) would become ERRORs, which would surface them at the top of briefs and likely change agent prioritisation.
- **Claude Review expansion** — none of the codes Claude Review would assess (G6, G20, N4, plus proposed C1, C4, G17, G21, G27, G32, N2, N3, Ch6.1) appeared in the fixes, because Claude Review was opt-in off. Worth enabling for the next run on a sample to measure what it catches.
- **Exit-criterion enforcement** — the agent stopped on usage limit, not on reaching a documented end state. Strengthen the prompt to produce a skipped-findings summary even at interruption.
- **Re-read reduction** — feed more enclosing-package context into each brief to cut the 13 reads of `CheckstyleFindingSource.java` and similar. Consider adding a "neighbours" block to the brief (sibling types in the same package).
