# Clean Code Plugin

[![CI](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml/badge.svg)](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Gradle](https://img.shields.io/badge/Gradle-9.0-blue)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

A Gradle plugin that detects violations of Robert C. Martin's *Clean Code* heuristics across a Java codebase. It combines static analysis tools (PMD, Checkstyle, SpotBugs, JaCoCo) with 53 custom OpenRewrite detection recipes and 11 refactoring recipes, normalises all findings into Martin's taxonomy, and produces linked HTML reports with book references and prescriptive guidance.

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
        methodBlankLineSections = 5       // default 4
        privateMethodMinLines = 8         // default 5
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

    packageSuppressions = mapOf(
        "org.fiftieshousewife.cleancode.recipes" to listOf("G5", "Ch7_2")
    )

    claudeReview {                             // opt-in LLM assessment (requires ANTHROPIC_API_KEY)
        enabled.set(true)                      // default: false
        model.set("claude-sonnet-4-6")         // default
        maxFilesPerRun.set(50)                 // default
        codes.set(listOf("G6", "G20", "N4"))   // default — the 3 codes needing semantic judgement
    }
}
```

## Suppressions

Three complementary mechanisms, from narrowest to broadest:

| Mechanism | Where it lives | Scope |
|---|---|---|
| `@SuppressCleanCode({...}, reason="...")` | Method, type, constructor | Exact block, source-anchored |
| `@SuppressCleanCode({...}, reason="...")` on `package-info.java` | Package | Every file in that package (CPD cross-file pairs too) |
| `cleanCode.packageSuppressions = mapOf(...)` | Gradle build script | Package, config-driven — fallback for findings without a source anchor |
| `cleanCode.disabledRecipes = listOf(...)` | Gradle build script | Heuristic code, project-wide |

Prefer the annotation. Reasons live next to the code, get reviewed in PRs, and can carry `until="YYYY-MM-DD"` so the suppression expires and reappears as a finding.

Example — suppress CPD duplication and null-density across a whole package:

```java
@SuppressCleanCode(
    value = { HeuristicCode.G5, HeuristicCode.Ch7_2 },
    reason = "OpenRewrite visitor pattern produces structurally similar scanners and "
            + "relies on null returns to signal no-change — API-imposed, not design flaws"
)
package org.fiftieshousewife.cleancode.recipes;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.annotations.SuppressCleanCode;
```

This repo applies exactly that to `recipes/` and `refactoring/`, together with JSpecify `@NullMarked` at the package level so IDE nullability inspections match the OpenRewrite API contract.

Gaps worth knowing:
- `E1` (outdated-dependency findings) have no source anchor; `@SuppressCleanCode` cannot suppress them. Use `disabledRecipes = listOf("E1")` if noise from dependency reports is unwanted.
- When a suppression expires (`until` date in the past), the index emits a meta-finding pointing at the annotation so it surfaces in the next report.

## Architecture

```
CleanClaude/
├── annotations/    HeuristicCode enum, @SuppressCleanCode annotation
├── core/           Finding, AggregatedReport, BuildOutputFormatter,
│                   HeuristicDescriptions, SuppressionIndex, BaselineManager,
│                   ClaudeMdGenerator, HtmlReportWriter, JSON report I/O
├── recipes/        53 custom OpenRewrite ScanningRecipes (detection)
├── refactoring/    11 OpenRewrite Recipes (code transformation)
├── adapters/       9 FindingSource implementations (PMD, Checkstyle, SpotBugs,
│                   CPD, JaCoCo, Surefire, Dependency Updates, OpenRewrite, Claude Review)
├── claude-review/  Claude API FindingSource for G6/G20/N4 (opt-in)
├── plugin/         Gradle plugin, tasks, extension DSL
└── build-logic/    Convention plugins
```

## Usage

```kotlin
plugins {
    id("org.fiftieshousewife.cleancode") version "1.0-SNAPSHOT"
}
```

```bash
./gradlew analyseCleanCode                           # full analysis with linked HTML report
./gradlew cleanCodeFixPlan                           # per-file fix briefs for agent handoff
./gradlew generateClaudeMd                           # generate CLAUDE.md
./gradlew cleanCodeBaseline                          # snapshot baseline
./gradlew cleanCodeExplain --finding=error-handling  # print skill guidance
```

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
    id("org.fiftieshousewife.cleancode") version "1.0-SNAPSHOT"
}
```

## Build

```bash
./gradlew build                 # compile + run all tests
./gradlew publishToMavenLocal   # publish all modules to ~/.m2
```

Requires Java 21. Uses Gradle 9.0 with version catalog (`gradle/libs.versions.toml`).

**Important:** The Gradle daemon must run on JDK 21. OpenRewrite 8.x uses internal `com.sun.tools.javac` APIs that were removed in JDK 25, causing `NoClassDefFoundError: com/sun/tools/javac/code/Type$UnknownType` at parse time. PMD, Checkstyle, SpotBugs, and JaCoCo still work on newer JDKs, but OpenRewrite recipe findings will be missing. Set `JAVA_HOME` to JDK 21 before running:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
./gradlew analyseCleanCode
```

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
| DeleteUnusedImportRecipe | G12/J1 | Removes unused imports, expands star imports |
| ExtractConstantRecipe | G25 | Adds `private static final` for repeated string literals |
| ReduceVisibilityRecipe | T1/Ch3.1 | Changes `private` to package-private for testability |
| RecordToLombokValueRecipe | F1 | Converts large records to `@Value @Builder` classes |
| ExtractExplanatoryVariableRecipe | G19/G28 | Extracts complex if-conditions to named variables |
| EncapsulateBoundaryRecipe | G33 | Adds named variable for `.length - 1` / `.size() - 1` |
| MoveDeclarationRecipe | G10 | Moves local variable declarations closer to first use |
| RemoveNestedTernaryRecipe | G16 | Converts nested ternary to if/else chains |
| WrapAssertAllRecipe | T1 | Wraps consecutive assertions in `assertAll` |
| AddLocaleRecipe | G26 | Adds `Locale.ROOT` to `toLowerCase()`/`toUpperCase()` |

## Dependencies

| Library                | Version | Used by                    |
|------------------------|---------|----------------------------|
| JUnit Jupiter          | 5.10.0  | All modules (test)         |
| JavaParser             | 3.26.2  | core (SuppressionIndex)    |
| Gson                   | 2.11.0  | core, adapters (JSON I/O)  |
| OpenRewrite            | 8.40.2  | recipes, refactoring, adapters |
| Anthropic Java SDK     | 2.25.0  | claude-review              |
| SpotBugs Gradle Plugin | 6.5.0   | plugin                     |
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

Pre-suppression counts (captured as `experiment/baseline/*.json` before package-level `@SuppressCleanCode` on `recipes/` and `refactoring/`):

| Module | Report | Errors | Warnings | Info |
|--------|--------|-------:|---------:|-----:|
| [annotations](annotations/) | [view report](https://htmlpreview.github.io/?https://github.com/fiftiesHousewife/Clean-Claude/blob/main/docs/reports/annotations.html) | 0 | 11 | 1 |
| [core](core/) | [view report](https://htmlpreview.github.io/?https://github.com/fiftiesHousewife/Clean-Claude/blob/main/docs/reports/core.html) | 0 | 55 | 1 |
| [adapters](adapters/) | [view report](https://htmlpreview.github.io/?https://github.com/fiftiesHousewife/Clean-Claude/blob/main/docs/reports/adapters.html) | 0 | 53 | 1 |
| [claude-review](claude-review/) | [view report](https://htmlpreview.github.io/?https://github.com/fiftiesHousewife/Clean-Claude/blob/main/docs/reports/claude-review.html) | 1 | 20 | 0 |
| [plugin](plugin/) | [view report](https://htmlpreview.github.io/?https://github.com/fiftiesHousewife/Clean-Claude/blob/main/docs/reports/plugin.html) | 1 | 32 | 0 |
| [recipes](recipes/) | [view report](https://htmlpreview.github.io/?https://github.com/fiftiesHousewife/Clean-Claude/blob/main/docs/reports/recipes.html) | 0 | 131 | 1 |
| [refactoring](refactoring/) | [view report](https://htmlpreview.github.io/?https://github.com/fiftiesHousewife/Clean-Claude/blob/main/docs/reports/refactoring.html) | 0 | 32 | 1 |

Current counts are after the `experiment/manual-pilot` run and the package-level `@SuppressCleanCode` on `recipes/` and `refactoring/`. A clean baseline from `experiment/manual-1` will replace these once that run lands. Regenerate locally with (self-applied via init script, no changes to committed build files):

```bash
./gradlew publishToMavenLocal
./gradlew --init-script scripts/cleancode-dogfood.init.gradle.kts analyseCleanCode
```

## Experiment: Manual vs Recipe-Assisted Fix

The project includes token monitoring hooks and a structured experiment plan to compare the cost of fixing all findings manually vs using the refactoring recipes first.

**Protocol:** 6 runs total — 3 manual fix sessions, 3 recipe-assisted sessions. Each starts from the same commit, uses a clean Claude Code session, and saves a git patch + token logs.

**Metrics compared:**
- Total tokens consumed
- Number of tool calls and turns
- Cache hit ratio
- Patch size and findings remaining

### Skills

Slash-command skills that drive the workflow:

| Skill | Usage | Purpose |
|---|---|---|
| `/clean-code` | `/clean-code` | Apply the plugin to any project, generate briefs, delegate per-file fixes to agents |
| `/experiment` | `/experiment manual 1` | Create branch, clear logs, print the fix prompt |
| `/experiment-save` | `/experiment-save` | Save patch + token logs after a run |
| `/experiment-analyse` | `/experiment-analyse` | Compare all runs and write `experiment/analysis.md` |

The plugin also ships ten domain skills (`clean-code-functions`, `clean-code-classes`, `clean-code-naming`, `clean-code-comments-and-clutter`, `clean-code-conditionals-and-expressions`, `clean-code-exception-handling`, `clean-code-null-handling`, `clean-code-java-idioms`, `clean-code-test-quality`, `clean-code-project-conventions`). Claude Code auto-discovers them from `.claude/skills/` and the `cleanCodeFixPlan` briefs route findings to the correct one via `SkillPathRegistry`.

### Running a single experiment (scripted, one command)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
scripts/run-experiment.sh manual 1
```

The script creates the branch, regenerates per-file fix briefs, invokes `claude -p` non-interactively with the fix prompt, and saves the patch + token usage JSON to `experiment/`. Suitable for `cron` or the `schedule` skill — no interactive input required.

#### Live feedback

The main terminal shows the gradle output and Claude's streamed responses. For a skimmable view of tool activity, open a second terminal:

```bash
tail -f .claude/tool-log.jsonl | jq -r '"\(.tool) \(.detail // "")"'
```

To track commit accumulation on the experiment branch, a third terminal:

```bash
while sleep 60; do
  git -C /path/to/CleanClaude log --oneline experiment/manual-1 ^main | wc -l
done
```

Stop the run with Ctrl-C in the main terminal.

### Running a single experiment (interactive)

```bash
# 1. In a Claude Code session, set up the run:
/experiment manual 1

# 2. Exit, then start a fresh session with the task label:
CLAUDE_TASK_LABEL="manual-fix-1" claude

# 3. Paste the fix prompt (printed by /experiment) and let it run

# 4. When done, save outputs:
/experiment-save

# 5. Return to main and repeat for the next run
git checkout main
```

### Scheduling overnight runs

Any scheduler that can invoke a shell command works. With the `schedule` skill inside Claude Code:

```
/schedule create --cron "30 23 * * *" --command "scripts/run-experiment.sh manual 1"
```

Or from `cron`:

```
30 23 * * * cd /path/to/CleanClaude && scripts/run-experiment.sh manual 1 > experiment/manual-1.log 2>&1
```

### Analysis

After all 6 runs, invoke `/experiment-analyse` to generate a comparison report at `experiment/analysis.md`.
