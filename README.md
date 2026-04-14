# Clean Code Plugin

A Gradle plugin that detects Clean Code violations across a Java codebase using multiple static analysis tools, normalises all findings into a single data model, and generates CLAUDE.md reports with skill file pointers for AI-assisted remediation.

Based on Robert C. Martin's *Clean Code* heuristics (C1-C5, E1-E2, F1-F4, G1-G36, N1-N7, T1-T9) plus chapter-specific patterns (Ch3, Ch6, Ch7, Ch10).

## Architecture

```
CleanClaude/
├── annotations/   HeuristicCode enum, @SuppressCleanCode annotation
├── core/          Finding, FindingSource, AggregatedReport, SuppressionIndex,
│                  FindingFilter, FindingAggregator, BaselineManager,
│                  ClaudeMdGenerator, JSON report I/O
├── recipes/       OpenRewrite ScanningRecipes for custom pattern detection
├── adapters/      FindingSource implementations per tool + OpenRewriteFindingSource
├── plugin/        Gradle plugin, tasks, extension DSL
└── build-logic/   Convention plugins (cleancode.java-conventions, cleancode.java-library)
```

## Modules

### annotations

Pure Java module containing the `HeuristicCode` enum (76 values covering all Martin heuristics, chapter codes, Java codes, and meta codes) and the `@SuppressCleanCode` annotation (`@Repeatable`, `RetentionPolicy.SOURCE`, mandatory `reason()`).

### core

The domain layer:

- **Finding** — immutable record normalising all tool output into a single type
- **FindingSource** — interface each adapter implements
- **AggregatedReport** — collects findings with `byCode()` and `bySeverity()` grouping
- **SuppressionIndex** — scans source with JavaParser for `@SuppressCleanCode` annotations, tracks expiry dates, produces meta-findings for expired/blank suppressions
- **FindingFilter** — post-filter removing suppressed findings (SpotBugs excluded — it handles its own suppression natively)
- **FindingAggregator** — orchestrates all FindingSources into an AggregatedReport
- **BaselineManager** — writes/reads baseline snapshots, computes deltas
- **ClaudeMdGenerator** — generates/updates CLAUDE.md with finding sections, preserves `<!-- ANNOTATE -->` blocks across regenerations, includes delta tables and skill pointers
- **JsonReportWriter/Reader** — Gson-based serialisation of AggregatedReport

### recipes

Custom OpenRewrite `ScanningRecipe` implementations detecting patterns that off-the-shelf tools miss:

| Recipe | Code | Detects |
|--------|------|---------|
| FlagArgumentRecipe | F3 | Boolean params on non-private methods |
| CatchLogContinueRecipe | Ch7.1 | Catch blocks that only log or are empty |
| LawOfDemeterRecipe | G36 | Method chains of depth >= 3 |
| NegativeConditionalRecipe | G29 | Double negation (`!isNotEmpty()`) |
| EncapsulateConditionalRecipe | G28 | Complex conditions with 2+ logical operators |
| NullDensityRecipe | Ch7.2 | Methods with >= 3 null checks |
| ClassLineLengthRecipe | Ch10.1 | Classes exceeding 150 lines |
| DisabledTestRecipe | T3/T4 | `@Disabled`/`@Ignore` without meaningful reason |

### adapters

One `FindingSource` implementation per static analysis tool, each parsing XML reports and mapping tool-specific rules to `HeuristicCode` values:

| Adapter | Tool | Parses | Key mappings |
|---------|------|--------|-------------|
| PmdFindingSource | PMD | `pmd/main.xml` | 17 rules -> G30, F4, G9, G12, G8, C5, G4, G23, G17, J2 |
| CheckstyleFindingSource | Checkstyle | `checkstyle/main.xml` | 18 checks -> F1, G25, N1, G30, J1, G12, J2, G8, G18, G28, G24, G10 |
| SpotBugsFindingSource | SpotBugs | `spotbugs/main.xml` | Category/type pairs -> G4, G18, G9, G26, G8 |
| CpdFindingSource | CPD | `cpd/cpd.xml` | All duplication -> G5, severity by token count |
| JacocoFindingSource | JaCoCo | `jacoco/test/jacocoTestReport.xml` | Coverage -> T1, T2, T8 |
| SurefireFindingSource | Surefire | `surefire-reports/TEST-*.xml` | Timing/skips -> T3, T4, T9 |
| OpenRewriteFindingSource | OpenRewrite | Inline scan | Runs all custom recipes |

### plugin

Gradle plugin providing:

- **analyseCleanCode** — runs all FindingSources, writes `findings.json`
- **generateClaudeMd** — generates CLAUDE.md from findings
- **cleanCodeBaseline** — snapshots current findings as baseline
- **cleanCodeExplain** — prints skill file content to terminal

## Build

```bash
./gradlew build       # compile all modules
./gradlew test        # run all 197 tests
```

Requires Java 21. Uses Gradle 9.0 with version catalog (`gradle/libs.versions.toml`).

## Usage

```kotlin
plugins {
    id("org.fiftieshousewife.cleancode")
}
```

```bash
./gradlew analyseCleanCode      # analyse and write findings.json
./gradlew generateClaudeMd      # generate CLAUDE.md
./gradlew cleanCodeBaseline     # snapshot baseline
./gradlew cleanCodeExplain --finding=error-handling  # print skill guidance
```

## Dependencies

| Library | Version | Used by |
|---------|---------|---------|
| JUnit Jupiter | 5.10.0 | All modules (test) |
| JavaParser | 3.26.2 | core (SuppressionIndex) |
| Gson | 2.11.0 | core (JSON report I/O) |
| OpenRewrite | 8.40.2 | recipes, adapters |
