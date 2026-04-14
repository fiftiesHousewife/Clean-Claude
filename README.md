# Clean Code Plugin

[![CI](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml/badge.svg)](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Gradle](https://img.shields.io/badge/Gradle-9.0-blue)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

A Gradle plugin that detects violations of Robert C. Martin's *Clean Code* heuristics across a Java codebase. It combines static analysis tools (PMD, Checkstyle, SpotBugs, JaCoCo) with 32 custom OpenRewrite recipes, normalises all findings into Martin's taxonomy, and produces human-readable output with book references and prescriptive guidance.

> *"Clean code reads like well-written prose."* -- Robert C. Martin, *Clean Code* (2008)

## Heuristic Coverage

Every finding is mapped to a specific heuristic from *Clean Code* Chapter 17 ("Smells and Heuristics") or to a chapter-specific pattern. The plugin currently detects:

| Code   | Name                                        | Reference       | Detection                                                        |
|--------|---------------------------------------------|-----------------|------------------------------------------------------------------|
| C3     | Redundant Comment                           | Ch.17 p.286     | MumblingCommentRecipe                                            |
| C5     | Commented-Out Code                          | Ch.17 p.287     | CommentedCodeRecipe                                              |
| E1     | Build Requires More Than One Step           | Ch.17 p.287     | DependencyUpdatesFindingSource                                   |
| F1     | Too Many Arguments                          | Ch.17 p.288     | Checkstyle ParameterNumber                                       |
| F2     | Output Arguments                            | Ch.17 p.288     | OutputArgumentRecipe                                             |
| F3     | Flag Arguments                              | Ch.17 p.288     | FlagArgumentRecipe                                               |
| F4     | Dead Function                               | Ch.17 p.288     | PMD UnusedPrivateMethod                                          |
| G4     | Overridden Safeties                         | Ch.17 p.289     | UncheckedCastRecipe, PMD EmptyCatchBlock, SpotBugs               |
| G5     | Duplication                                 | Ch.17 p.289     | CPD token-based detection                                        |
| G8     | Too Much Information                        | Ch.17 p.291     | VisibilityReductionRecipe, PMD ExcessivePublicCount              |
| G9     | Dead Code                                   | Ch.17 p.292     | PMD UnusedLocalVariable, SpotBugs UUF_UNUSED_FIELD               |
| G10    | Vertical Separation                         | Ch.17 p.292     | VerticalSeparationRecipe                                         |
| G11    | Inconsistency                               | Ch.17 p.292     | InconsistentNamingRecipe                                         |
| G12    | Clutter                                     | Ch.17 p.293     | PMD/Checkstyle UnusedImports, RedundantImport                    |
| G14    | Feature Envy                                | Ch.17 p.293     | FeatureEnvyRecipe                                                |
| G16    | Obscured Intent                             | Ch.17 p.295     | NestedTernaryRecipe                                              |
| G19    | Use Explanatory Variables                   | Ch.17 p.296     | MissingExplanatoryVariableRecipe                                 |
| G20    | Function Names Should Say What They Do      | Ch.17 p.297     | PMD UseLocaleWithCaseConversions                                 |
| G22    | Make Logical Dependencies Physical          | Ch.17 p.298     | Checkstyle FinalLocalVariable                                    |
| G23    | Prefer Polymorphism to If/Else or Switch    | Ch.17 p.299     | SwitchOnTypeRecipe, StringSwitchRecipe                           |
| G24    | Follow Standard Conventions                 | Ch.17 p.299     | Checkstyle LeftCurly, RightCurly, LineLength                     |
| G25    | Replace Magic Numbers with Named Constants  | Ch.17 p.300     | MagicStringRecipe                                                |
| G28    | Encapsulate Conditionals                    | Ch.17 p.301     | EncapsulateConditionalRecipe                                     |
| G29    | Avoid Negative Conditionals                 | Ch.17 p.302     | NegativeConditionalRecipe                                        |
| G30    | Functions Should Do One Thing               | Ch.17 p.302     | WhitespaceSplitMethodRecipe, ImperativeLoopRecipe                |
| G33    | Encapsulate Boundary Conditions             | Ch.17 p.304     | BoundaryConditionRecipe                                          |
| G34    | Functions Should Descend Only One Level     | Ch.17 p.304     | SectionCommentRecipe                                             |
| G36    | Avoid Transitive Navigation                 | Ch.17 p.306     | LawOfDemeterRecipe (fluent APIs excluded)                        |
| J1     | Avoid Long Import Lists                     | Ch.17 p.307     | Checkstyle AvoidStarImport                                       |
| J2     | Don't Inherit Constants                     | Ch.17 p.307     | InheritConstantsRecipe                                           |
| J3     | Constants versus Enums                      | Ch.17 p.308     | EnumForConstantsRecipe                                           |
| N1     | Choose Descriptive Names                    | Ch.17 p.309     | Checkstyle LocalVariableName, MethodName, TypeName               |
| N5     | Use Long Names for Long Scopes             | Ch.17 p.312     | ShortVariableNameRecipe                                          |
| N6     | Avoid Encodings                             | Ch.17 p.312     | EncodingNamingRecipe                                             |
| N7     | Names Should Describe Side-Effects          | Ch.17 p.313     | SideEffectNamingRecipe                                           |
| T1     | Insufficient Tests                          | Ch.17 p.313     | JaCoCo line coverage analysis                                    |
| T2     | Use a Coverage Tool                         | Ch.17 p.313     | JaCoCo report presence check                                     |
| T3     | Don't Skip Trivial Tests                    | Ch.17 p.313     | DisabledTestRecipe                                               |
| T4     | An Ignored Test Is a Question               | Ch.17 p.313     | DisabledTestRecipe                                               |
| T8     | Test Coverage Patterns                      | Ch.17 p.314     | JaCoCo per-class coverage analysis                               |
| T9     | Tests Should Be Fast                        | Ch.17 p.314     | Surefire test timing analysis                                    |
| Ch3.1  | Small Functions                             | Ch.3 p.34       | PrivateMethodTestabilityRecipe                                   |
| Ch7.1  | Use Exceptions Rather Than Return Codes     | Ch.7 p.103      | CatchLogContinueRecipe                                           |
| Ch7.2  | Don't Return Null                           | Ch.7 p.110      | NullDensityRecipe, SpotBugs redundant null checks                |
| Ch10.1 | Classes Should Be Small                     | Ch.10 p.136     | ClassLineLengthRecipe                                            |
| Ch10.2 | The Single Responsibility Principle         | Ch.10 p.138     | LargeRecordRecipe                                                |

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

## Configuration

```kotlin
cleanCode {
    thresholds {
        classLineCount = 200          // default 150
        recordComponentCount = 6      // default 5
        nullCheckDensity = 4          // default 3
        chainDepthThreshold = 4       // default 3
        verticalSeparationDistance = 8 // default 5
        methodBlankLineSections = 3   // default 2
        privateMethodMinLines = 8     // default 5
        magicStringMinOccurrences = 3 // default 2
        stringSwitchMinCases = 4      // default 3
        shortNameMinLength = 2        // default 2
    }
    disabledRecipes = listOf("G36", "G10")
}
```

## Architecture

```
CleanClaude/
├── annotations/   HeuristicCode enum, @SuppressCleanCode annotation
├── core/          Finding, AggregatedReport, BuildOutputFormatter,
│                  HeuristicDescriptions, SuppressionIndex, BaselineManager,
│                  ClaudeMdGenerator, JSON report I/O
├── recipes/       26 custom OpenRewrite ScanningRecipes
├── adapters/      8 FindingSource implementations (PMD, Checkstyle, SpotBugs,
│                  CPD, JaCoCo, Surefire, Dependency Updates, OpenRewrite)
├── plugin/        Gradle plugin, tasks, extension DSL
└── build-logic/   Convention plugins
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

| Library                | Version | Used by                    |
|------------------------|---------|----------------------------|
| JUnit Jupiter          | 5.10.0  | All modules (test)         |
| JavaParser             | 3.26.2  | core (SuppressionIndex)    |
| Gson                   | 2.11.0  | core, adapters (JSON I/O)  |
| OpenRewrite            | 8.40.2  | recipes, adapters          |
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
