# Clean Code Plugin

[![CI](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml/badge.svg)](https://github.com/fiftiesHousewife/Clean-Claude/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Gradle](https://img.shields.io/badge/Gradle-9.0-blue)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

A Gradle plugin that detects violations of Robert C. Martin's *Clean Code* heuristics across a Java codebase. It combines static analysis tools (PMD, Checkstyle, SpotBugs, JaCoCo) with 32 custom OpenRewrite recipes, normalises all findings into Martin's taxonomy, and produces human-readable output with book references and prescriptive guidance.

> *"Clean code reads like well-written prose."* -- Robert C. Martin, *Clean Code* (2008)

## Heuristic Coverage

Every finding is mapped to a specific heuristic from *Clean Code* Chapter 17 ("Smells and Heuristics") or to a chapter-specific pattern. The plugin currently detects:

For the full Robert Martin text, detection details, and skill file links for every heuristic, see **[HEURISTICS.md](HEURISTICS.md)**.

| Code   | Name                                        | Reference       | Detection                                                        |
|--------|---------------------------------------------|-----------------|------------------------------------------------------------------|
| [C3](HEURISTICS.md#c3-redundant-comment)     | Redundant Comment                           | Ch.17 p.286     | MumblingCommentRecipe                                            |
| [C5](HEURISTICS.md#c5-commented-out-code)     | Commented-Out Code                          | Ch.17 p.287     | CommentedCodeRecipe                                              |
| [E1](HEURISTICS.md#e1-build-requires-more-than-one-step)     | Build Requires More Than One Step           | Ch.17 p.287     | DependencyUpdatesFindingSource                                   |
| [F1](HEURISTICS.md#f1-too-many-arguments)     | Too Many Arguments                          | Ch.17 p.288     | Checkstyle ParameterNumber                                       |
| [F2](HEURISTICS.md#f2-output-arguments)     | Output Arguments                            | Ch.17 p.288     | OutputArgumentRecipe                                             |
| [F3](HEURISTICS.md#f3-flag-arguments)     | Flag Arguments                              | Ch.17 p.288     | FlagArgumentRecipe                                               |
| [F4](HEURISTICS.md#f4-dead-function)     | Dead Function                               | Ch.17 p.288     | PMD UnusedPrivateMethod                                          |
| [G4](HEURISTICS.md#g4-overridden-safeties)     | Overridden Safeties                         | Ch.17 p.289     | UncheckedCastRecipe, PMD, SpotBugs                               |
| [G5](HEURISTICS.md#g5-duplication)     | Duplication                                 | Ch.17 p.289     | CPD token-based detection                                        |
| [G8](HEURISTICS.md#g8-too-much-information)     | Too Much Information                        | Ch.17 p.291     | VisibilityReductionRecipe, PMD                                   |
| [G9](HEURISTICS.md#g9-dead-code)     | Dead Code                                   | Ch.17 p.292     | PMD, SpotBugs                                                    |
| [G10](HEURISTICS.md#g10-vertical-separation)    | Vertical Separation                         | Ch.17 p.292     | VerticalSeparationRecipe                                         |
| [G11](HEURISTICS.md#g11-inconsistency)    | Inconsistency                               | Ch.17 p.292     | InconsistentNamingRecipe                                         |
| [G12](HEURISTICS.md#g12-clutter)    | Clutter                                     | Ch.17 p.293     | PMD/Checkstyle                                                   |
| [G14](HEURISTICS.md#g14-feature-envy)    | Feature Envy                                | Ch.17 p.293     | FeatureEnvyRecipe                                                |
| [G16](HEURISTICS.md#g16-obscured-intent)    | Obscured Intent                             | Ch.17 p.295     | NestedTernaryRecipe                                              |
| [G19](HEURISTICS.md#g19-use-explanatory-variables)    | Use Explanatory Variables                   | Ch.17 p.296     | MissingExplanatoryVariableRecipe                                 |
| [G20](HEURISTICS.md#g20-function-names-should-say-what-they-do)    | Function Names Should Say What They Do      | Ch.17 p.297     | PMD                                                              |
| [G22](HEURISTICS.md#g22-make-logical-dependencies-physical)    | Make Logical Dependencies Physical          | Ch.17 p.298     | Checkstyle FinalLocalVariable                                    |
| [G23](HEURISTICS.md#g23-prefer-polymorphism-to-ifelse-or-switchcase)    | Prefer Polymorphism to If/Else or Switch    | Ch.17 p.299     | SwitchOnTypeRecipe, StringSwitchRecipe                           |
| [G24](HEURISTICS.md#g24-follow-standard-conventions)    | Follow Standard Conventions                 | Ch.17 p.299     | Checkstyle                                                       |
| [G25](HEURISTICS.md#g25-replace-magic-numbers-with-named-constants)    | Replace Magic Numbers with Named Constants  | Ch.17 p.300     | MagicStringRecipe                                                |
| [G28](HEURISTICS.md#g28-encapsulate-conditionals)    | Encapsulate Conditionals                    | Ch.17 p.301     | EncapsulateConditionalRecipe                                     |
| [G29](HEURISTICS.md#g29-avoid-negative-conditionals)    | Avoid Negative Conditionals                 | Ch.17 p.302     | NegativeConditionalRecipe                                        |
| [G30](HEURISTICS.md#g30-functions-should-do-one-thing)    | Functions Should Do One Thing               | Ch.17 p.302     | WhitespaceSplitMethodRecipe, ImperativeLoopRecipe                |
| [G33](HEURISTICS.md#g33-encapsulate-boundary-conditions)    | Encapsulate Boundary Conditions             | Ch.17 p.304     | BoundaryConditionRecipe                                          |
| [G34](HEURISTICS.md#g34-functions-should-descend-only-one-level-of-abstraction)    | Functions Should Descend Only One Level     | Ch.17 p.304     | SectionCommentRecipe                                             |
| [G36](HEURISTICS.md#g36-avoid-transitive-navigation)    | Avoid Transitive Navigation                 | Ch.17 p.306     | LawOfDemeterRecipe (fluent APIs excluded)                        |
| [J1](HEURISTICS.md#j1-avoid-long-import-lists-by-using-wildcards)     | Avoid Long Import Lists                     | Ch.17 p.307     | Checkstyle AvoidStarImport                                       |
| [J2](HEURISTICS.md#j2-dont-inherit-constants)     | Don't Inherit Constants                     | Ch.17 p.307     | InheritConstantsRecipe                                           |
| [J3](HEURISTICS.md#j3-constants-versus-enums)     | Constants versus Enums                      | Ch.17 p.308     | EnumForConstantsRecipe                                           |
| [N1](HEURISTICS.md#n1-choose-descriptive-names)     | Choose Descriptive Names                    | Ch.17 p.309     | Checkstyle                                                       |
| [N5](HEURISTICS.md#n5-use-long-names-for-long-scopes)     | Use Long Names for Long Scopes             | Ch.17 p.312     | ShortVariableNameRecipe                                          |
| [N6](HEURISTICS.md#n6-avoid-encodings)     | Avoid Encodings                             | Ch.17 p.312     | EncodingNamingRecipe                                             |
| [N7](HEURISTICS.md#n7-names-should-describe-side-effects)     | Names Should Describe Side-Effects          | Ch.17 p.313     | SideEffectNamingRecipe                                           |
| [T1](HEURISTICS.md#t1-insufficient-tests)     | Insufficient Tests                          | Ch.17 p.313     | JaCoCo line coverage                                             |
| [T2](HEURISTICS.md#t2-use-a-coverage-tool)     | Use a Coverage Tool                         | Ch.17 p.313     | JaCoCo report presence                                           |
| [T3](HEURISTICS.md#t3-dont-skip-trivial-tests)     | Don't Skip Trivial Tests                    | Ch.17 p.313     | DisabledTestRecipe                                               |
| [T4](HEURISTICS.md#t4-an-ignored-test-is-a-question-about-an-ambiguity)     | An Ignored Test Is a Question               | Ch.17 p.313     | DisabledTestRecipe                                               |
| [T8](HEURISTICS.md#t8-test-coverage-patterns-can-be-revealing)     | Test Coverage Patterns                      | Ch.17 p.314     | JaCoCo per-class analysis                                        |
| [T9](HEURISTICS.md#t9-tests-should-be-fast)     | Tests Should Be Fast                        | Ch.17 p.314     | Surefire timing                                                  |
| [Ch3.1](HEURISTICS.md#ch31-small-functions)  | Small Functions                             | Ch.3 p.34       | PrivateMethodTestabilityRecipe                                   |
| [Ch7.1](HEURISTICS.md#ch71-use-exceptions-rather-than-return-codes)  | Use Exceptions Rather Than Return Codes     | Ch.7 p.103      | CatchLogContinueRecipe                                           |
| [Ch7.2](HEURISTICS.md#ch72-dont-return-null)  | Don't Return Null                           | Ch.7 p.110      | NullDensityRecipe, SpotBugs                                      |
| [Ch10.1](HEURISTICS.md#ch101-classes-should-be-small) | Classes Should Be Small                     | Ch.10 p.136     | ClassLineLengthRecipe                                            |
| [Ch10.2](HEURISTICS.md#ch102-the-single-responsibility-principle) | The Single Responsibility Principle         | Ch.10 p.138     | LargeRecordRecipe                                                |

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
    skillsDir = ".claude/skills"      // default — where skill files are scaffolded

    thresholds {
        classLineCount = 200          // default 150
        recordComponentCount = 8      // default 6
        nullCheckDensity = 4          // default 3
        chainDepthThreshold = 4       // default 3
        verticalSeparationDistance = 15 // default 10
        methodBlankLineSections = 5   // default 4
        privateMethodMinLines = 8     // default 5
        magicStringMinOccurrences = 3 // default 2
        stringSwitchMinCases = 4      // default 3
        shortNameMinLength = 2        // default 2
        cpdMinimumTokens = 100       // default 50
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
├── recipes/       32 custom OpenRewrite ScanningRecipes
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
