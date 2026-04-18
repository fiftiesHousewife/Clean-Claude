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

**D1. Port "Extract Method".**
IntelliJ IDEA Community is open source; its extract-method refactoring lives in `java-impl/src/com/intellij/refactoring/extractMethod/`. Read the algorithm (signature inference, variable escape analysis, return-value synthesis) and re-implement as an OpenRewrite recipe that takes a parameter `selection: { file, startLine, endLine, newMethodName }`.

Use this as the foundation for a real `SplitClassRecipe` (for Ch10.1): pick a coherent subset of methods, extract them to a new class, update call sites. Today's class-split brief asks the agent to do this by hand every time.

**D2. Port "Introduce Variable" / "Introduce Parameter".**
Smaller than D1 but useful: backs a true `ExtractExplanatoryVariableRecipe` that works on any expression, not just if-conditions.

**D3. Port "Rename" (with call-site awareness).**
OpenRewrite has `org.openrewrite.java.RenameVariable` but it only handles variables; method/class rename with call-site updates is harder and IntelliJ has it. Backs a more robust `RenameShortNameRecipe` for N5.

Scope: **only the algorithms, not the plugin infrastructure.** We're not shipping an IntelliJ plugin; we're translating the algorithmic ideas into OpenRewrite idioms.

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
