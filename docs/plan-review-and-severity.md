# Plan: expand Claude Review coverage and rebalance severities

Two related gaps, captured for a future session. Do not implement yet.

## 1. Claude Review prompt is sparse

### Current state

`claude-review/src/main/resources/claude-review-system.txt` defines three heuristics: G6, G20, N4. Each gets ~3 sentences of description plus an example. The prompt caps output at 5 findings per file and instructs the model to be conservative.

Findings in the last baseline attributable to Claude Review: essentially zero (the opt-in flag was off and only three codes are wired up).

### Problems

- **Narrow coverage.** A lot of heuristics are flagged as "Manual review only" in `HEURISTICS.md` but are precisely the ones an LLM can judge better than regex scanners. Nothing in the current prompt helps for C1, C4, G2, G3, G17, G21, G27, G32, N2, N3, Ch6.1, which collectively cover real design smells.
- **Thin descriptions.** Each heuristic gets a one-paragraph definition and one example. Not enough to discriminate genuine violations from borderline cases, especially for a conservative reviewer that should only flag clear hits. The prompt pattern that works better elsewhere: definition, positive example (what violates), negative example (what does *not* violate), and an anti-goal list.
- **No project context.** The prompt has no slot for "this is an OpenRewrite recipe project, these patterns are intentional." So Claude Review would flag the visitor pattern duplication and null returns as it did before we added package-level `@SuppressCleanCode`.
- **Single pass.** One call per file with all codes in the system prompt. As codes grow, prompt length grows and the model loses focus. Per-code passes (or grouping by concern) would keep each call sharply focused.

### Proposed fixes

1. **Expand coverage to the judgement-requiring set.**
   Target heuristics — in rough priority order:
   - `C1` Inappropriate Information — changelogs, author blocks, issue-tracker IDs in comments
   - `C4` Poorly Written Comment — comments that confuse more than clarify
   - `G2` Obvious Behaviour Is Unimplemented — stub methods, `throw new UnsupportedOperationException()` in places that should work
   - `G3` Incorrect Behaviour at the Boundaries — off-by-one-prone sites (empty/null/max inputs)
   - `G17` Misplaced Responsibility — methods that clearly belong elsewhere
   - `G21` Understand the Algorithm — dense methods where the algorithm is not apparent
   - `G27` Structure over Convention — conventions where an enum/type would be safer
   - `G32` Don't Be Arbitrary — "why this number, why this order, why this way" smells
   - `N2` Choose Names at Appropriate Level of Abstraction
   - `N3` Use Standard Nomenclature Where Possible
   - `Ch6.1` Data/Object Anti-Symmetry — exposing internals vs encapsulating
   - Keep existing: `G6`, `G20`, `N4`

2. **Richer per-heuristic section.** For each code, replace the single paragraph with:
   - **Definition** — one sentence
   - **Clear violation** — a short code block that IS a violation
   - **Not a violation** — a short code block that LOOKS similar but isn't
   - **Common false positives** — explicit list, e.g. "builders, visitors, Spring test fixtures"

   This matches the pattern used in `.claude/skills/clean-code-*/SKILL.md` files and reuses the same content.

3. **Structured prompt template with a project-context slot.**
   Introduce a system-prompt template with placeholders:
   ```
   {{PROJECT_CONTEXT}}
   {{HEURISTICS_BLOCK}}
   {{RESPONSE_SCHEMA}}
   {{RULES}}
   ```
   `PROJECT_CONTEXT` is populated from a new `cleanCode.claudeReview.projectContext` extension property so downstream projects can drop in "this is a Spring Boot 3 app; controllers use reactive types; repositories use JPA". In our own build, we'd slot in "this is an OpenRewrite plugin; structurally similar visitors and null-signals-no-change are idiomatic, not smells."

4. **Grouping by concern, not one pass for all.**
   Split the codes across a few focused calls:
   - `review-comments`: C1, C4
   - `review-naming`: G20, N2, N3, N4
   - `review-design`: G6, G17, G27, Ch6.1
   - `review-behaviour`: G2, G3, G21, G32

   Each call gets only its own heuristic block + shared context/schema/rules. Smaller prompts per call, better focus, easier to prompt-cache (the call set is stable per file).

5. **Prompt-caching discipline.**
   Place the system prompt + project context under a cache breakpoint; vary only the file content. A repo-wide run with 20 files × 4 concerns = 80 calls should hit a >95% cache read rate on the system text.

### Out of scope for this plan

- Migrating to OpenRouter / a non-Anthropic model.
- Adding a chat-style "self-review" pass to catch missed findings.
- Rendering the Claude Review output in a separate HTML section.

## 2. Severity distribution is too flat

### Current state

Across the current baseline (1,395 findings) only ~6 are `ERROR`. Everything else is `WARNING` or `INFO`. The build-output formatter treats ERROR specially (shown above the fold, counted prominently) but the signal never fires.

Severity sources today:
- **PMD**: per-rule mapping. A handful are ERROR (`EmptyCatchBlock`, `GodClass`); most are WARNING.
- **Checkstyle**: per-rule mapping.
- **SpotBugs**: per-rule mapping.
- **OpenRewrite**: **every finding is WARNING** (`OpenRewriteFindingSource.java` emits `Severity.WARNING` unconditionally) — this is the biggest gap.
- **CPD**: WARNING.
- **JaCoCo**: coverage-percentage based (`<50% ERROR, <75% WARNING, else INFO`).
- **DependencyUpdates**: WARNING.
- **Claude Review**: per-finding from the model.

### Principle

- `ERROR` = latent bug, safety violation, or production harm. Must block merge in a strict project.
- `WARNING` = hurts maintainability but the code works. Fix soon.
- `INFO` = style/preference. Fix opportunistically.

### Proposed severity changes

The biggest fix is to give OpenRewrite per-code severity. `OpenRewriteFindingSource.java:659,665` should look up the HeuristicCode in a severity map rather than hardcoding `Severity.WARNING`. Concrete escalations:

| Code | Recipe | Current | Proposed | Why |
|---|---|---|---|---|
| G4 | `SwallowedExceptionRecipe` | WARNING | ERROR | Empty catch silently loses errors — latent bug |
| G4 | `SystemOutRecipe` | WARNING | ERROR | `System.out`/`printStackTrace` in production bypasses logging — observability loss |
| G4 | `SuppressedWarningRecipe` | WARNING | ERROR | `@SuppressWarnings("unchecked")` hides real bugs |
| G4 | `UncheckedCastRecipe` | WARNING | ERROR | Same reason as above |
| Ch7.1 | `CatchLogContinueRecipe` | WARNING | ERROR | Log-and-continue is the canonical hidden bug — user called this out explicitly |
| Ch7.1 | `BroadCatchRecipe` | WARNING | ERROR | `catch (Exception)` masks programming errors alongside expected ones |
| Ch7.2 | `NullDensityRecipe` | WARNING | WARNING | Keep — symptom, not bug. But bump Confidence to HIGH when density > 5. |
| G8 | `VisibilityReductionRecipe` (public mutable field) | WARNING | ERROR | Encapsulation violation with real side effects |
| F2 | `OutputArgumentRecipe` | WARNING | ERROR | Caller is lied to about mutation — genuine bug surface |
| T3/T4 | `DisabledTestRecipe` (no reason given) | INFO | WARNING | Disabled-without-reason is a lost test |
| G1 | `EmbeddedLanguageRecipe` | WARNING | WARNING | Keep — maintainability, not bug |
| G5 | CPD | WARNING | WARNING + ERROR when tokens > 2× threshold | Large duplication blocks are actionable bugs |
| Ch10.1 | `ClassLineLengthRecipe` | WARNING | WARNING + ERROR when > 2× threshold | Same escalation pattern |
| F1 | `LargeConstructorRecipe` | WARNING | WARNING + ERROR when > 2× threshold | Same |
| E1 | `DependencyUpdatesFindingSource` | WARNING | INFO | User said this is low-value noise relative to other issues |

The "2× threshold" escalation pattern captures what's genuinely alarming vs merely over the line. Keep thresholds configurable in `cleanCode.thresholds.*` so consumers can tune per-project.

### Implementation sketch

1. Add a `private static final Map<HeuristicCode, Severity> DEFAULT_SEVERITY` in `OpenRewriteFindingSource`, seeded with the table above.
2. Replace the two `Severity.WARNING` hardcodes at lines 659 and 665 with a lookup, falling back to WARNING.
3. For the "2× threshold" cases, compute severity at the point the Finding is built (each recipe knows its threshold) — pass a `Severity` into the emit site instead of hardcoding.
4. Add a `cleanCode.severityOverrides = mapOf("G4" to "WARNING")` extension option for consumers who disagree with the defaults.
5. Tests: a per-source unit test that asserts the severity for each representative code.

### Consequences for the build-output formatter

The `BuildOutputFormatter` already sorts findings with ERROR first; moving real issues into ERROR makes the top of the report actually useful. The HTML report's red badge will finally reflect something actionable. No formatter changes needed.

### Consequences for the experiment

Running the experiment before and after the severity rebalance gives a second data point — how does agent attention shift when the errors count is non-trivial? Worth baselining once before making the change and once after, to see if the fix-brief prompt ("errors first") actually changes behaviour.

## Rollout order

1. OpenRewrite severity map + tests (smallest, highest-leverage change; lets the ERROR count reflect reality).
2. Per-tool severity review (PMD/Checkstyle/SpotBugs mappings) for any codes that should escalate based on the principle above.
3. Claude Review prompt expansion — grouped-by-concern template, richer per-heuristic sections, project-context slot.
4. Extension options: `severityOverrides`, `claudeReview.projectContext`, `claudeReview.groups`.

Independent work; each step ships on its own.
