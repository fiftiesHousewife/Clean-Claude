# Clean Code Plugin

[![CI](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml/badge.svg)](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Gradle](https://img.shields.io/badge/Gradle-9.0-blue)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

A Gradle plugin that detects violations of Robert C. Martin's *Clean Code* heuristics across a Java codebase. It combines static analysis tools (PMD, Checkstyle, SpotBugs, JaCoCo) with 23 custom OpenRewrite recipes, normalises all findings into Martin's taxonomy, and produces human-readable output with book references and prescriptive guidance.

> *"Clean code reads like well-written prose."* -- Robert C. Martin, *Clean Code* (2008)

## Heuristic Coverage

Every finding is mapped to a specific heuristic from Chapter 17 of *Clean Code* ("Smells and Heuristics") or to a chapter-specific pattern. The plugin currently detects:

### Comments (Ch.17 p.286-287)
| Code | Name | Detection |
|------|------|-----------|
| C3 | Redundant Comment | MumblingCommentRecipe -- comments that restate the method name or parameters |
| C5 | Commented-Out Code | CommentedCodeRecipe -- commented-out code blocks that should be deleted |

### Environment (Ch.17 p.287)
| Code | Name | Detection |
|------|------|-----------|
| E1 | Build Requires More Than One Step | DependencyUpdatesFindingSource -- outdated dependencies (minor versions) |

### Functions (Ch.17 p.288)
| Code | Name | Detection |
|------|------|-----------|
| F1 | Too Many Arguments | Checkstyle ParameterNumber check |
| F2 | Output Arguments | OutputArgumentRecipe -- methods that mutate collection arguments |
| F3 | Flag Arguments | FlagArgumentRecipe -- boolean parameters on non-private methods |
| F4 | Dead Function | PMD UnusedPrivateMethod rule |

### General (Ch.17 p.288-306)
| Code | Name | Detection |
|------|------|-----------|
| G4 | Overridden Safeties | PMD EmptyCatchBlock, SpotBugs DE_MIGHT_IGNORE |
| G5 | Duplication | CPD token-based duplication detection |
| G8 | Too Much Information | PMD ExcessivePublicCount, CouplingBetweenObjects, TooManyFields |
| G9 | Dead Code | PMD UnusedLocalVariable, SpotBugs UUF_UNUSED_FIELD |
| G10 | Vertical Separation | VerticalSeparationRecipe -- variables declared far from first use |
| G12 | Clutter | PMD UnusedImports, Checkstyle UnusedImports/RedundantImport |
| G20 | Function Names Should Say What They Do | PMD UseLocaleWithCaseConversions |
| G22 | Make Logical Dependencies Physical | Checkstyle FinalLocalVariable |
| G23 | Prefer Polymorphism to If/Else or Switch/Case | SwitchOnTypeRecipe + StringSwitchRecipe |
| G24 | Follow Standard Conventions | Checkstyle LeftCurly/RightCurly/LineLength |
| G25 | Replace Magic Numbers with Named Constants | MagicStringRecipe -- duplicated string literals |
| G28 | Encapsulate Conditionals | EncapsulateConditionalRecipe -- complex boolean conditions |
| G29 | Avoid Negative Conditionals | NegativeConditionalRecipe -- double negation |
| G30 | Functions Should Do One Thing | WhitespaceSplitMethodRecipe -- methods with multiple blank-line sections |
| G34 | Functions Should Descend Only One Level | SectionCommentRecipe -- section comment banners |
| G36 | Avoid Transitive Navigation | LawOfDemeterRecipe -- method chains (fluent APIs excluded) |

### Java (Ch.17 p.307-308)
| Code | Name | Detection |
|------|------|-----------|
| J1 | Avoid Long Import Lists | Checkstyle AvoidStarImport |
| J2 | Don't Inherit Constants | InheritConstantsRecipe -- constant-only interface implementation |
| J3 | Constants versus Enums | EnumForConstantsRecipe -- static final groups that should be enums |

### Names (Ch.17 p.309-313)
| Code | Name | Detection |
|------|------|-----------|
| N1 | Choose Descriptive Names | Checkstyle LocalVariableName/MethodName/TypeName |
| N5 | Use Long Names for Long Scopes | ShortVariableNameRecipe -- single-letter names outside loops/lambdas |
| N6 | Avoid Encodings | EncodingNamingRecipe -- Hungarian notation and type prefixes |

### Tests (Ch.17 p.313-314)
| Code | Name | Detection |
|------|------|-----------|
| T1 | Insufficient Tests | JaCoCo line coverage analysis |
| T2 | Use a Coverage Tool | JaCoCo report presence check |
| T3 | Don't Skip Trivial Tests | DisabledTestRecipe -- @Disabled/@Ignore without reason |
| T4 | An Ignored Test Is a Question | DisabledTestRecipe -- disabled tests signal ambiguity |
| T8 | Test Coverage Patterns | JaCoCo per-class coverage analysis |
| T9 | Tests Should Be Fast | Surefire test timing analysis |

### Chapter-Specific Patterns
| Code | Name | Reference | Detection |
|------|------|-----------|-----------|
| Ch3.1 | Small Functions | Ch.3 p.34 | PrivateMethodTestabilityRecipe -- non-trivial private methods |
| Ch7.1 | Use Exceptions Rather Than Return Codes | Ch.7 p.103 | CatchLogContinueRecipe -- swallowed exceptions |
| Ch7.2 | Don't Return Null | Ch.7 p.110 | NullDensityRecipe + SpotBugs redundant null checks |
| Ch10.1 | Classes Should Be Small | Ch.10 p.136 | ClassLineLengthRecipe -- classes exceeding 150 lines |
| Ch10.2 | The Single Responsibility Principle | Ch.10 p.138 | LargeRecordRecipe -- records with too many components |

## Sample Output

```
═══════════════════════════════════════════════════════════════════════════
  CLEAN CODE ANALYSIS  —  my-project
═══════════════════════════════════════════════════════════════════════════

  1 errors  ·  18 warnings  ·  2 info

───────────────────────────────────────────────────────────────────────────
  Ch7_1: Use Exceptions Rather Than Return Codes (1)
  Clean Code Ch.7 'Error Handling' p.103

  Exceptions are for exceptional circumstances. When you catch an
  exception and merely log it — or worse, leave the catch block empty —
  you've told the calling code that everything is fine when it isn't.
  The caller makes decisions based on a lie.

     ! UserService.java  Catch block in 'save' only logs or is empty

───────────────────────────────────────────────────────────────────────────
  F3: Flag Arguments (1)
  Clean Code Ch.17 'Smells and Heuristics — Functions' p.288

  Boolean arguments loudly declare that the function does more than one
  thing. It does one thing if the flag is true and another if the flag
  is false. Split it into two methods.

     ! OrderController.java  Method 'list' takes boolean parameter 'verbose' — split into two methods instead

───────────────────────────────────────────────────────────────────────────
  G23: Prefer Polymorphism to If/Else or Switch/Case (1)
  Clean Code Ch.17 'Smells and Heuristics — General' p.299

  When you see code that tests for a type to decide what behaviour to
  invoke, consider replacing it with polymorphism.

     ! PaymentGateway.java  Switch on String 'type' with 5 cases — replace with an enum that encapsulates the behaviour

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

## Architecture

```
CleanClaude/
├── annotations/   HeuristicCode enum, @SuppressCleanCode annotation
├── core/          Finding, AggregatedReport, BuildOutputFormatter,
│                  HeuristicDescriptions, SuppressionIndex, BaselineManager,
│                  ClaudeMdGenerator, JSON report I/O
├── recipes/       23 custom OpenRewrite ScanningRecipes
├── adapters/      8 FindingSource implementations (PMD, Checkstyle, SpotBugs,
│                  CPD, JaCoCo, Surefire, Dependency Updates, OpenRewrite)
├── plugin/        Gradle plugin, tasks, extension DSL
└── build-logic/   Convention plugins (cleancode.java-conventions, cleancode.java-library)
```

## Usage

```kotlin
plugins {
    id("org.fiftieshousewife.cleancode") version "1.0-SNAPSHOT"
}
```

```bash
./gradlew analyseCleanCode                           # full analysis
./gradlew generateClaudeMd                           # generate CLAUDE.md
./gradlew cleanCodeBaseline                          # snapshot baseline
./gradlew cleanCodeExplain --finding=error-handling  # print skill guidance
```

The plugin automatically applies `java`, `pmd`, `checkstyle`, `jacoco`, and `com.github.spotbugs`. It provides a bundled Checkstyle configuration if the project has none, and wires `analyseCleanCode` to depend on all tool report tasks.

## Build

```bash
./gradlew build                 # compile + run all tests
./gradlew publishToMavenLocal   # publish all modules to ~/.m2
```

Requires Java 21. Uses Gradle 9.0 with version catalog (`gradle/libs.versions.toml`).

## Dependencies

| Library | Version | Used by |
|---------|---------|---------|
| JUnit Jupiter | 5.10.0 | All modules (test) |
| JavaParser | 3.26.2 | core (SuppressionIndex) |
| Gson | 2.11.0 | core, adapters (JSON report I/O) |
| OpenRewrite | 8.40.2 | recipes, adapters |
| SpotBugs Gradle Plugin | 6.5.0 | plugin |
| Ben-Manes Versions | 0.53.0 | build-logic |

## References

Robert C. Martin, *Clean Code: A Handbook of Agile Software Craftsmanship*, Prentice Hall, 2008.

- Chapter 3: Functions (p.31-52) -- function size, arguments, flag arguments
- Chapter 7: Error Handling (p.103-112) -- exceptions vs return codes, null handling
- Chapter 10: Classes (p.135-151) -- class size, single responsibility principle
- Chapter 17: Smells and Heuristics (p.285-314) -- the complete taxonomy of 66 code smells
