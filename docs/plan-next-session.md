# Plan: next session handoff

## 2026-04-19 evening handoff — 5th variant, 3 new recipes, published Lombok recipe, JDK 25, full automation

### What shipped this session (commits `c333675` → `5e02d15`)

| Commit | Scope |
|---|---|
| `c333675` | `scripts/watch-rework-run.sh` — 30s heartbeat for any rework run |
| `7b8bf41` | `scripts/nightly-compare.sh` + `scripts/compare-runs.py` — one-command handoff (publishes ALL modules; archives raw report; diffs vs previous archive). Closes the stale-jar trap that broke the morning run. |
| `ea340e5` | RECIPES_ONLY 5th variant (no agent — pure recipe baseline); HARNESS retry now reruns `HarnessRecipePass` before the agent on each retry; F2 recipe hardened (preserves `static`/`final` modifiers; refuses to rewrite when any caller is in an unsafe shape). |
| `eb6d1c5` | `MergeInlineValidationRecipe` — folds inline `if (x == null) errors.add(MSG)` validation in caller methods into an existing `void validate(..., List<String> errors)` on the same class. Expands validate's parameters as needed. |
| `3b84f2f` | `FixedStringLogRecipe` (G12 detection) — flags `log.info("starting up")` shapes that carry no runtime info. |
| `e607cba` | Wired published `io.github.fiftieshousewife:system-out-to-lombok-log4j:0.2` (deps variant) via `Environment.scanRuntimeClasspath`. New `DeleteMumblingLogRecipe` (refactoring twin to FixedStringLogRecipe). `HarnessRecipePass.runOne` now filters multi-result outputs by source path so a recipe that creates `log4j2.xml` doesn't overwrite the input Java. |
| `5e02d15` | JDK 25 + Gradle 9.4.1 + SpotBugs 4.9.8. Daemon JVM pin deleted. `openrewrite-java21` → `openrewrite-java25` everywhere. `Configuration.setVisible()` deprecation cleared. SystemOutRecipe finding remapped from G12 (Clutter) to **G17 (Misplaced Responsibility)**. |

### State at end of session

- Working tree clean on `main`; well ahead of origin/main.
- `./gradlew clean test` green end-to-end on JDK 25 + Gradle 9.4.1.
- `./gradlew :sandbox:analyseCleanCode` produces 55 findings across 17 codes including 3 G17 sites for the System.err calls in HttpRetryPolicy + UserAccountService.
- mavenLocal updated: every module's jar is current.

### Next morning — kick off the 5-way

```bash
./scripts/nightly-compare.sh
```

That single command publishes all modules, runs the 5-way comparison
on the standard 10-file batch with a 30s heartbeat, archives the raw
report under `docs/sessions/<date>-rework-runN-raw.md`, and prints a
side-by-side diff of cost/duration/turns/findings vs the previous
archive (today's `2026-04-19-four-way-comparison-run2-raw.md`).

### Hypotheses for the 5-way

The morning run-2 had 4 variants; this run adds a 5th and several
recipes. Hypotheses worth testing:

1. **G18 false positives drop to 0 across variants** (the detector fix
   was always correct; the morning run loaded a stale `recipes` jar).
   `nightly-compare.sh` publishes everything, so this should hold.
2. **F2 recipe fires on `UserAccountService.validate`** in HARNESS's
   recipe pass list (it didn't in run-2 because the published-jar bug
   masked it). After the fix, expect the recipes column for
   UserAccountService.java to include `ReturnInsteadOfMutateArgRecipe`.
3. **`MergeInlineValidationRecipe` fires on `UserAccountService.createAccount`**
   — the inline 5-check validation should fold into the existing
   `validate()` method, expanding it from 2 params to 3.
4. **System.err / System.out converted to `@Log4j2`** by the published
   recipe in the HARNESS variant. HttpRetryPolicy's `System.err.println`
   becomes `log.error(...)`. Sandbox build will gain Lombok + Log4j2
   deps automatically.
5. **Mumbling logs deleted** if any sandbox file has them (the current
   sandbox doesn't, so this stays quiet — coverage is unit tests).
6. **RECIPES_ONLY baseline**: final-findings count should be the lower
   bound for what pure deterministic OpenRewrite achieves. Cost = $0,
   wall < 30s. If HARNESS's final findings ≈ RECIPES_ONLY's, the agent
   isn't adding much durable quality on top.
7. **HARNESS retry cost**: now reruns recipes before the agent. Expect
   HARNESS retry-pass agent invocations to skip entirely when recipes
   clear all introduced findings — should drop HARNESS cost meaningfully.

### Mini-backlog for the run-up

- The earlier backlog item "introduced-findings breakdown by code per variant"
  is still open — without it we can't tell which patterns the feedback
  loop and the new recipes actually fix.
- Decide whether `maxRetries=1` or `=2` is the right default after
  observing the new HARNESS-retry-with-recipes behaviour.
- Sandbox module currently has no Lombok dep declared. The published
  recipe's deps variant SHOULD add them on first run; verify.
- `Lombok permits Unsafe::objectFieldOffset` warning prints on every
  test run under JDK 25. Annoying but harmless. Lombok 1.18.45+ should
  fix it; bump when available.

---

## 2026-04-19 late-session handoff — 4-way measured, stale-findings + feedback-loop + F2 recipe + G18-detector fix landed

### What shipped tonight (after the morning handoff below)

- **4-way measured** (`074232d`) — first full 4-way run completed in 35m 20s.
  Raw comparison markdown + analysis write-up archived to
  `docs/sessions/2026-04-19-four-way-comparison-{raw,analysis}.md`. All
  three pre-run hypotheses **falsified**: HARNESS did not beat MCP_RECIPES
  on cost ($3.17 vs $3.17), quality tied at 23/24 final findings across
  variants, duration did not drop. Headline: every variant introduced 7-10
  new findings while fixing 27-30 — net improvement ~20 but real tech debt
  was left behind.
- **Stale findings fix for HARNESS** (`cf29324`) — the agent was reading
  the baseline findings list even after the recipe pass had fixed many of
  them; wasted turns rejecting "already done" findings. `ReworkOrchestrator`
  now threads an optional re-analyser; for HARNESS_RECIPES_THEN_AGENT it
  re-analyses between recipes and agent so the agent sees residuals only.
- **Findings measurement wired into the harness** (`b6d623d`) — previously
  the comparison report showed cost/tokens but not findings counts. Now
  every variant reports baseline / fixed / introduced / final (scoped to
  target files). Lifecycle stdout line also upgraded to show wall/cost/tokens
  per variant so the numbers are visible without opening the markdown.
  New `SandboxAnalysis` helper (extracted from `AnalyseTask`) lets the
  rework harness re-run analysis in-process without a nested Gradle build.
- **Prompt fix: stylistic findings don't block extract_method** (`7d93e26`)
  — earlier prompt wording coupled two independent rules; agent was
  falling back to Edit unnecessarily on files with stylistic findings.
- **Post-agent feedback loop + new-class checklist** (`f49426f`) —
  `ReworkOrchestrator.Options` carries a re-analyser + `maxRetries`; after
  the agent runs, if introduced findings exist we invoke the agent once
  more with a RETRY PASS banner and only those findings. Default
  `maxRetries=1`, override via `-PfeedbackRetries=N`. Shared prompt
  checklist tells the agent that any new class/helper it creates is held
  to the same standards (no catch-log-continue, no StringBuilder sb
  threading, no if-not-null as control flow, no System.err, no FQN,
  companion test for new top-level classes).
- **G18 detector fix + F2 recipe** (`5ab68f2`) — the `InappropriateStaticRecipe`
  detector missed unqualified instance-field writes like `rowsParsed++`,
  producing 5-8 identical false positives per variant. Now catches them.
  New `ReturnInsteadOfMutateArgRecipe` rewrites the pure-accumulator F2
  shape (private/package-private `void` method with sole `List<T> arg`
  mutated only via `.add()`) to return the list instead; wired into
  `HarnessRecipePass` so variant 4 applies it deterministically.

### State at end of session

- Working tree clean on `main`; 13 commits ahead of origin/main.
- All tests green: `./gradlew test` completes in ~1m, all modules pass.
- mavenLocal updated: run the **root** `./gradlew publishToMavenLocal`
  before any rework run. The run-2 comparison on 2026-04-19 used a
  stale `recipes-1.0-SNAPSHOT.jar` because the handoff only published
  `refactoring` + `plugin`; G18 detector fix was in the source but
  never in the jar the analyser loaded. Always publish all 8 modules.

### Next morning — run the 4-way

One command, no manual batch:

```bash
./scripts/nightly-compare.sh
```

The script (1) publishes ALL modules to mavenLocal, (2) runs
`:sandbox:reworkCompare` against the standard 10-file batch baked in,
(3) streams a 30s heartbeat via `watch-rework-run.sh`, (4) archives
`batch-10-comparison.md` under `docs/sessions/<date>-rework-runN-raw.md`,
and (5) prints a side-by-side diff of cost / duration / turns /
findings against the previous archived run.

Pass an optional suffix for a labeled run:
`./scripts/nightly-compare.sh post-g18-fix` → archives as
`docs/sessions/<date>-rework-run1-post-g18-fix-raw.md`.

Hypotheses for the next run:
1. **Introduced count drops** across every variant (primary feedback-loop signal).
2. **VANILLA benefits most** from the feedback loop (today it introduced 10, highest).
3. **HARNESS variant's rejected count** drops toward 0 (G18 false positives gone).
4. **Total cost** rises 10-20% across variants (one retry adds tokens; the quality trade is what we're paying for).
5. **Final findings** drops across all variants, ideally to 15-20 from tonight's 23-24.

Mini-backlog for the run-up:
- Capture the introduced-findings breakdown **by code** per variant, not just the count. Would let us see which specific patterns the feedback loop actually fixes.
- Decide whether `maxRetries=1` or `=2` is the right default. `=1` is cheap insurance; `=2` converges harder but doubles retry cost at worst.

### New backlog item — IDE warnings sweep

Walk every IDE warning this codebase generates (IntelliJ → Inspect Code; or
`./gradlew check` output), and for each class of warning that's not already
covered by an existing finding: propose a detection recipe + (where safe) a
remediation recipe, then fix the instances in this repo. Use the same
shape as existing recipes — IntelliJ's inspection catalogue is the source,
OpenRewrite is the sink. Track under D5 ("Sweep every IntelliJ warning
and port it") below.

### Unchanged from morning handoff

Everything under the morning handoff section below still applies except the
"To measure in the morning" block, which is now complete.

---

## 2026-04-19 handoff — 7 deterministic recipes + 4th variant wired, ready to measure

Following the 2026-04-18 cost-reduction session (below), tonight's arc shipped:

- Seven deterministic refactoring recipes derived from the transformation catalogue the agents produced on the 10-file batch: `MakeMethodStaticRecipe`, `RestoreInterruptFlagRecipe`, `DeleteSectionCommentsRecipe`, `ChainConsecutiveBuilderCallsRecipe`, `MathMinCapRecipe`, `ReplaceForAddNCopiesRecipe`, `CollapseSiblingGuardsRecipe`. Each with TDD tests.
- `HARNESS_RECIPES_THEN_AGENT` wired as a 4th rework variant. The harness applies all deterministic recipes (the 7 above plus `AddFinalRecipe`, `InvertNegativeConditionalRecipe`, `ShortenFullyQualifiedReferencesRecipe`) in-process to every target file BEFORE the agent runs. Agent sees a prompt listing which finding classes are already done and focuses on residuals (F2, F3, G23, stream conversion, naming).
- `HarnessRecipePass` helper + tests. `ReworkCompareTask.ALL_VARIANTS` now runs all 4 variants by default.
- Plugin gains refactoring + openrewrite runtime deps.
- `./gradlew build` green. `plugin:publishToMavenLocal` done. `.claude/settings.json` already allows all the needed tools.

### To measure in the morning

Not yet run: the 4-way cost comparison against the same 10-file batch from commit `a498332`. Expected shape:

```bash
FILES="sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java,\
sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java,\
sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java,\
sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java,\
sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java,\
sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java,\
sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java,\
sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java,\
sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java,\
sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java"
./gradlew -PcleanCodeSelfApply=true :sandbox:reworkCompare -Pfiles="$FILES"
```

This runs all 4 variants. Expected cost: previous 3-way was $9.19 (wall 23m 52s). Adding the 4th variant adds roughly another $1–$3 depending on how much the agent has to do after the harness pass strips the easy findings. Full 4-way probably $11–$14.

### Hypotheses to test

1. **HARNESS_RECIPES_THEN_AGENT beats MCP_RECIPES on cost** by 20–40% because the agent has fewer findings to address per file (deterministic recipes land the G18/G29/G31/G34 cluster upfront).
2. **Quality matches** because the deterministic transforms are exactly what the agent would have done, minus the variance.
3. **Duration drops more than cost** because fewer turns = less wall time.

### If the 4th variant underperforms, known risks

- **Recipes may land edits the agent wouldn't have** — e.g. `MakeMethodStaticRecipe` on a method the agent would have judged "doesn't need to be static". The prompt warns the agent these are done. If the diff quality is worse, inspect which recipe landed the contentious change.
- **Recipe interactions** — none tested pairwise. The pass runs recipes sequentially to fixpoint per file; if Recipe A's output breaks Recipe B's preconditions we may see missed opportunities.
- **Comparison report shape** — the harness pass adds "HarnessRecipePass" pseudo-actions to the action list. The comparison renders them in the usual Actions section; the existing `cost per action` metric is therefore diluted (more actions, same cost). Treat that metric skeptically for this variant.

### Commits tonight

- `f12af1e` Four deterministic recipes (MakeMethodStatic, RestoreInterruptFlag, DeleteSectionComments, ChainConsecutiveBuilderCalls)
- `<next>` Three more recipes (MathMinCap, ReplaceForAddNCopies, CollapseSiblingGuards) + JavaTemplate/cursor idioms noted in commit body
- `<last>` Wire HARNESS_RECIPES_THEN_AGENT 4th variant

### OpenRewrite idioms learned (for future recipes)

1. **Never construct `new Cursor(null, node)` for `printTrimmed`** — it has no SourceFile ancestor so the printer throws `IllegalStateException`. Use the visitor's own `getCursor()`, thread it into helpers.
2. **When `template.apply` will return a different statement type than the one being visited** (e.g. replacing `J.If` with `J.MethodInvocation`), use `JavaVisitor<ExecutionContext>` not `JavaIsoVisitor` — the iso visitor's return type is fixed to the input type, triggering `ClassCastException`.
3. **To construct a standalone `J.Binary` expression without hand-building the Space/Markers**, use the variable-declaration-holder trick: `boolean __tmp__ = #{any(boolean)} || #{any(boolean)};` via JavaTemplate, then pull `.getInitializer()` off the VariableDeclarations. Remember to strip prefix whitespace via `.withPrefix(Space.EMPTY)`.
4. **JavaTemplate's `contextSensitive()` is almost always needed** — without it, it defaults to standalone parsing and loses ambient imports / type info.

---

## 2026-04-18 handoff — rework-harness fixes, MCP extract_method, 4-way variant

The three-way harness run has been iterated three times. This session fixed real bugs in `extract_method`, the MCP server, and the prompt. The next session should (1) run the three-way one more time to validate, (2) implement the 4th variant (HARNESS_RECIPES_THEN_AGENT), (3) add soak testing.

### What's landed this session

- **`ExtractMethodTool` uses `parseInputs(Parser.Input.fromString(path, source))`** — old `parse(String)` let OpenRewrite's JavaParser guess the CU's source path by regex-scanning for `class <ident>`, which picked up Javadoc text ("the class holds …" → `holds.java`) and defeated `sourcePathMatches`. Fixed + regression tested.
- **Textual splicing replaces AST print-back.** `ExtractMethodRecipe.extractTextually` computes the new source by splicing: `source[0..rangeStart) + callSite + source[rangeEnd..methodClose) + newMethod + source[methodClose..]`. Bypasses OpenRewrite's `requirePrintEqualsInput` check, which was firing false positives on PipelineFixture's Javadoc (em-dash + `{@link}` references). Source bytes outside the extraction region are preserved verbatim.
- **Invariant check: `parsesCleanly`** — the spliced output is re-parsed; if it's not valid Java, the tool errors out instead of writing it. Guards against the "passes print check but corrupts file" class of bug.
- **Rejection-reason plumbing.** `ExtractMethodRecipe.lastRejectionReason()` surfaces the specific reason (source-path mismatch, alignment, analysis-rejected, reparse-failed). MCP tool returns it in the error payload so agents don't need to guess.
- **`GradleInvoker` migrated to Gradle Tooling API** — single `ProjectConnection` for the MCP server's lifetime, closed via try-with-resources in `McpServer.main`. No more per-call `./gradlew` wrapper JVM.
- **Prompt guardrails** (`plugin/.../PromptBuilder.java`):
  - Shared: "stay within target files; do NOT Read/Edit/Write under `refactoring/`, `mcp/`, `plugin/`, `build-logic/`; if a tool misbehaves, switch strategies rather than investigate."
  - `recipesBlock()`: "if `extract_method` returns ANY error, fall back to Edit immediately. DO NOT diagnose the recipe or read its source."

### Known issues (backlog)

- **`verify_build` / `run_tests` / `format` MCP tools don't pass `-PcleanCodeSelfApply=true`**, so they can't target the sandbox module. Every sandbox-targeted call falls back to Bash. Fix: either thread the flag through `GradleInvoker`, or auto-detect a sandbox target and opt in.
- **Splice indentation is hand-rolled.** `ExtractMethodRecipe.indentEachLine` / `outdentOneLevel` / `indentOfLine` plus `ExtractionSource.dedent` — ~30 lines producing reasonable but not pretty output. Proper fix: invoke the project's formatter (spotless) after the splice, OR run OpenRewrite's AutoFormat on the re-parsed spliced tree (risks re-triggering the idempotency issue on Javadoc-heavy files — would need same ctx flag). Currently the agent's prompt calls `format` at the end of a session so this is only visible in intermediate states.
- **Sandbox has no spotless task wired.** `./gradlew :sandbox:spotlessApply` fails; the `format` MCP tool errors on sandbox. Fix: either wire spotless on sandbox or make the format tool skip modules without it.
- **Soak-test corpus for recipes.** We found the Javadoc idempotency bug by pure luck during the three-way run. Need a dedicated corpus of real-world Java files (various Javadoc shapes, method lengths, control-flow styles) and a harness that runs every recipe against every file and reports rejections / output diffs / parse failures. Would have caught this bug in 30s.

### How to drive the retry

1. Working tree should be clean except for the source changes listed by `git status` at handoff time. `sandbox/.../PipelineFixture.java` is reset to HEAD.
2. `./gradlew :plugin:publishToMavenLocal` has been run — the sandbox's `reworkCompare` task sees the updated prompt.
3. `./gradlew :mcp:jar` has been run — the fat jar at `mcp/build/libs/mcp-1.0-SNAPSHOT.jar` has the textual-splice `extract_method`. `.mcp.json` points at it.
4. Run: `./gradlew -PcleanCodeSelfApply=true :sandbox:reworkCompare -Pfile=sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/PipelineFixture.java`.
5. Expectations:
   - MCP_RECIPES variant calls `extract_method` successfully on at least one phase (lines 64-67 is a known-good range).
   - If it hits a real recipe limitation (e.g. Phase B outer-field-mutation on the start phase), the error text now names the specific reason and the agent should fall back to Edit without spelunking.
   - `verify_build` will still fail on sandbox because of the `-PcleanCodeSelfApply` issue; expect 1-2 Bash fallbacks per variant. Don't treat that as a recipe bug.

### 4-way variant spec (from this session's discussion)

Add `HARNESS_RECIPES_THEN_AGENT`:
1. Before invoking the agent, the harness runs every applicable-and-safe recipe on each target file.
2. `verify_build` after the recipe pass; rollback the file if it fails (treat it as a recipe bug, record for soak testing).
3. Hand the partially-fixed file to the agent with a note: "recipes X, Y ran; they fixed findings A, B. Your job is the remaining findings."
4. Agent runs with the same prompt as MCP_RECIPES but with fewer findings to address.

Rationale: isolates the marginal value of the agent from the marginal value of the recipes. Expected outcome: lower cost than MCP_RECIPES because the deterministic dispatcher is cheaper than an LLM, and same-or-higher quality because the recipes don't make the kind of mistakes LLMs do.

### Backlog additions (from this session)

- Formatter integration for `extract_method` output (see "Splice indentation" above).
- `-PcleanCodeSelfApply=true` in GradleInvoker for sandbox targets.
- Recipe soak-test corpus + harness.
- Spotless wiring on sandbox module.
- `ExtractMethodRecipe` still has a dual path — the original `attemptExtraction` (AST-based, unused by MCP now) and the new `extractTextually`. Consider collapsing to one path.

---

Post-manual-1 handoff. What's landed since the pilot, what manual-1's audit exposed, and how to drive the next run.

## What's done since the pilot

### Plugin + recipes (on `main`)

- E1 deduped and anchored to `gradle/libs.versions.toml`; sub-modules skip. Severity → ERROR.
- Line-length ≥ 150 chars escalates to ERROR. Threshold now configurable via `cleanCode.thresholds.lineLengthErrorThreshold`.
- OpenRewrite severity map: `G4`, `Ch7.1`, `F2`, `G8` default to ERROR.
- Spotless / Google Java Format enforcement (opt-in via `cleanCode.enforceFormatting`).
- New detection recipe: `FullyQualifiedReferenceRecipe` (G12) — flags inline FQNs per file.
- New refactoring recipes:
  - `ShortenFullyQualifiedReferencesRecipe` (G12)
  - `ExtractClassConstantRecipe` (G35)
  - `InvertNegativeConditionalRecipe` (G29)
  - `SplitFlagArgumentRecipe` (F3)
  - `RenameShortNameRecipe` (N5)
- Gradle tasks: `updateVersionCatalog` (Ben-Manes → `libs.versions.toml` rewriter) and `cleanCodeSummary` (aggregates module findings into `docs/reports/index.html`).
- NPE guards: `VerticalSeparationRecipe`, `MissingExplanatoryVariableRecipe`.
- SpotBugs bundled into plugin jar — consumers need only `mavenLocal()` + `mavenCentral()` in `pluginManagement`.

### Brief-generation + runner

- Brief preamble: "your first tool calls MUST be Reads of every cited skill file."
- Per-code section: "You MUST Read this file first — before any Edit or Write tool call."
- E1 brief: action-first "bump the version, one commit per dep, skip only major-version jumps."
- Sibling-types block: lists other `.java` classnames in the same directory.
- Metric-squeezing guard: when a brief contains Ch10.1/G30 + G5, the brief warns "split by responsibility, not LOC."
- Exit protocol: "before exiting for any reason, write `experiment/<approach>-<n>-summary.md`."
- Runner pass-2 step: re-analyse after session ends; if findings appeared, spawn another Claude session.
- Cron wrapper at `~/.local/bin/cron-run-cleanclaude.sh` is branch-independent and handles auto-stash + checkout-main + pull.

### Runs

- `experiment/manual-pilot` on origin — 101 commits, 152 files, analysis at `experiment/manual-pilot-analysis.md`.
- `experiment/manual-1` on origin — 88 commits, 101 files, analysis at `experiment/manual-1-analysis.md` with addendum critique.

## What manual-1 exposed

The numerical comparison with the pilot is in the analysis. The three qualitative findings that drive this plan:

1. **When the agent creates new classes during a fix, it silently violates the skills it would enforce on existing code.** 51 new main classes on the branch; one new test class. Multiple catch-log-continue (Ch7.1) sites reproduced inside extracted helpers. The fix-brief only describes the *existing* file being fixed — the new code has no brief and no skill-check.
2. **When a skill offers multiple remediation paths, the commit message doesn't record which one the agent picked.** `fix: Foo (Ch7.1)` doesn't say whether Ch7.1 was addressed by rethrow, translate, TWR, or retry. Auditing the choice requires reading the diff.
3. **StringBuilder threading + `sb` naming is endemic.** 16 files mutation-thread a `StringBuilder` through private methods as an output-like parameter, typically named `sb`. Both F2 and a naming issue. The agent happily writes new code in this style.

## Outstanding work

Ordered roughly by leverage per hour. Items marked **(deferred)** predate manual-1 but are still in scope.

### A. Agent-behaviour changes the brief enforces

**A1. "New class you create" checklist injected into every fix brief.**
When the agent's fix involves creating a new `.java` file, the new file must satisfy every skill that the plugin would scan for:
- ≥ 1 companion test class in `src/test/java` at the matching path
- No `catch (…) { log; return empty/null; }` blocks (Ch7.1)
- Imports, not FQN references (G12)
- Visibility narrow by default — `final class` with package-private methods unless it needs wider scope
- No `StringBuilder sb` threading as an output-like parameter (F2)

Add this as a dedicated "## If you create new classes" section to `FixBriefGenerator.renderBrief`. Land before the next run.

**A2. Commit-message format evolves to include remediation choice.**
New format when a skill offered multiple paths:

```
fix: Foo (Ch7.1:translate, G30:extract)

# choice: Ch7.1 → translated to DomainException; skill also lists rethrow
# and TWR, picked translate because the caller already handles DomainException
```

The header line stays parseable by the existing aggregation scripts (which count codes). The body is new. Update `scripts/experiment-manual-prompt.txt` + `scripts/experiment-recipe-prompt.txt`. Update the agent's brief sections that list multiple paths: "If you pick between paths, document which in the commit body."

**A3. Single-file experiment harness.**
New script `scripts/run-single-file.sh <relative/path/To/File.java>`:
- Creates a scratch branch (`debug/single-<hash>`) from `main`
- Runs the plugin, takes the existing brief for that file
- Invokes `claude -p` with the single brief
- Records commits, tool log, usage, any new files created, test status
- Returns a summary the operator can read in under a minute

Use cases: iterating on brief wording, testing a new recipe against a known target, reproducing pathological behaviour, piloting A1/A2 before spending a full experiment run on them. Write to `experiment/single/<timestamp>-<File>.md`.

**A4. Post-commit regression detector.**
After each commit the agent makes, run a lightweight hook that fails the commit if:
- The commit creates a new `.java` file with no corresponding test
- The commit introduces a `catch (…) { log(...); return ...; }` block
- The commit adds a `StringBuilder sb` parameter

Implementation: a git hook in `.githooks/pre-commit` that the runner installs at the start of each experiment. Violations turn into test-fail output, the agent re-works.

### B. Collapse Anthropic SDK into the `claude -p` CLI

**B1. Replace `claude-review` module's direct Anthropic SDK calls with `claude -p` subprocess invocations.**

Current state: `claude-review` uses `com.anthropic:anthropic-java` SDK + `ANTHROPIC_API_KEY` env var to call Claude on a per-file basis for design-level heuristics (G6, G20, N4). The rest of the harness already uses `claude -p` via `run-experiment.sh`, which authenticates through the Claude Code login (no API key).

Target: one auth path, no API key. `ClaudeReviewer.analyseFile` shells out to `claude -p --output-format json < prompt` and parses the JSON response. Drop the Anthropic SDK dependency, delete `anthropic` from `libs.versions.toml`, remove the `ANTHROPIC_API_KEY` plumbing in `CleanCodeExtension` and `AnalyseTask`.

Open questions to answer before starting:
- `claude -p` doesn't have built-in output caching the way the SDK does — does the per-file hash-based `ReviewCache` need to work harder?
- Concurrency: the SDK call path could in principle batch; `claude -p` is per-invocation. Does it matter for a 20-file review?
- Session-quota accounting: the API bills separately. Moving to CLI means review calls consume the user's Claude Code session budget. Acceptable for dogfooding; may not be for a tool we hand out.

Likely answer: the dogfood value of "no API key setup" beats the lost batching. Ship it; revisit if concurrency becomes a bottleneck.

### C. StringBuilder / `sb` detection + remediation

**C1. Detection recipe `StringBuilderThreadingRecipe`.**
Flags two patterns:
- Any local variable declared as `StringBuilder sb` or `StringBuffer sb` (naming)
- Any method with a parameter of type `StringBuilder` whose body calls `.append(...)` on that parameter (mutation threading → F2 / misplaced responsibility)

Maps to F2 + a new sub-finding under G24 (naming convention).

**C2. Remediation recipe `ReplaceStringBuilderWithTextBlockRecipe`.**
Targets the most common idiom the audit exposed: a chain of `.append(...)` calls building up an HTML/Markdown fragment. Rewrite as `"""text block""".formatted(...)` when the content is mostly literal, or as a `List<String>` + `String.join("\n", ...)` when the content is mostly computed.

Won't cover every case — intentionally conservative. Agents use the skill for the rest.

**C3. Skill file update.**
New section in `.claude/skills/clean-code-java-idioms/SKILL.md`: "StringBuilder: when to use, when not to, what to name it." Gives the positive patterns (`html`, `markdown`, `buffer` names; hot-path loops) and the negative ones (mutation threading, `sb` name, sub-10-line builders).

### D. IntelliJ refactoring algorithms → OpenRewrite primitives

**D1. Port "Extract Method" — remaining phases.**
Shipped as of `f0e6c78`: Phases A (void conditional-exit), B (outer-local reassignment → output), and F (Gradle task + CLI + docs). See `docs/extract-method-recipe.md` for usage. The following phases are deferred — each one lifts a specific rejection currently documented in `ExtractMethodRecipe`'s class javadoc:

1. **Phase C — reference-type conditional exit.** Range contains `return expr;` inside a method that returns a nullable reference type. IntelliJ picks `null` as the "no early return" sentinel; call site becomes `T r = newMethod(args); if (r != null) return r;`. Blocked today by `ExitMode.classify` returning empty for any non-bare return.
2. **Phase D — `var`-typed output resolution.** Today we reject when the output local was declared with `var`. Fix: read `nv.getVariableType().getType()` and map the `JavaType` to a source name (`Primitive.getKeyword()` for primitives; simple name for `JavaType.Class` when in scope, FQN otherwise). Unblocks every modern-Java accumulator.
3. **Phase E — generic enclosing method propagation.** Today rejects if the enclosing method declares type parameters. Fix: copy `method.getTypeParameters()` into the extracted method's signature and into the call site's type arguments when inference can't resolve. Medium value — our codebase uses generics sparingly.
4. **Phase G — break/continue AST analysis.** Shipped (commit after 30d3c03). `ExitMode.classify` now walks the range AST and checks whether each `J.Break` / `J.Continue` has an enclosing loop (or switch, for break) *inside* the range. Loop-internal jumps accepted; escaping jumps still rejected. Remaining tail: `return` inside a lambda body (currently flagged as escaping even when it isn't), labeled break targeting an outer loop, and `throw` when the enclosing method declares the exception type.
5. **Phase H — `throw` when the enclosing method declares the exception.** Allow `throw` inside the range when the enclosing method's `throws` clause already propagates the thrown type. Until we have a CFG (G), this is a textual check against the throws list.
6. **Phase I — multi-output via wrapper record.** IntelliJ arrays the outputs when > 1. We could synthesise an inline record per extraction: `record Result1(int a, String b) {}`; extracted returns `Result1`; call site decomposes. Low frequency, but cleaner than rejecting.
7. **Phase J — duplicate detection.** IntelliJ scans the rest of the file for blocks equivalent to the extracted one and offers to replace them. For our agent use case this is gold — a single extraction can remove several G5 findings in one pass. Port `JavaDuplicatesExtractMethodProcessor` (already in `java-impl-refactorings/`).
8. **Phase K — post-extraction formatter pass.** The smoke-test output in `f0e6c78` had under-indented lines in the new method. Extend `ExtractMethodCli` to run Spotless (or equivalent) across the touched ranges before writing the file back. Roughly 10 lines in the CLI class.
9. **Phase L — name suggestion.** IntelliJ suggests names from the extracted statements (`ExtractMethodProcessor.suggestInitialMethodName`). Non-essential: the agent already supplies a name today. Defer.
10. **Phase M — expression-level selection.** Today only runs of complete top-level statements extract. IntelliJ also extracts sub-expressions. Agent use case never needs this; defer.

Phases C–H, J, K are the ones that unblock concrete agent workflows. See also D6 below — the regex-based variable-usage detection that underpins A and B should be replaced with AST reference resolution before expanding the surface area further.

Use extract method as the foundation for a real `SplitClassRecipe` (for Ch10.1): pick a coherent subset of methods, extract them to a new class, update call sites. Today's class-split brief asks the agent to do this by hand every time.

**D2. Port "Introduce Variable" / "Introduce Parameter".**
Smaller than D1 but useful: backs a true `ExtractExplanatoryVariableRecipe` that works on any expression, not just if-conditions.

**D3. Port "Rename" (with call-site awareness).**
OpenRewrite has `org.openrewrite.java.RenameVariable` but it only handles variables; method/class rename with call-site updates is harder and IntelliJ has it. Backs a more robust `RenameShortNameRecipe` for N5.

**D4. Port the "ConstantConditions" / DataFlow analyser to a detection recipe.**
IntelliJ flags expressions whose value is provably constant at compile time — `if (alwaysTrue) { … }`, `boolean b = !enabled && true`, `String s = value; s == null ? … : …` where `value` was just assigned a non-null literal. Port the dataflow logic into an OpenRewrite detection recipe that emits a `ConstantCondition` finding when a conditional or expression is provably always true, always false, or always a specific literal. Model the build on `SplitFlagArgumentRecipe` — same shape: pick the IntelliJ logic, translate it to the OpenRewrite primitives we have, surface it as a finding with a skill-backed brief rather than auto-rewriting.

**D5. Sweep every IntelliJ warning and port it.**
Walk the IDE's inspection catalogue (Preferences → Editor → Inspections → Java) and, for each enabled warning that matches something we care about (not Kotlin-only, not style-trivial that Spotless already handles), produce two artefacts:
1. A detection recipe that reproduces the warning as a Clean Code finding — same shape as D4 (ConstantConditions), `SplitFlagArgumentRecipe` (flag args), or `FullyQualifiedReferenceRecipe` (G12).
2. Where safe, a companion refactoring recipe that applies IntelliJ's own quick-fix — same shape as `InvertNegativeConditionalRecipe` (G29), `ShortenFullyQualifiedReferencesRecipe` (G12).
Ported from IntelliJ IDEA Community when the inspection logic is non-trivial (dataflow, reachability, nullability). Start with the inspections IntelliJ enables by default — those are the set users already trust, so false-positive risk is lowest. Track progress as a checklist under this item; cross off an inspection only when both detection and (if applicable) fix recipes ship with tests.

**D7. Port "Move Method" (`MoveInstanceMethodProcessor` + `MoveStaticMemberHandler`).**
Move a single method from one class to another and rewrite every call site. Parameters: `(file, methodName, targetType)`. Foundation for D8. Narrow first cut: static methods only, or instance methods whose body references exactly one field of `this` (move into that field's class). Broader scope later: visibility widening, conflict detection, generic-parameter propagation, duplicate-match folding. Fixes G14 (feature envy), G17 (misplaced responsibility), G18 (inappropriate static).

**D8. Port "Extract Class" (`ExtractClassProcessor`).**
Move N methods and M fields from an existing class into a new class; rewrite call sites either via a held reference or static delegation. Uses D7 as the primitive — extract class is "new target type + N × move method + constructor wiring". Parameters: `(file, methodNames[], fieldNames[], newClassName)`. Fixes Ch10.1 (class too large), Ch10.2 (SRP). Highest-leverage architectural lever we don't have.

**D9. Port "Introduce Parameter Object" (`IntroduceParameterObjectProcessor`).**
When a method has ≥ 4 parameters that are frequently passed together, collect them into an inline `record` (or Lombok `@Value`) and rewrite every call site. Parameters: `(file, methodName, parameterNames[], newTypeName)`. Fixes F1 (too many arguments). Mechanical once a record-synthesis helper exists.

**D10. Port "Use Try-With-Resources" (`UseTryWithResourcesInspection`).**
Detect manual `close()` calls in `finally` blocks or explicit try/finally patterns that should be `try-with-resources`, then auto-fix. IntelliJ has both inspection and quick-fix. Fixes the biggest cluster of Ch7.1 findings mechanically. Needs a simple dataflow to recognise the pattern, nothing as deep as ConstantConditions (D4).

**D11. Generate Test Scaffold recipe (T1).**
For every public class in `src/main/java` with no corresponding file at the matching path under `src/test/java`, create `FooTest.java` with `@Test`-annotated placeholder methods — one per public method of the source class — with bodies that just `fail("TODO: " + methodName)` and a JUnit `@DisplayName` drawn from the method name. Not a real refactor, but removes the "where do I put this test file" friction from every T1 brief. Pure template work, ~80 lines.

**D13. Teach extract / move recipes to update the matching test class.**
When `ExtractMethodRecipe` creates a new helper, it should also add a placeholder `@Test` method to `<SourceClass>Test.java` at the matching path under `src/test/java` (creating the file if absent), asserting `fail("TODO")` and named after the new method. When `MoveMethodRecipe` moves `Utils.doubleIt` to `Helpers`, any `UtilsTest` method whose body calls `Utils.doubleIt(...)` should move to `HelpersTest`. Drops the manual "don't forget the test" step that every agent brief re-learns. Partial alternative: a post-extraction rejection if the matching test file has no test for the new helper, so the agent is forced to write one in the same commit.

**D12. Port "Inline Method" / "Inline Variable" (`InlineMethodProcessor`, `InlineLocalVariableProcessor`).**
Inverse of extract method and of D2. A pass to kill G9 (dead code) and simplifications where a named local or helper method reads as less expressive than its expansion. Constrained first cut: only inline methods called from exactly one site, and only variables assigned once.

**D15. Expose `move_method` as an MCP tool.**
The `mcp/` server (shipped in this session) exposes `extract_method`, `verify_build`, `run_tests`, and `format` — but not `move_method` yet. MoveMethodRecipe is wired in-process, but move is a project-wide transform: beyond the source file, it needs to find and rewrite call sites across every CU that references the moved method. Decide the MCP tool's project-scanning shape (read from findings.json? walk the module? take an explicit list of files?) and implement. Default target: same module as source, but accept a `callerModules` argument for cross-module moves.

**D14. De-duplicate `refactoring/` — hoist shared support, kill CPD hits.**
G5 was formerly suppressed at the package level on the grounds that OpenRewrite visitor scaffolding repeats across recipes. A CPD pass (post-d5c2b0d) with that suppression lifted shows that claim was overclaimed — most duplicated blocks are in the scan+rewrite bodies we authored, not in visitor boilerplate. Seven CPD-confirmed duplications to drive the work, biggest first:

1. **Constant-extraction pipeline — 50 lines of near-duplicate logic across `ExtractClassConstantRecipe` ↔ `ExtractConstantRecipe` (24 + 17 + 9 line blocks).** They're basically the same recipe parameterised by "literal type". Extract a shared scan→rewrite skeleton; the two surviving subclasses differ only in which J.Literal kinds they accept.
2. **Parse-a-holder-class + unpack statement** — 10-line block across `ExtractExplanatoryVariableRecipe` ↔ `WrapAssertAllRecipe`; 6-line 3-way with `RemoveNestedTernaryRecipe`. Also hit by `EncapsulateBoundaryRecipe` and `ExtractConstantRecipe`. `AstFragments.parseStatement` already does this — hoist it from `refactoring/.../extractmethod/` to `refactoring/.../support/` and migrate each site to a one-line call. `AstFragments.parseField` (new) covers the constant-recipe use case.
3. **Statement-list-replace** — 9 lines across `EncapsulateBoundaryRecipe` ↔ `MoveDeclarationRecipe`; 8-9 lines across `ExtractExplanatoryVariableRecipe` ↔ `EncapsulateBoundaryRecipe` ↔ `MoveDeclarationRecipe`. Extract a `Statements.replaceMatching(block, predicate, replacement)` helper into `support/`.
4. **Modifier-mutation visitor** — 9 lines across `AddFinalRecipe` ↔ `ReduceVisibilityRecipe`. Both add / remove `J.Modifier` entries on matching declarations. Extract a `ModifierEditor` helper.
5. **Smaller tail** — 8-line duplications across `RecordToLombokValueRecipe` ↔ `SplitFlagArgumentRecipe` and a couple of others. Address only if the surrounding work makes them obvious.

`AstFragments.parseMethod`, `LineIndex`, and `VariableUsagePatterns` have no reuse candidates today; leave them in the extractmethod package until a second caller surfaces.

Acceptance: G5 findings in `refactoring/` drop to zero or are narrowly suppressed at the class level with a specific reason; existing tests stay green without modification.

**D6. Replace `VariableUsagePatterns` regex with real reference resolution.**
The D1 port analyses reads and writes inside an extracted range via word-boundary regex (see `refactoring/.../extractmethod/VariableUsagePatterns.java`). The compromise is conservative — it over-includes (matches identifiers in comments and string literals) and can't tell the difference between `foo.bar` and `bar`. Rewrite to walk the AST: collect `J.Identifier` nodes and filter by `getFieldType()` / cursor parent / name-reference semantics to resolve each identifier to the right binding. IntelliJ uses `PsiReference.resolve()` for the equivalent job. Acceptance: every extract-method test still passes, plus new tests proving false-positive matches in comments/strings no longer register as reads.

Scope: **only the algorithms, not the plugin infrastructure.** We're not shipping an IntelliJ plugin; we're translating the algorithmic ideas into OpenRewrite idioms.

### H. Urgent — move the whole build to JDK 25

Today's `gradle/gradle-daemon-jvm.properties` pins the daemon to JDK 21 because OpenRewrite's `rewrite-java-21` module references `com.sun.tools.javac.code.Type$UnknownType`, which moved/disappeared in JDK 25. That pin works but ages badly — contributors on machines with only JDK 25 need to install 21, and we lose every JDK 22-25 language feature across the build.

Fix: move `cleancode.java-conventions.gradle.kts` to `JavaLanguageVersion.of(25)`, switch every module's `runtimeOnly(libs.openrewrite.java21)` to `libs.openrewrite.java25`, and delete `gradle-daemon-jvm.properties`. The TestKit-subproject failure observed during the 8.79.5 bump (`Unsupported class file major version 69`) came from mixing JDK 21 subprocess runtimes with JDK 25 jars on the parent classpath — once everything is consistently 25, that conflict disappears.

Acceptance: full build + all tests pass without the daemon pin, and `:sandbox:analyseCleanCode` produces findings via OpenRewrite. Do this before the next real experiment run so we're not nursing the JDK-21 daemon pin into every future setup.

### I. Post-MCP-experiment findings and follow-ups

Three-way comparisons on the sandbox fixtures (OrchestratorFixture,
AccumulatorFixture, GuardFixture) showed **vanilla is consistently cheapest**
and the MCP variants add ~30-70% cost without producing different output.
Every variant converges on the same 3 actions of equivalent quality.

**Why MCP costs more, not less.** Cache-creation tokens (not shown in the
summary) dominate: each variant's prompt shape gets its own cache key →
three distinct prompts in one benchmark run pays cache-creation ×3. The
MCP tool descriptions add ~400 prompt tokens → ~80-120K extra cache-
creation per MCP variant. Compact MCP responses save ~50 tokens per call
but that's a rounding error at the per-session level. Conclusion: MCP is
worthwhile only when tool usage is dense within a session and the prompt
shape is stable across sessions; for paired experiments it's a cost.

**Bash drift is rational.** For tools that are a one-line wrapper over a
Gradle task (`format(module)` ≡ `./gradlew :<module>:spotlessApply`),
the agent skips the MCP tool because reaching it costs a ToolSearch
round-trip, while Bash is on the default surface. Only non-Bash work
(like `extract_method` which is genuinely in-process OpenRewrite, not
a Gradle wrapper) has a durable reason to exist on the MCP server.

**Permission denials are silent in non-interactive mode.** The MCP
extract_method call in the three-way run was denied because the project-
scope `.claude/settings.json` didn't pre-allow it. Fixed in the same
commit. McpServerPermissionsTest now enforces that every tool in
McpServer.defaultRegistry has a matching allow entry — prevents the
same failure shape when a new tool ships.

**Follow-ups** (roughly ordered by value):

1. Extend `GradleInvoker` to use the Gradle Tooling API instead of
   `ProcessBuilder`. Daemon connection reused across tool calls in the
   same MCP session; ~5-10s saved on the first call and ~0.5-2s saved
   per subsequent call. Also eliminates the daemon-start flakes on
   contributor machines.
2. Codify the patterns the vanilla agent produced into MCP recipes:
   imperative-loop → stream-chain; `Objects.requireNonNull` fail-fast
   null guards at method entry; void-mutating-parameter → value-
   returning method + record result type; static-utility conversion.
3. Investigate whether the MCP tool schema can be declared NOT as a
   deferred tool — skipping ToolSearch would reduce the one-time schema-
   load cost. May be a Claude Code configuration detail we don't control.
4. Pre-batch target files by finding shape before handing them to the
   agent. Today `reworkCompare -Pfiles=<csv>` runs one session over the
   list regardless of shape mismatch; when the batch mixes
   extraction-heavy and stylistic files, the agent context-switches and
   extract_method's schema-load cost amortizes poorly. Add a
   `BatchPlanner` that clusters files by dominant-finding-class
   (extraction / stylistic / null / mutation), runs one batch per
   cluster per variant, and writes one combined report. Trade-off is
   more total sessions (more cache creation) for better per-session
   tool-fit.
5. Measure cache-creation tokens explicitly in the comparison report
   (SHIPPED — ComparisonReport now shows every token category plus
   derived metrics like cache-hit rate and cost-per-action).

### G. Recipe ordering

Recipes are not commutative. Running formatting (Spotless / Google Java Format) before extracting a method means the next extraction re-introduces formatting deltas; running add-final after extract-method means the extracted body needs another pass to become compliant. The agent shouldn't have to figure out the order per-brief — the plugin should orchestrate it.

Proposed passes, earliest first, each an ordered list of recipes. Later passes only start once the previous pass has produced no new findings:

1. **Architectural overhauls (semantic, widest blast radius).** ExtractMethodRecipe (D1), MoveMethodRecipe (D7), ExtractClassRecipe (D8), IntroduceParameterObjectRecipe (D9), InlineMethodRecipe (D12). Each can invalidate the others — an extraction might expose an obvious rename; a class split might make some methods inlineable. Run to fixpoint *within* this pass before advancing.

2. **Mechanical refactors (narrow, predictable).** InvertNegativeConditionalRecipe (G29), ShortenFullyQualifiedReferencesRecipe (G12), ExtractClassConstantRecipe (G35), EncapsulateBoundaryRecipe (G33), SplitFlagArgumentRecipe (F3), RemoveNestedTernaryRecipe (G19/G28), RenameShortNameRecipe (N5), MoveDeclarationRecipe (G10), UseTryWithResourcesRecipe (D10). Each is local and idempotent; ordering within this pass is independent but running to fixpoint is cheap.

3. **Housekeeping (near-tokens).** AddFinalRecipe (G22), DeleteUnusedImportRecipe (J1), WrapAssertAllRecipe (T1), version-catalog bumps.

4. **Formatting (always last).** Spotless / Google Java Format. Any earlier pass produces formatting noise; the final pass normalises. Never re-triggers previous passes.

Implementation: a new `CleanCodeOrchestrate` Gradle task that runs passes 1 → 4 in sequence, stopping each pass only when a full iteration produces zero edits. Skips passes when the plugin isn't configured to enforce formatting (`cleanCode.enforceFormatting = false`). Each pass emits a delta report so the user can see which phase did what.

Open question: does the agent still get per-file briefs, or does the orchestrator hide everything below the architectural layer? Candidate answer: agent briefs target pass 1 only; passes 2-4 are plugin-driven and the agent never sees those findings because they auto-fix.

### E. Other recipes worth building

In order of estimated leverage (from manual-1's remaining-finding table):

| Code | Remaining | Recipe idea |
|---|---:|---|
| G30 (107) | method size | Hard; deliberate semantic split. Agent-driven with better skill prose. |
| G29 (51) | negative conditionals | Extend `InvertNegativeConditionalRecipe` to cover `!= null` / `!= ""` cases — currently only unary `!` at top level. |
| G35 (31) | configurable data | Extend `ExtractClassConstantRecipe` to cover String literals that ARE repeated (currently just numbers), with name heuristics (URL → `ENDPOINT_X`, `.properties` key → `PROP_X`). |
| F2 (27) | output arguments | **Partially shipped** (`5ab68f2`): `ReturnInsteadOfMutateArgRecipe` handles `void foo(List<T> out, ...)` with `.add()`-only mutation. Still to do: `Map`/`Set` accumulators, public visibility, cross-module callers. |
| T1 (26) | missing tests | New `GenerateTestScaffoldRecipe` — for every public class/method with no test, create `FooTest.java` with an empty-body placeholder test. The agent fills in the body via the T1 brief. |
| G10 (25) | vertical separation | Extend `MoveDeclarationRecipe` to cover the common patterns the current version misses. |

### F. Previously deferred (still open)

**F1. B4 from the pilot plan — HEURISTICS.md formal examples.**
Each heuristic entry gets violation/correction/false-positive code blocks. Mostly content work; can be parallelised across heuristics.

**F2. B5 from the pilot plan — Skill body audit.**
Walk each `clean-code-*/SKILL.md`, cross-check against `SkillPathRegistry`, fill gaps with worked examples.

**F3. C1 from the pilot plan — Pippa reclassification list on the pilot branch.**
Add a "Codes Pippa should re-review" section to `experiment/manual-pilot-analysis.md`.

**F4. D1 — Schedule `recipe-1` run.**
Not yet executed. Once A1–A4 and some of C ship, schedule a `recipe-1` cron so we can compare manual vs recipe-assisted side by side.

## Branch discipline

Still the same as the pilot:

- `main` — all plugin code, all plans. Push freely.
- `experiment/manual-pilot`, `experiment/manual-1` — analysis and run logs only. Pushed; don't merge back.
- Next runs (`manual-2`, `recipe-1`) — will branch from current main at launch time.

## How to resume in a fresh session

Read this document, then `experiment/manual-1-analysis.md` on the `experiment/manual-1` branch for the concrete evidence behind sections A–C. Start with **A1** (new-class checklist) — it's the smallest change that closes the biggest quality leak.
