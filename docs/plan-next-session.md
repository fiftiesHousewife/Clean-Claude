# Plan: next session handoff

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
4. **Phase G — real control-flow graph for break/continue.** Port IntelliJ's `ControlFlowWrapper` / `ControlFlowUtil` in enough detail to tell a loop-internal `break` from a method-escaping one. Replaces the conservative "any break/continue rejects" rule with the correct "only escaping exits need sentinel handling."
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
| F2 (27) | output arguments | New `OutputArgumentToReturnRecipe` — detect `void foo(Result r)` where `r` is mutated, rewrite as `Result foo(...)` that returns a fresh Result. Private methods only. |
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
