# Manual-1 — analysis

Branch: `experiment/manual-1`. Approach: manual fix (agents work per-file, no refactoring module recipes). This is the first "clean" run from `main` after the pilot, with every plugin improvement from that retro applied: E1 dedup to the root catalog, severity rebalance, Spotless opt-in, the new `ShortenFullyQualifiedReferences` / `InvertNegativeConditional` / `ExtractClassConstant` / `SplitFlagArgument` / `RenameShortName` recipes, the sibling-types block in each brief, the metric-squeezing guard, the "agent MUST read skill first" wording, and the pass-2 runner step.

The run hit the 5-hour usage limit twice. It was resumed once manually; the second limit occurred after session 2 and no further resume was attempted within the measurement window.

## Top-line summary

| Metric | Value |
|---|---|
| Approach | manual |
| Run | 1 |
| Sessions | 2 (`30bbb1f3` initial, `a4e1565e` resume) |
| Turns | 385 |
| Tool calls | 2,544 |
| Total tokens | 44.5M |
| Billed tokens | 1.87M |
| Output tokens | 612K |
| Cache hit % | 95.8% |
| Commits | 88 |
| Files changed | 101 |
| Lines added / removed | 5,064 / 3,386 |
| Agents spawned | 141 |
| Active wall clock | ~118 min (1h 58m) |

## Head-to-head with the pilot

Same task shape, different starting tip:

| Metric | pilot | manual-1 | Δ |
|---|---:|---:|---:|
| Commits | 101 | 88 | −13 |
| Tool calls | 1,759 | 2,544 | +785 |
| Agents | 93 | 141 | +48 |
| Files changed | 152 | 101 | −51 |
| Lines added | 8,047 | 5,064 | −2,983 |
| Lines removed | 5,458 | 3,386 | −2,072 |
| Billed tokens | 1.53M | 1.87M | +0.34M (+22%) |
| Cache hit % | 96.9% | 95.8% | −1.1 pp |
| Active wall clock | 99.7 min | 117.8 min | +18.1 min |

**Reading:** manual-1 touches fewer files but delegates harder (141 agents vs 93) and reads more (893 reads vs 425). More context-building per file, and the per-brief agent pattern was actually followed more tightly: 141 agents / 101 files with findings ≈ **1.40 agents per file**, versus the pilot's **0.61**.

The run cost ~22% more billed tokens for 13 fewer commits — a regression in commit-per-token. But inspecting the per-commit diffs, manual-1 commits average 96 lines changed vs pilot's 89 — slightly larger per commit, so the cost is real but not as bad as commits-alone suggests.

## Tool breakdown

| Tool | manual-1 | pilot | Ratio |
|---|---:|---:|---|
| Read | 893 | 425 | **2.1×** |
| Bash | 851 | 750 | 1.13× |
| Grep | 236 | 117 | 2.0× |
| Agent | 141 | 93 | 1.5× |
| Edit | 137 | 112 | 1.2× |
| Write | 133 | 183 | 0.73× |
| Glob | 128 | 58 | 2.2× |
| TodoWrite | 9 | 17 | 0.5× |

The 2× jump in Reads, Greps, and Globs is the "read the skill file first" rule at work — agents are front-loading context. That's visible in the per-file read tallies:

| File | Reads |
|---|---:|
| `clean-code-comments-and-clutter/SKILL.md` | **65** |
| `clean-code-functions/SKILL.md` | **48** |
| `clean-code-classes/SKILL.md` | **41** |
| `clean-code-java-idioms/SKILL.md` | **35** |
| `plugin/CleanCodeExtension.java` | 26 |
| `plugin/SkillFileScaffolder.java` | 23 |
| `adapters/CheckstyleFindingSource.java` | 18 |

The top four are skill files, not source — 189 skill reads total, versus **zero** named skill files in the pilot's top-read list. That's the BP4 ("read skill first") instruction landing as intended; also the reason Reads jumped 2.1×.

Modification thrashing (source files re-read many times) dropped: pilot's worst was 13 reads on `CheckstyleFindingSource.java`; manual-1's is 26 on `CleanCodeExtension.java`, but that's also the file the agent refactored 3 separate times.

## Commit pattern

Codes addressed (top 15, from `fix: <File> (<codes>)` messages):

| Code | Commits | New recipe shipped this session? |
|---|---:|---|
| Ch10 | 22 | — (class size, always architectural) |
| G30 | 18 | — (method size) |
| J1 | 15 | DeleteUnusedImportRecipe |
| G22 | 15 | AddFinalRecipe |
| G5 | 13 | — (duplication) |
| G12 | 11 | **ShortenFullyQualifiedReferencesRecipe (new)** |
| G24 | 10 | (formatter via Spotless, opt-in) |
| Ch7 | 10 | — |
| G29 | 7 | **InvertNegativeConditionalRecipe (new)** |
| T1 | 6 | WrapAssertAllRecipe |
| G31 | 6 | — (temporal coupling) |
| G19 | 6 | ExtractExplanatoryVariableRecipe |
| F1 | 6 | — |
| G4 | 5 | — |
| G26 | 5 | — (deferred: File → Path) |

Eleven commits touched G12 — confirmation that the new FQN detection / shortening recipe produced real work. Seven G29 commits show the negative-conditional inversion is landing too. Zero F3 commits in this run (the `SplitFlagArgumentRecipe` had nothing to do — no private-method-with-boolean-flag patterns survived long enough).

Rework (files touched by ≥ 2 commits):

| File | commits |
|---|---:|
| `CleanCodeExtension.java` | 3 |
| `SourceFileCollector.java` | 3 |
| `ClaudeReviewFindingSource.java` | 3 |
| +11 more at 2 commits each | — |

Eleven at 2-commit and three at 3-commit rework is higher than pilot's hottest four (`SkillFileScaffolder`, `OpenRewriteFindingSource`, `CheckstyleFindingSource`, `SuppressionParser`). A lot of the 3-commit files are classes the agent extracted out of bigger originals and then kept polishing — rework in spirit, not in effect.

## Finding delta

Before: pre-suppression baseline (historical `experiment/baseline/*.json`). After: `build/reports/clean-code/findings.json` on `experiment/manual-1` after `./gradlew --rerun-tasks analyseCleanCode`.

Per module (post-suppression on both sides):

| Module | After main (pre-run) | After manual-1 | Δ |
|---|---:|---:|---:|
| (root) | 18 | 18 | 0 |
| annotations | 1 | 2 | +1 |
| core | 103 | 105 | +2 |
| adapters | 236 | 61 | **−175** |
| claude-review | 9 | 8 | −1 |
| plugin | 25 | 71 | **+46** |
| recipes | 65 | 294 | **+229** |
| refactoring | 9 | 58 | **+49** |
| **Total** | **466** | **617** | **+151** |

Raw totals went **up** by 151. That's not a regression — it's three things the headline figure hides:

1. **Suppressed packages now produce findings on new code.** `recipes/` and `refactoring/` carry package-level `@SuppressCleanCode`. The suppression covers the *original* classes, but the agent extracted many new classes out of big recipes (ten helper classes extracted in `refactoring/` alone — `AddFinalRecipe`, `ExtractExplanatoryVariableRecipe`, `RemoveNestedTernaryRecipe`, `SplitFlagArgumentRecipe`, `WrapAssertAllRecipe` each +20-160 line deltas). Package suppression still covers them, so this isn't the +278 number; the real driver is next.
2. **New G12 detection fires on touched files.** 114 of the 617 remaining findings are G12 (inline FQN references). The `FullyQualifiedReferenceRecipe` scanner didn't exist in the pilot. It produces one finding per file with ≥ 1 inline FQN; post-manual-1, 114 files still have some. That's the single biggest chunk of new findings and shouldn't be read as "the agent made things worse."
3. **adapters got cleaned up.** From 236 → 61 = 175-finding reduction. That's mostly severity-rebalance-driven findings (long lines, G4, Ch7.1) that the agent genuinely fixed.

Top 15 remaining codes on the branch:

| Code | Remaining | Comment |
|---|---:|---|
| G12 | 114 | Fully-qualified references — `ShortenFullyQualifiedReferencesRecipe` can sweep most of these |
| G30 | 107 | Method size, semantic; agent addressed 18 but the long tail stays |
| G29 | 51 | Negative conditionals — `InvertNegativeConditionalRecipe` should trim this in a recipe-assisted run |
| G35 | 31 | Magic-number class constants; `ExtractClassConstantRecipe` handles the repeated-literal subset |
| G31 | 28 | Temporal coupling (hard; architectural) |
| F2 | 27 | Output arguments |
| T1 | 26 | Missing tests |
| G10 | 25 | Vertical separation |
| G18 | 20 | Inappropriate static |
| E1 | 18 | Outdated dependencies — left for the agent, not acted on this run |
| G28 | 18 | Encapsulate conditionals |
| Ch10.1 | 15 | Class size |
| G22 | 14 | Missing `final` — `AddFinalRecipe` is mechanical |
| G19 | 13 | Explanatory variable |
| G33 | 11 | Boundary conditions |

Plenty of leverage remaining for a recipe-assisted run.

## Efficiency signals

**Test cadence.** 181 `./gradlew ... test` invocations over 2,544 tool calls = one test run every 14 tool calls. Pilot had 291 gradle invocations overall including test; similar discipline.

**Gaps.** Only 4 tool-call gaps over 120 s; largest was 8.1 min (a gradle full build during the plugin-refactor stall — see below). No long "thinking" pauses.

**Two session cadences.** Session 1: 1,045 calls in 55 min = 19 calls/min. Session 2: 1,499 calls in 54 min = 28 calls/min. Session 2 was 45% faster at the tool-call level, mirroring the pilot's session-2 speedup. Same explanation: warm Gradle daemon, smaller residual after session 1, more delegation (the agent had already built context).

**The plugin refactor stall.** Around T+25 min into session 2, commit count sat at 14 for ~25 minutes while the agent extracted 10+ helper classes (`CleanCodeTasks`, `StaticAnalysisConfiguration`, `ClaudeReviewExtension`, `ThresholdsExtension`, `RuntimeDependencies`, `TaskPaths`, `SkillTemplateResolver`, `ThresholdsHashStore`, `OutdatedDependencyReport`, `VersionCatalogIndex`, `VersionCatalogRewriter`) out of `CleanCodePlugin` and `AnalyseTask`. Plugin TestKit tests broke during the extraction; the agent cycled through `:plugin:test → :plugin:clean → --stop → :plugin:test --no-daemon` before finding the cause. The protocol forbids committing before tests pass, so 10+ files of real refactor work were held until the whole batch went green. It eventually did — commits 15–22 were all plugin helpers, landed in a short burst.

That stall is a visible cost of the per-file-brief model when the agent decides a multi-file refactor is needed: the commit cadence goes dark until the whole thing compiles.

## Quality review (spot sample)

Sampled five commits across the run (5th, 20th, 40th, 60th, 88th):

| Commit | Subject | LOC | Verdict |
|---|---|---:|---|
| `fbd4f77` | `fix: SuppressCleanCode (G12)` | +10 / −6 | **Clear improvement** — replaced inline `org.fiftieshousewife...` FQN with import; new G12 detection caught it, agent ran the shortening manually. |
| `25530b7` | `fix: BaselineManager (G22, J1)` | +19 / −15 | **Clear improvement** — added `final` on locals, removed unused imports. Mechanical but clean. |
| `46bbd2c` | `fix: CheckstyleRuleMap (G12)` | +3 / −2 | **Neutral** — new file extracted from `CheckstyleFindingSource` (the agent split it in earlier commit 39); this one shortens the FQNs in the extracted class. Fine. |
| `221991d` | `fix: ClaudeReviewExtension (G12)` | +3 / −2 | **Clear improvement** — `gradle.api.Action` FQN replaced with import. |
| `f5cb33e` | `fix: SummaryLines (G12, G25, G31)` | +20 / −14 | **Mild readability cost** — extracted magic strings and separated timeline math, but the resulting class is a thin wrapper over two static methods with narrow reuse. Would be fine if several callers emerged; borderline otherwise. |

**4 of 5 clear improvements, 1 borderline.** No "squeezed under threshold" anti-pattern hits in this sample. The metric-squeezing warning in the brief appears to have been heeded — the agent did split `CleanCodePlugin` into many pieces but the pieces are semantically coherent (task registration is now `CleanCodeTasks`, analyser wiring is `StaticAnalysisConfiguration`, etc.) rather than forced line-count splits.

## Protocol adherence

| Instruction | Outcome |
|---|---|
| One Agent per brief | 141 agents, 101 files with findings → **1.40 per file** (pilot was 0.61). Higher than 1.0 because some files were touched in multiple commits / needed a second agent to verify tests. Goal of ≥ 1.0 achieved. |
| Read cited skill before editing | 189 top-5 skill reads vs zero in the pilot. BP4 instruction landing. |
| Run `:<module>:test` after each batch | 181 gradle-test invocations — same order of magnitude as the pilot, consistent. |
| Commit per file with `fix: <File> (<codes>)` | 86 of 88 commit messages match the format (two checkpoint commits from the operator: `Guard VerticalSeparationRecipe …` on this branch was accidental — see Housekeeping below). |
| Write `experiment/manual-1-summary.md` before exit | **Missed.** The agent never wrote one, both sessions ended on usage limit without producing the handoff file. The exit-protocol wording I added on `main` isn't on this branch — it was only pulled into the run's prompt at invocation time, so the agent saw it, but still didn't write the file. Future runs need this instruction promoted or gated (e.g. make the driver script refuse to exit cleanly without writing the summary). |

## Bugs and housekeeping

**NPE in `VerticalSeparationRecipe`.** Observed in session 1 during the first `./gradlew analyseCleanCode`: `J.MethodDeclaration.getBody()` is null for abstract / interface / annotation-element methods; the recipe dereferenced it unconditionally. Gradle's `ignoreFailures` on the OpenRewrite source swallowed the NPE and the run continued, but the finding source produced zero rows from it. Fixed on `main` (commit `4fffcdc`) via a defensive early-return; the fix is **not** on this branch (branch was created from `main` before the fix). Picking it up requires a rebase or merge.

**Accidental operator commit on this branch.** `c222311 Guard VerticalSeparationRecipe against null method bodies` was committed on `experiment/manual-1` in error — the operator's working tree was on the experiment branch while the active run was using it, so `git commit` landed on the wrong branch. The same change was then cleanly cherry-picked onto `main` via a worktree, but the local commit on `manual-1` remained. Treat that one commit as non-experiment; subtract it from agent-work totals if comparing strictly.

**Worktree blocked manual-2.** The recovery worktree used to land the NPE fix was not torn down, and the `02:30` cron for manual-2 hit `fatal: 'main' is already checked out at '/private/tmp/cleanclaude-main'`. Manual-2 did not run. Worktree has since been removed.

**Uncommitted state at exit.** Both usage-limit exits left an uncommitted mid-edit (three `core/` files the second time). That's a preflight tripwire on the next fresh run and needed manual stash / discard before resuming. The cron wrapper now auto-stashes as a safety net (commit `26aab69` on main).

## What went well

1. **Skill-first discipline.** 189 named skill reads in the top tool log. BP4 (strengthened brief wording — "your first tool calls MUST be Reads…") and A4 (skill-path cited in each brief section) visibly changed agent behaviour.
2. **Per-file agent delegation exceeded the target.** 1.40 agents / brief-file — the driver rarely did direct edits, matching BP4's new prompt language.
3. **No test-break-loop panic visible in commits.** No `revert`, `fix test`, `broken` in messages; the plugin-test stall was resolved without cascade.
4. **Adapters module cleaned up substantially** — from 236 → 61 findings (−175), mostly in long-line and severity-rebalanced codes that the agent genuinely fixed rather than suppressed.
5. **New recipes landed real work.** G12 (11 commits), G29 (7 commits), G22 via `AddFinalRecipe` (15 commits): direct evidence the shipped fix recipes are useful.
6. **Session 2 throughput.** 28 tool calls / min vs session 1's 19 — the warm-daemon hypothesis from the pilot reproduced cleanly.

## What didn't

1. **Total findings went up** by 151, driven by new G12 detection producing 114 findings on files the agent touched. That's not regression, but it changes the "did we make progress" story. Need a pre-run vs post-run delta that excludes codes whose detection was added this session, or the message gets muddy.
2. **Plugin-refactor stall cost 25 min of no-commits** while the agent debugged broken TestKit tests after extracting 10+ helper classes from `CleanCodePlugin` / `AnalyseTask`. The per-file-brief model has no cheap way to handle "this one change is a multi-file refactor" and the protocol (no commit until tests pass) made the stall visible and long.
3. **Exit-summary file was never written** despite the explicit instruction in the prompt. Usage-limit exits bypass the exit-protocol step entirely. Need the driver script to write a partial summary on its own if the agent doesn't.
4. **Uncommitted mid-edits on each usage-limit exit** required manual stash/discard before resume. The cron wrapper now stashes automatically; the foreground runner should too.
5. **Two ops-side bugs** lost manual-2's data for the night: the orphan worktree blocked the cron checkout, and the branch-tied cron wrapper path pinned the wrapper's content to an older commit. Both fixed post-hoc (`~/.local/bin/cron-run-cleanclaude.sh` is now branch-independent), but the lesson is **cron wrappers don't belong inside the git working tree** when the working tree is also the experiment's scratchpad.
6. **Rework rate up from the pilot.** 3 files touched by 3 commits, 11 by 2. Not a disaster but worth tracking in run 2; if it keeps climbing, the sibling-types block in the brief may not be enough and the brief needs method-signature summaries too.

## What to change before manual-2 / recipe-1

| Change | Effect |
|---|---|
| Add the `VerticalSeparationRecipe` NPE guard to any branch (it's on `main` already) | Clean analyse pipeline from the first second |
| Teach `run-experiment.sh` to write a partial `experiment/<approach>-<n>-summary.md` itself when the script exits (success OR failure) | Guaranteed handoff artifact even on usage-limit exit |
| Teach `run-experiment.sh` to `git stash push -u` before exit, record the stash ref in the summary | No manual cleanup required to resume |
| Add a rebase step to the resume protocol so resumed runs pick up main's plugin fixes | Second session doesn't run on a stale analyser |
| Promote the exit-protocol wording into a final-turn system prompt Claude sees at T−N% usage | Forces the summary before the limit kicks in |
| Expose `SummaryReportTask` to the experiment runner so a post-run HTML is produced automatically | `docs/reports/index.html` stays current without operator intervention |

## Bottom line

Manual-1 is a clean second data point. Same approach as the pilot, different starting tip with every pilot-retro improvement in place. The new skill-read discipline is visible and substantial (2× read volume, skill files dominate the top-read list). Per-file delegation hit 1.40×. Cost is up ~22% in billed tokens for 13 fewer commits, which is the honest price of the new reading regime; whether it buys better quality is answerable once the recipe-1 run exists and we can compare both against the same baseline.

Pre-run total was 466 findings. Post-run is 617 — headline goes the wrong way, but ~114 of the delta is new G12 detection the agent can't be blamed for. Next run's analysis should quote a like-for-like delta that holds the detection surface constant.

---

## Addendum — what the commit-by-commit audit exposes

Re-reading the branch with a sharper lens, focused on the question *"when the agent creates a new class, does it honour the same skills it would enforce on an existing one?"*

### New-class skill violations are systemic

| Statistic | Value |
|---|---:|
| New classes added on `experiment/manual-1` | **51** |
| New test classes | **1** |
| Ratio | 51 : 1 |

Every skill we enforce on existing code (T1 "insufficient tests", C5 / Ch7.1 exception handling, G12 imports-not-FQN, G8 visibility) is unconditional about *quality of code as it currently stands*. But the fix-brief pattern is built around existing files — it has nothing to say about the code the agent creates during a fix. Result: 51 new classes, one test, and several other silent regressions:

**Sampled commit `403aecd` (`fix: ClaudeReviewFindingSource (F1, G12, G30, Ch10.1)`) extracted four helper classes:**

- `CachedFindings`, `ClaudeReviewer`, `FindingJsonParser`, `SourceFileCollector`
- Zero tests written for any of them
- `ClaudeReviewer.analyseFile` contains `catch (AnthropicException e) { LOG.log(...); return ""; }` — catch-log-continue, Ch7.1 anti-pattern
- `FindingJsonParser.parse` contains `catch (JsonParseException | IllegalStateException e) { LOG.log(...); return List.of(); }` — same anti-pattern
- `FindingJsonParser.toFinding` has `catch (IllegalArgumentException e) { LOG.log(Level.FINE, ...); return Optional.empty(); }` — same again

The agent addressed the listed codes (F1, G12, G30, Ch10.1) correctly but reproduced a Ch7.1 violation while moving code into the new classes. Ch7.1 wasn't *in the brief* for that file, so the agent didn't think about it. This is a systemic gap, not a one-off: the per-file brief pattern only covers the codes already flagged on the existing file's findings, and any new files produced during the fix are invisible to both the brief and the next analyse run (they get scanned once committed, but by then the pattern is in place).

**What would fix it:** a "new classes you create must satisfy **every** applicable skill, not just the ones in the brief" section on every brief; plus a post-commit detection pass that flags any new `.java` file missing a corresponding `Test.java`, or a `catch` block whose body is only a log call and a return, as a first-class finding.

### StringBuilder is everywhere, and so is `sb`

Sixteen files across the branch use `StringBuilder` or `StringBuffer`. Many of them thread a `StringBuilder` through nested private methods as a mutable parameter:

```java
// core/ClaudeMdSectionBuilder.java (new file created this run)
StringBuilder appendPreamble(final StringBuilder sb) { … }
private StringBuilder appendFrameworksSection(final StringBuilder sb, final List<String> dependencies) { … }
private StringBuilder appendBaselineDelta(final StringBuilder sb, final Path baselineFile) throws IOException { … }
```

That's both an F2 violation (output-argument-ish — the method mutates its parameter and returns the same reference) and a naming complaint (`sb` communicates nothing). Modern Java has better tools for every one of these cases:

- Markdown/HTML *section builders* like `ClaudeMdSectionBuilder`, `BriefMarkdown`, `BaselineDeltaTable`, `HtmlFindingsSection`, `SummaryReportHtml`: rewrite as small functions that each return their own `String` (or `List<String>`) and compose with `String.join("\n", parts)` or `Collectors.joining`. No mutable threading.
- Multi-line literal HTML/Markdown: text blocks with `.formatted(...)`.
- Concatenation loops: `stream().map(...).collect(Collectors.joining(...))`.
- Actual performance-critical hot-path string building: keep `StringBuilder`, but name the variable for its role (`html`, `markdown`, `buffer` — never `sb`).

Zero uses of `StringBuffer` survive on this branch — the legacy type is dead in the repo — but a detection recipe should still flag it as a future-proof guard. And a `sb` naming finding would be cheap to detect.

### Counts aren't the whole story: choice-of-remediation isn't recorded

Several skills offer multiple fix paths. The commit messages don't say which path was taken, and sometimes the path chosen is worse than the alternative:

- **Ch7.1** can be fixed by (a) catch-rethrow, (b) translate-to-domain-exception, (c) try-with-resources, (d) retry with backoff. Our skill lists all four. Commit messages only say `fix: X (Ch7.1)`; reading the diff is the only way to know if the agent picked the lazy option.
- **G30** can be fixed by extracting a new method, or by deleting dead branches, or by restructuring the single responsibility. All three show up as `fix: X (G30)`.
- **Ch10.1** can be fixed by class split (often the agent's default) OR by deleting unused public API. The agent almost never does the second.

Changing the commit-message format to `fix: X (Ch7.1:translate, G30:extract)` or adding a one-line body `# choice: Ch7.1→translate (throws DomainException); G30→extract` would make the path auditable cheaply.

### A short list of anti-patterns the audit surfaced

| Pattern | Where | Count |
|---|---|---:|
| New class, zero tests | everywhere | 50 of 51 |
| `catch (…) { log; return empty/null; }` (Ch7.1) | `ClaudeReviewer`, `FindingJsonParser`, a few others | at least 4 |
| `StringBuilder sb` threaded through private methods as output-like parameter | `ClaudeMdSectionBuilder`, `BriefMarkdown`, others | 10+ sites |
| Extracted helper as `final class` with no visibility above package-private *and* no test — agent can't verify the helper in isolation | every new helper class | 51 |

These are the real quality cost of manual-1. The headline count (617 findings vs 466) masks that the new code itself is the source of the next round of findings the plugin will emit.

### Recommendations rolled into the next-session plan

Tracked in `docs/plan-next-session.md` on `main`:

1. "New class checklist" section injected into every fix brief.
2. Commit-message format evolves to include the remediation choice when the skill offered multiple.
3. Single-file experiment harness so we can iterate on brief wording without burning a full-repo run.
4. Replace Anthropic SDK in `claude-review` with `claude -p` CLI calls — no API key required.
5. Port selected IntelliJ refactoring algorithms (extract method, rename) into OpenRewrite primitives so class-split recipes actually work.
6. Detection + remediation recipes for StringBuilder threading and `sb` naming.
7. Detection recipe for "new main file added without companion test file."
