# Clean Code Plugin

[![CI](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml/badge.svg)](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Gradle](https://img.shields.io/badge/Gradle-9.0-blue)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

A Gradle plugin that detects Clean Code violations across a Java codebase using multiple static analysis tools, normalises all findings into a single data model, and generates CLAUDE.md reports with skill file pointers for AI-assisted remediation.

Based on Robert C. Martin's *Clean Code* heuristics (C1-C5, E1-E2, F1-F4, G1-G36, N1-N7, T1-T9) plus chapter-specific patterns (Ch3, Ch6, Ch7, Ch10).

## Architecture

```
CleanClaude/
├── annotations/   HeuristicCode enum, @SuppressCleanCode annotation
├── core/          Finding, FindingSource, AggregatedReport, SuppressionIndex,
│                  FindingFilter, FindingAggregator, BaselineManager,
│                  ClaudeMdGenerator, JSON report I/O
├── recipes/       18 custom OpenRewrite ScanningRecipes
├── adapters/      8 FindingSource implementations (PMD, Checkstyle, SpotBugs,
│                  CPD, JaCoCo, Surefire, Dependency Updates, OpenRewrite)
├── plugin/        Gradle plugin, tasks, extension DSL
└── build-logic/   Convention plugins (cleancode.java-conventions, cleancode.java-library)
```

## What the plugin does

When you run `./gradlew analyseCleanCode`, the plugin:

1. **Applies** PMD, Checkstyle, SpotBugs, and JaCoCo to the project
2. **Runs** all static analysis tools with sensible defaults (configurable)
3. **Parses** 18 custom OpenRewrite recipes directly against source
4. **Reads** Ben-Manes dependency update reports (if available)
5. **Maps** every finding to a Robert Martin heuristic code
6. **Writes** a consolidated `findings.json` report

## Modules

### annotations

Pure Java module containing the `HeuristicCode` enum (76 values covering all Martin heuristics, chapter codes, Java codes, and meta codes) and the `@SuppressCleanCode` annotation (`@Repeatable`, `RetentionPolicy.SOURCE`, mandatory `reason()`).

### core

The domain layer:

- **Finding** -- immutable record normalising all tool output into a single type
- **FindingSource** -- interface each adapter implements
- **AggregatedReport** -- collects findings with `byCode()` and `bySeverity()` grouping
- **SuppressionIndex** -- scans source with JavaParser for `@SuppressCleanCode` annotations, tracks expiry dates, produces meta-findings for expired/blank suppressions
- **FindingFilter** -- post-filter removing suppressed findings (SpotBugs excluded -- it handles its own suppression natively)
- **FindingAggregator** -- orchestrates all FindingSources into an AggregatedReport
- **BaselineManager** -- writes/reads baseline snapshots, computes deltas
- **ClaudeMdGenerator** -- generates/updates CLAUDE.md with finding sections, preserves `<!-- ANNOTATE -->` blocks across regenerations, includes delta tables and skill pointers
- **JsonReportWriter/Reader** -- Gson-based serialisation of AggregatedReport

### recipes

18 custom OpenRewrite `ScanningRecipe` implementations detecting patterns that off-the-shelf tools miss:

| Recipe | Code | Detects |
|--------|------|---------|
| FlagArgumentRecipe | F3 | Boolean params on non-private methods |
| OutputArgumentRecipe | F2 | Output arguments (mutated collection params) |
| CatchLogContinueRecipe | Ch7.1 | Catch blocks that only log or are empty |
| NullDensityRecipe | Ch7.2 | Methods with >= 3 null checks |
| ClassLineLengthRecipe | Ch10.1 | Classes exceeding 150 lines |
| LargeRecordRecipe | Ch10.2 | Records with too many components |
| LawOfDemeterRecipe | G36 | Method chains of depth >= 3 |
| NegativeConditionalRecipe | G29 | Double negation (`!isNotEmpty()`) |
| EncapsulateConditionalRecipe | G28 | Complex conditions with 2+ logical operators |
| VerticalSeparationRecipe | G10 | Variables declared far from first use |
| SwitchOnTypeRecipe | G23 | Type-switching if/else chains (prefer polymorphism) |
| DisabledTestRecipe | T3/T4 | `@Disabled`/`@Ignore` without meaningful reason |
| CommentedCodeRecipe | C5 | Commented-out code blocks |
| MumblingCommentRecipe | C3 | Incoherent or restating comments |
| SectionCommentRecipe | G34 | Section comment banners in methods |
| EncodingNamingRecipe | N6 | Hungarian notation and type encoding |
| InheritConstantsRecipe | J2 | Constants inherited via interface |
| EnumForConstantsRecipe | J3 | Static final groups that should be enums |

All 18 recipes share a single parsed LST -- source files are parsed once and reused across all recipe runs.

### adapters

8 `FindingSource` implementations, each mapping tool output to `HeuristicCode` values:

| Adapter | Tool | Report format | Key mappings |
|---------|------|---------------|-------------|
| PmdFindingSource | PMD | `pmd/main.xml` | 21 rules -> G30, F4, G9, G12, G8, C5, G4, G23, G17, J2, G20, G22 |
| CheckstyleFindingSource | Checkstyle | `checkstyle/main.xml` | 26 checks -> F1, G25, N1, G30, J1, G12, J2, G8, G18, G28, G24, G10, G22, G4, Ch10.1 |
| SpotBugsFindingSource | SpotBugs | `spotbugs/main.xml` | Category/type pairs -> G4, G18, G9, G26, G8, Ch7.2 |
| CpdFindingSource | CPD | `cpd/cpd.xml` | All duplication -> G5, severity by token count |
| JacocoFindingSource | JaCoCo | `jacoco/test/jacocoTestReport.xml` | Coverage -> T1, T2, T8 |
| SurefireFindingSource | Surefire | `surefire-reports/TEST-*.xml` | Timing/skips -> T3, T4, T9 |
| DependencyUpdatesFindingSource | Ben-Manes | `dependencyUpdates/report.json` | Outdated deps -> E1 (minor versions only) |
| OpenRewriteFindingSource | OpenRewrite | Inline source scan | Runs all 18 custom recipes |

### plugin

Gradle plugin (`org.fiftieshousewife.cleancode`) providing:

- **analyseCleanCode** -- applies PMD, Checkstyle, SpotBugs, JaCoCo; runs all tools; runs 18 OpenRewrite recipes; consolidates findings into `build/reports/clean-code/findings.json`
- **generateClaudeMd** -- generates CLAUDE.md from findings
- **cleanCodeBaseline** -- snapshots current findings as baseline
- **cleanCodeExplain** -- prints skill file content to terminal

The plugin automatically:
- Applies the `java`, `pmd`, `checkstyle`, `jacoco`, and `com.github.spotbugs` plugins
- Configures `ignoreFailures = true` so analysis runs to completion
- Provides a bundled Checkstyle config if the project has none
- Wires `analyseCleanCode` to depend on all tool report tasks
- Picks up Ben-Manes `dependencyUpdates` if the versions plugin is applied

## Sample Output

```
═══════════════════════════════════════════════════════════════
  CLEAN CODE ANALYSIS  —  my-project
═══════════════════════════════════════════════════════════════

  1 errors  ·  18 warnings  ·  2 info

───────────────────────────────────────────────────────────────

  Ch7_1 (1)
     ! UserService.java  Catch block in 'save' only logs or is empty

  F3 (1)
     ! OrderController.java  Boolean parameter 'verbose' on method 'list'

  G23 (1)
     ! PaymentGateway.java  Type switch in 'process': if/else-if chain with type dispatch

  J3 (1)
     ! Status.java  5 static final fields with prefix 'STATUS' should be an enum

  T1 (1)
    !! (project)  Overall line coverage: 42.3% (210/497 lines covered)

───────────────────────────────────────────────────────────────

  Sources:
    openrewrite: 12
    checkstyle: 5
    spotbugs: 3
    pmd: 1
    jacoco: 1

═══════════════════════════════════════════════════════════════
  22 findings  —  Run ./gradlew cleanCodeExplain --finding=<code> for guidance
═══════════════════════════════════════════════════════════════
```

## Build

```bash
./gradlew build                 # compile + run all tests
./gradlew publishToMavenLocal   # publish all modules to ~/.m2
```

Requires Java 21. Uses Gradle 9.0 with version catalog (`gradle/libs.versions.toml`).

## Usage

```kotlin
plugins {
    id("org.fiftieshousewife.cleancode") version "1.0-SNAPSHOT"
}
```

```bash
./gradlew analyseCleanCode                        # full analysis
./gradlew generateClaudeMd                        # generate CLAUDE.md
./gradlew cleanCodeBaseline                       # snapshot baseline
./gradlew cleanCodeExplain --finding=error-handling  # print skill guidance
```

## Dependencies

| Library | Version | Used by |
|---------|---------|---------|
| JUnit Jupiter | 5.10.0 | All modules (test) |
| JavaParser | 3.26.2 | core (SuppressionIndex) |
| Gson | 2.11.0 | core, adapters (JSON report I/O) |
| OpenRewrite | 8.40.2 | recipes, adapters |
| SpotBugs Gradle Plugin | 6.5.0 | plugin |
| Ben-Manes Versions | 0.53.0 | build-logic |
