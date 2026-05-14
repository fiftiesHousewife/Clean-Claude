# Clean Code Plugin

[![CI](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml/badge.svg)](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Gradle](https://img.shields.io/badge/Gradle-9.0-blue)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

A Gradle plugin that detects violations of Robert C. Martin's *Clean Code* heuristics across a Java codebase. It combines static analysis tools (PMD, Checkstyle, SpotBugs, JaCoCo) with 56 custom OpenRewrite detection recipes and 29 refactoring recipes, normalises all findings into Martin's taxonomy, and produces linked HTML reports with book references and prescriptive guidance.

> *"Clean code reads like well-written prose."* -- Robert C. Martin, *Clean Code* (2008)

## Heuristic Coverage

Every finding is mapped to a specific heuristic from *Clean Code* Chapter 17 ("Smells and Heuristics") or to a chapter-specific pattern. The plugin currently detects **60 heuristic codes** across **9 finding sources**.

For the full Robert Martin text, detection details, and skill file links for every heuristic, see **[HEURISTICS.md](HEURISTICS.md)**. For per-tool rule mappings with documentation links, see **[FINDING-SOURCES.md](FINDING-SOURCES.md)**.

| Code   | Name                                        | Reference       | Detection                                                        |
|--------|---------------------------------------------|-----------------|------------------------------------------------------------------|
| [C2](HEURISTICS.md#c2-obsolete-comment)     | Obsolete Comment                            | Ch.17 p.286     | ObsoleteCommentRecipe                                            |
| [C3](HEURISTICS.md#c3-redundant-comment)     | Redundant Comment                           | Ch.17 p.286     | MumblingCommentRecipe                                            |
| [C5](HEURISTICS.md#c5-commented-out-code)     | Commented-Out Code                          | Ch.17 p.287     | CommentedCodeRecipe                                              |
| [E1](HEURISTICS.md#e1-build-requires-more-than-one-step)     | Build Requires More Than One Step           | Ch.17 p.287     | DependencyUpdatesFindingSource                                   |
| [F1](HEURISTICS.md#f1-too-many-arguments)     | Too Many Arguments                          | Ch.17 p.288     | Checkstyle ParameterNumber, LargeConstructorRecipe               |
| [F2](HEURISTICS.md#f2-output-arguments)     | Output Arguments                            | Ch.17 p.288     | OutputArgumentRecipe, InconsistentReturnRecipe                   |
| [F3](HEURISTICS.md#f3-flag-arguments)     | Flag Arguments                              | Ch.17 p.288     | FlagArgumentRecipe                                               |
| [F4](HEURISTICS.md#f4-dead-function)     | Dead Function                               | Ch.17 p.288     | PMD UnusedPrivateMethod                                          |
| [G1](HEURISTICS.md#g1-multiple-languages-in-one-source-file)     | Multiple Languages in One Source File       | Ch.17 p.288     | EmbeddedLanguageRecipe                                           |
| [G4](HEURISTICS.md#g4-overridden-safeties)     | Overridden Safeties                         | Ch.17 p.289     | UncheckedCastRecipe, SystemOutRecipe, SwallowedExceptionRecipe, SuppressedWarningRecipe, PMD, SpotBugs |
| [G5](HEURISTICS.md#g5-duplication)     | Duplication                                 | Ch.17 p.289     | CPD token-based detection                                        |
| [G7](HEURISTICS.md#g7-base-classes-depending-on-their-derivatives)     | Base Classes Depending on Derivatives       | Ch.17 p.291     | BaseClassDependencyRecipe                                        |
| [G8](HEURISTICS.md#g8-too-much-information)     | Too Much Information                        | Ch.17 p.291     | VisibilityReductionRecipe, PMD, SpotBugs                         |
| [G9](HEURISTICS.md#g9-dead-code)     | Dead Code                                   | Ch.17 p.292     | PMD, SpotBugs                                                    |
| [G10](HEURISTICS.md#g10-vertical-separation)    | Vertical Separation                         | Ch.17 p.292     | VerticalSeparationRecipe                                         |
| [G11](HEURISTICS.md#g11-inconsistency)    | Inconsistency                               | Ch.17 p.292     | InconsistentNamingRecipe                                         |
| [G12](HEURISTICS.md#g12-clutter)    | Clutter                                     | Ch.17 p.293     | PMD/Checkstyle                                                   |
| [G13](HEURISTICS.md#g13-artificial-coupling)    | Artificial Coupling                         | Ch.17 p.293     | ArtificialCouplingRecipe                                         |
| [G14](HEURISTICS.md#g14-feature-envy)    | Feature Envy                                | Ch.17 p.293     | FeatureEnvyRecipe                                                |
| [G15](HEURISTICS.md#g15-selector-arguments)    | Selector Arguments                          | Ch.17 p.294     | SelectorArgumentRecipe                                           |
| [G16](HEURISTICS.md#g16-obscured-intent)    | Obscured Intent                             | Ch.17 p.295     | NestedTernaryRecipe                                              |
| [G18](HEURISTICS.md#g18-inappropriate-static)    | Inappropriate Static                        | Ch.17 p.296     | InappropriateStaticRecipe, Checkstyle, SpotBugs                  |
| [G19](HEURISTICS.md#g19-use-explanatory-variables)    | Use Explanatory Variables                   | Ch.17 p.296     | MissingExplanatoryVariableRecipe                                 |
| [G22](HEURISTICS.md#g22-make-logical-dependencies-physical)    | Make Logical Dependencies Physical          | Ch.17 p.298     | Checkstyle FinalLocalVariable                                    |
| [G23](HEURISTICS.md#g23-prefer-polymorphism-to-ifelse-or-switchcase)    | Prefer Polymorphism to If/Else or Switch    | Ch.17 p.299     | SwitchOnTypeRecipe, StringSwitchRecipe, StringlyTypedDispatchRecipe |
| [G24](HEURISTICS.md#g24-follow-standard-conventions)    | Follow Standard Conventions                 | Ch.17 p.299     | Checkstyle                                                       |
| [G25](HEURISTICS.md#g25-replace-magic-numbers-with-named-constants)    | Replace Magic Numbers with Named Constants  | Ch.17 p.300     | MagicStringRecipe                                                |
| [G26](HEURISTICS.md#g26-be-precise)    | Be Precise                                  | Ch.17 p.300     | LegacyFileApiRecipe, RawGenericRecipe, PMD                       |
| [G28](HEURISTICS.md#g28-encapsulate-conditionals)    | Encapsulate Conditionals                    | Ch.17 p.301     | EncapsulateConditionalRecipe                                     |
| [G29](HEURISTICS.md#g29-avoid-negative-conditionals)    | Avoid Negative Conditionals                 | Ch.17 p.302     | NegativeConditionalRecipe, GuardClauseRecipe                     |
| [G30](HEURISTICS.md#g30-functions-should-do-one-thing)    | Functions Should Do One Thing               | Ch.17 p.302     | WhitespaceSplitMethodRecipe, ImperativeLoopRecipe                |
| [G31](HEURISTICS.md#g31-hidden-temporal-couplings)    | Hidden Temporal Couplings                   | Ch.17 p.304     | TemporalCouplingRecipe                                           |
| [G33](HEURISTICS.md#g33-encapsulate-boundary-conditions)    | Encapsulate Boundary Conditions             | Ch.17 p.304     | BoundaryConditionRecipe                                          |
| [G34](HEURISTICS.md#g34-functions-should-descend-only-one-level-of-abstraction)    | Functions Should Descend Only One Level     | Ch.17 p.304     | SectionCommentRecipe                                             |
| [G35](HEURISTICS.md#g35-keep-configurable-data-at-high-levels)    | Keep Configurable Data at High Levels       | Ch.17 p.306     | ConfigurableDataRecipe, HardcodedListRecipe                      |
| [G36](HEURISTICS.md#g36-avoid-transitive-navigation)    | Avoid Transitive Navigation                 | Ch.17 p.306     | LawOfDemeterRecipe (fluent APIs excluded)                        |
| [J1](HEURISTICS.md#j1-avoid-long-import-lists-by-using-wildcards)     | Avoid Long Import Lists                     | Ch.17 p.307     | Checkstyle AvoidStarImport                                       |
| [J2](HEURISTICS.md#j2-dont-inherit-constants)     | Don't Inherit Constants                     | Ch.17 p.307     | InheritConstantsRecipe                                           |
| [J3](HEURISTICS.md#j3-constants-versus-enums)     | Constants versus Enums                      | Ch.17 p.308     | EnumForConstantsRecipe                                           |
| [N1](HEURISTICS.md#n1-choose-descriptive-names)     | Choose Descriptive Names                    | Ch.17 p.309     | BadClassNameRecipe, Checkstyle                                   |
| [N5](HEURISTICS.md#n5-use-long-names-for-long-scopes)     | Use Long Names for Long Scopes             | Ch.17 p.312     | ShortVariableNameRecipe                                          |
| [N6](HEURISTICS.md#n6-avoid-encodings)     | Avoid Encodings                             | Ch.17 p.312     | EncodingNamingRecipe                                             |
| [N7](HEURISTICS.md#n7-names-should-describe-side-effects)     | Names Should Describe Side-Effects          | Ch.17 p.313     | SideEffectNamingRecipe                                           |
| [T1](HEURISTICS.md#t1-insufficient-tests)     | Insufficient Tests                          | Ch.17 p.313     | JaCoCo line coverage, MultipleAssertRecipe, PrivateMethodTestabilityRecipe |
| [T2](HEURISTICS.md#t2-use-a-coverage-tool)     | Use a Coverage Tool                         | Ch.17 p.313     | JaCoCo report presence                                           |
| [T3](HEURISTICS.md#t3-dont-skip-trivial-tests)     | Don't Skip Trivial Tests                    | Ch.17 p.313     | DisabledTestRecipe                                               |
| [T4](HEURISTICS.md#t4-an-ignored-test-is-a-question-about-an-ambiguity)     | An Ignored Test Is a Question               | Ch.17 p.313     | DisabledTestRecipe                                               |
| [T8](HEURISTICS.md#t8-test-coverage-patterns-can-be-revealing)     | Test Coverage Patterns                      | Ch.17 p.314     | JaCoCo per-class analysis                                        |
| [T9](HEURISTICS.md#t9-tests-should-be-fast)     | Tests Should Be Fast                        | Ch.17 p.314     | Surefire timing                                                  |
| [Ch7.1](HEURISTICS.md#ch71-use-exceptions-rather-than-return-codes)  | Use Exceptions Rather Than Return Codes     | Ch.7 p.103      | CatchLogContinueRecipe, BroadCatchRecipe                         |
| [Ch7.2](HEURISTICS.md#ch72-dont-return-null)  | Don't Return Null                           | Ch.7 p.110      | NullDensityRecipe, SpotBugs                                      |
| [Ch10.1](HEURISTICS.md#ch101-classes-should-be-small) | Classes Should Be Small                     | Ch.10 p.136     | ClassLineLengthRecipe                                            |

## Sample Output

```
═══════════════════════════════════════════════════════════════════════════
  CLEAN CODE ANALYSIS  —  my-project
═══════════════════════════════════════════════════════════════════════════

  1 errors  ·  18 warnings  ·  2 info

───────────────────────────────────────────────────────────────────────────
  Ch7_1: Use Exceptions Rather Than Return Codes (1)
  Clean Code Ch.7 'Error Handling' p.103

     ! UserService.java  Catch block in 'save' only logs or is empty

───────────────────────────────────────────────────────────────────────────
  Sources:
    openrewrite: 14
    checkstyle: 4
    spotbugs: 3
    pmd: 1
    jacoco: 1

═══════════════════════════════════════════════════════════════════════════
  23 findings  —  ./gradlew cleanCodeExplain --finding=<code>
═══════════════════════════════════════════════════════════════════════════
```

## Configuration

```kotlin
cleanCode {
    skillsDir = ".claude/skills"
    repositoryUrl = "https://github.com/your-org/your-repo"   // enables linked HTML reports

    thresholds {
        classLineCount = 200              // default 150
        recordComponentCount = 8          // default 6
        nullCheckDensity = 4              // default 3
        chainDepthThreshold = 4           // default 3
        verticalSeparationDistance = 15    // default 10
        methodBlankLineSections = 8       // default 6
        privateMethodMinLines = 15        // default 12
        magicStringMinOccurrences = 3     // default 2
        stringSwitchMinCases = 4          // default 3
        shortNameMinLength = 2            // default 2
        cpdMinimumTokens = 100            // default 50
        magicNumberMinValue = 1           // default 1
        sectionCommentThreshold = 1       // default 1
        hardcodedListMinLiterals = 5      // default 5
        temporalCouplingMinCalls = 3      // default 3
    }
    disabledRecipes = listOf("G36", "G10")

    // Opt-in rules — disabled by default because they're noisy or only valuable
    // to teams that have already committed to the convention. Run the build
    // once and the report's "Optional rules" panel lists what's available.
    enabledOptionalRules = listOf(
        "checkstyle:FinalLocalVariable",       // G22 — require all locals/params to be final
        "pmd:UseLocaleWithCaseConversions"     // G26 — require an explicit Locale on toLowerCase/toUpperCase
    )

    servePort = 7070                           // port for ./gradlew cleanCodeServe (default 7070)

    packageSuppressions = mapOf(
        "io.github.fiftieshousewife.cleancode.recipes" to listOf("G5", "Ch7_2")
    )
}
```

## Suppressions

Five complementary mechanisms, from narrowest to broadest:

| Mechanism | Where it lives | Scope |
|---|---|---|
| `@SuppressWarnings("CleanCode:Gxx")` | Method, type, constructor | Exact block, JDK-standard annotation |
| `@SuppressCleanCode({...}, reason="...")` | Method, type, constructor | Exact block, with required reason + optional `until` expiry |
| `@SuppressCleanCode({...}, reason="...")` on `package-info.java` | Package | Every file in that package (CPD cross-file pairs too) |
| `cleanCode.packageSuppressions = mapOf(...)` | Gradle build script | Package, config-driven — fallback for findings without a source anchor |
| `cleanCode.disabledRecipes = listOf(...)` | Gradle build script | Heuristic code, project-wide |

Prefer `@SuppressCleanCode` over plain `@SuppressWarnings`: it forces a `reason`, supports `until="YYYY-MM-DD"` so the suppression expires and reappears as a finding, and emits a meta-finding when the reason is blank or `TODO`. Use `@SuppressWarnings("CleanCode:Gxx")` when the IDE's existing yellow-warning UX matters more than the audit trail (e.g. one-off cleanup, no team policy on suppression hygiene).

Example — suppress CPD duplication and null-density across a whole package:

```java
@SuppressCleanCode(
    value = { HeuristicCode.G5, HeuristicCode.Ch7_2 },
    reason = "OpenRewrite visitor pattern produces structurally similar scanners and "
            + "relies on null returns to signal no-change — API-imposed, not design flaws"
)
package io.github.fiftieshousewife.cleancode.recipes;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.annotations.SuppressCleanCode;
```

This repo applies exactly that to `recipes/` and `refactoring/`, together with JSpecify `@NullMarked` at the package level so IDE nullability inspections match the OpenRewrite API contract.

Gaps worth knowing:
- `E1` (outdated-dependency findings) have no source anchor; `@SuppressCleanCode` cannot suppress them. Use `disabledRecipes = listOf("E1")` if noise from dependency reports is unwanted.
- When a suppression expires (`until` date in the past), the index emits a meta-finding pointing at the annotation so it surfaces in the next report.

### What the localhost:7070 buttons do

The interactive report has three persistence buttons. Each writes to a tracked file you can review and commit:

| Button | Writes to | Effect |
|---|---|---|
| **Suppress** | The source file at the finding's line | Inserts `@SuppressWarnings("CleanCode:CODE")` directly above the enclosing declaration |
| **Disable** | `build.gradle.kts` | Appends the code to `cleanCode { disabledRecipes.add(...) }` |
| **Tune** | `build.gradle.kts` | Updates the named threshold under `cleanCode { thresholds { ... } }` |

There is no separate `.cleancode/suppressions.yml` — every change goes to a file you'd commit anyway.

## Architecture

```
CleanClaude/
├── annotations/    HeuristicCode enum, @SuppressCleanCode annotation
├── core/           Finding, AggregatedReport, BuildOutputFormatter,
│                   HeuristicDescriptions, SuppressionIndex, BaselineManager,
│                   ClaudeMdGenerator, HtmlReportWriter, JSON report I/O
├── recipes/        53 custom OpenRewrite ScanningRecipes (detection)
├── refactoring/    11 OpenRewrite Recipes (code transformation)
├── adapters/       8 FindingSource implementations (PMD, Checkstyle, SpotBugs,
│                   CPD, JaCoCo, Surefire, Dependency Updates, OpenRewrite)
├── plugin/         Gradle plugin, tasks, extension DSL
└── build-logic/    Convention plugins
```

## Usage

```kotlin
plugins {
    id("io.github.fiftieshousewife.cleancode") version "0.2.1"
}
```

### Tasks

| Task | Group | What it does |
|---|---|---|
| `analyseCleanCode` | verification | Run all detectors against the module; emit `build/reports/clean-code/findings.{html,json}` plus the boxed text summary shown under [Sample Output](#sample-output). |
| `cleanCodeSummary` | verification | *(root only)* Aggregate every module's `findings.json` into `docs/reports/index.html` and the deterministic `docs/reports/SUMMARY.md`. |
| `generateClaudeMd` | verification | Regenerate `CLAUDE.md` from the latest findings + skill registry. Run after upgrading the plugin so newly added skills appear. |
| `cleanCodeFixPlan` | verification | Group findings by file into per-class fix briefs for agent handoff. |
| `cleanCodeBaseline` | verification | Snapshot current findings as `clean-code-baseline.json`. |
| `cleanCodeExplain` | help | Print skill guidance for a finding code, e.g. `cleanCodeExplain --finding=error-handling`. |
| `cleanCodeServe` | clean code | Long-running interactive triage UI at `http://localhost:7070` (see below). |
| `cleanCodeStop` | clean code | Stop a running `cleanCodeServe` daemon. |
| `updateVersionCatalog` | verification | *(root only)* Rewrite `gradle/libs.versions.toml` with non-major updates from the `dependencyUpdates` report. |

The `analyseCleanCode` text summary (per-module banner, sources breakdown, total) is what the [Sample Output](#sample-output) section shows. Run `./gradlew analyseCleanCode` on any module to reproduce it; on this repo run `./scripts/dogfood.sh` to apply the plugin to every module and aggregate.

### Interactive triage — `cleanCodeServe`

Long-running task that runs the analysis, opens the report at `http://localhost:7070`, and accepts in-page batched changes:

| Action | Where | What it does |
|---|---|---|
| 🔇 **Suppress** | per finding row | inserts `// CleanCode-suppress CODE: <reason>` + `@SuppressWarnings("CleanCode:CODE")` above the enclosing method/class |
| ❌ **Disable** | per code section | appends the code to `cleanCode.disabledRecipes` in `build.gradle.kts` |
| ⚙️ **Tune** | per code section (when configurable) | updates the matching threshold in `cleanCode.thresholds { ... }` |

Click any action → reason modal (≥ 5 chars, required) → entry is staged in the browser's `localStorage`. The staging bar at the top shows pending count and offers **Confirm & apply** (POSTs the batch) or **Discard**. On confirm the server applies all edits, re-runs the analysis, and the page reloads with fresh state.

Notes:
- Server binds to `127.0.0.1` only; the apply endpoint mutates files on disk so off-host requests are refused.
- If you reopen `findings.html` cold (no server running), the triage buttons are disabled with a tooltip pointing back to `./gradlew cleanCodeServe`.
- Existing `@SuppressWarnings` annotations are merged into, not duplicated. The single-string form is upgraded to array form when a new code is added.
- Configurable port via `cleanCode.servePort = 8080`. Press `Ctrl-C` in the terminal to stop the server.

The plugin automatically applies `java`, `pmd`, `checkstyle`, `jacoco`, and `com.github.spotbugs`. It provides a bundled Checkstyle configuration if the project has none, and wires `analyseCleanCode` to depend on all tool report tasks.

### Opt-in formatter enforcement

Set `cleanCode.enforceFormatting = true` to apply the Spotless plugin with Google Java Format (AOSP) to every `src/**/*.java` source set. Intended for projects ready to commit to a single formatter — running it on an older codebase will reformat many files at once. Once enabled, `./gradlew check` fails if any file drifts from the style; `./gradlew spotlessApply` fixes it.

## Apply to another project

The SpotBugs Gradle plugin is bundled into the Clean Code plugin jar, so consumers
do not need `gradlePluginPortal()` in their `pluginManagement.repositories`.
`mavenLocal()` plus `mavenCentral()` is enough.

```bash
# From this repo: publish the plugin to your local Maven repo
./gradlew publishToMavenLocal
```

In the target project's `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
```

In the target project's `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.fiftieshousewife.cleancode") version "0.2.1"
}
```

## Build

```bash
./gradlew build                 # compile + run all tests
./gradlew publishToMavenLocal   # publish all modules to ~/.m2
```

Consumers need JDK 21+. Plugin classes are emitted with `--release 21` so any Gradle build on JDK 21 or newer can apply the plugin. Building this repo uses the JDK 25 toolchain (auto-provisioned by Gradle). Uses Gradle 9.4 with version catalog (`gradle/libs.versions.toml`).

## Testing

```bash
./gradlew test                  # run all unit tests
./gradlew :plugin:test          # run plugin TestKit tests only
./gradlew :recipes:test         # run recipe tests only
./gradlew :refactoring:test     # run refactoring recipe tests only
```

The plugin module includes Gradle TestKit tests that verify plugin application, task registration, CPD end-to-end detection, skill file scaffolding, and threshold refresh behaviour.

Recipe tests use OpenRewrite's `RewriteTest` harness to verify detection and transformation accuracy against inline Java source fixtures.

## Refactoring Recipes

The `refactoring` module contains OpenRewrite recipes that **transform** code, not just detect problems:

| Recipe | Fixes | What it does |
|--------|-------|--------------|
| AddFinalRecipe | G22 | Adds `final` to non-reassigned local variables |
| `org.openrewrite.java.RemoveUnusedImports` | G12/J1 | (upstream) Removes unused imports, expands star imports |
| `org.openrewrite.java.ShortenFullyQualifiedTypeReferences` | G12 | (upstream) Replaces inline `org.pkg.Foo` with `Foo` + an import statement |
| ExtractConstantRecipe | G25 | Adds `private static final` for repeated string literals |
| ReduceVisibilityRecipe | T1/Ch3.1 | Changes `private` to package-private for testability |
| RecordToLombokValueRecipe | F1 | Converts large records to `@Value @Builder` classes |
| ExtractExplanatoryVariableRecipe | G19/G28 | Extracts complex if-conditions to named variables |
| EncapsulateBoundaryRecipe | G33 | Adds named variable for `.length - 1` / `.size() - 1` |
| MoveDeclarationRecipe | G10 | Moves local variable declarations closer to first use |
| RemoveNestedTernaryRecipe | G16 | Converts nested ternary to if/else chains |
| WrapAssertAllRecipe | T1 | Wraps consecutive assertions in `assertAll` |
| AddLocaleRecipe | G26 | Adds `Locale.ROOT` to `toLowerCase()`/`toUpperCase()` |
| ExtractClassConstantRecipe | G35 | Promotes repeated numeric literals to `private static final` fields |
| InvertNegativeConditionalRecipe | G29 | Rewrites `if (!cond) A else B` as `if (cond) B else A` |
| SplitFlagArgumentRecipe | F3 | Emits `<name>When<Flag>()` / `<name>When<Flag>IsFalse()` helpers next to a private method whose sole boolean parameter drives a single if/else |
| RenameShortNameRecipe | N5 | Renames short non-loop variable names using a user-supplied `Map<String, String>` |

## Dependencies

| Library                | Version | Used by                    |
|------------------------|---------|----------------------------|
| JUnit Jupiter          | 5.14.4  | All modules (test)         |
| JavaParser             | 3.28.0  | core (SuppressionIndex)    |
| Gson                   | 2.14.0  | core, adapters (JSON I/O)  |
| OpenRewrite            | 8.81.3  | recipes, refactoring, adapters |
| SpotBugs Gradle Plugin | 6.5.4   | plugin                     |
| PMD                    | 7.24.0  | plugin (analyzer)          |
| Checkstyle             | 10.26.1 | plugin (analyzer)          |
| JaCoCo                 | 0.8.14  | plugin (analyzer)          |
| Ben-Manes Versions     | 0.53.0  | build-logic                |

## References

Robert C. Martin, *Clean Code: A Handbook of Agile Software Craftsmanship*, Prentice Hall, 2008.

| Chapter | Pages   | Topics                                              |
|---------|---------|-----------------------------------------------------|
| Ch.3    | p.31-52 | Function size, arguments, flag arguments             |
| Ch.7    | p.103-112 | Exceptions vs return codes, null handling           |
| Ch.10   | p.135-151 | Class size, single responsibility principle         |
| Ch.17   | p.285-314 | The complete taxonomy of 66 code smells            |

## This Project's Code Cleanliness Index

The plugin analyses its own codebase. Each module report includes clickable links to the source at the exact line of each finding.

**Self-analysis summary (auto-generated):** [docs/reports/SUMMARY.md](docs/reports/SUMMARY.md) — total + per-module + per-code counts. Deterministic (no paths, no timestamps), so local and CI runs produce byte-identical output. Regenerated by `./gradlew cleanCodeSummary` (root project only); CI fails if it drifts. The browser-friendly counterpart is [docs/reports/index.html](https://htmlpreview.github.io/?https://github.com/fiftiesHousewife/Clean-Claude/blob/main/docs/reports/index.html). Per-module HTML reports are gitignored — uploaded as the `dogfood-report` CI artifact and regenerated locally for browser viewing.

Key context when reading the numbers:

- Counts are **post-`@SuppressCleanCode`**; `recipes/` and `refactoring/` packages are annotated, which hides most of their findings.
- **E1 findings** (outdated dependencies) are emitted only at the Gradle root — sub-modules skip them once the catalog is anchored at `gradle/libs.versions.toml`.

Regenerate locally with (self-applied via init script, no changes to committed build files):

```bash
./scripts/dogfood.sh
```

The script publishes all modules to mavenLocal, runs `analyseCleanCode` against every Java module via the init script, copies each module's `findings.html` into `docs/reports/`, and regenerates `docs/reports/index.html` via `cleanCodeSummary`. CI runs the same script and fails if `docs/reports/` drifts from the committed state, so the summary on this README always matches `main`.

When you've intentionally changed something that shifts finding counts (a rule edit, a new recipe), pass `--update-baseline` to stage the new `SUMMARY.md` and `index.html` for commit instead of failing:

```bash
./scripts/dogfood.sh --update-baseline
```

## Skills

The plugin ships ten domain skills (`clean-code-functions`, `clean-code-classes`, `clean-code-naming`, `clean-code-comments-and-clutter`, `clean-code-conditionals-and-expressions`, `clean-code-exception-handling`, `clean-code-null-handling`, `clean-code-java-idioms`, `clean-code-test-quality`, `clean-code-project-conventions`) plus the umbrella `clean-code` skill. Claude Code auto-discovers them from `.claude/skills/`, and the `cleanCodeFixPlan` briefs route findings to the correct one via `SkillPathRegistry`.
