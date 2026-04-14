# Clean Code Plugin — Design Reference

A complete record of design decisions, architectural patterns, data models,
and operational guidance from the plugin design sessions.

---

## 1. Complete Heuristic Coverage Map

Every Clean Code heuristic mapped to its detection tool and confidence level.
High-confidence, tool-detectable heuristics are the primary investment.
Low-confidence heuristics produce narrative-only sections in CLAUDE.md.

| Code | Heuristic | Tool | Confidence |
|---|---|---|---|
| **Comments** |
| C1 | Inappropriate Information | Manual / Narrative | LOW |
| C2 | Obsolete Comment | Manual / Semantic | LOW |
| C3 | Redundant Comment | OpenRewrite — comment mirrors signature | MEDIUM |
| C4 | Poorly Written Comment | Manual / NLP | LOW |
| C5 | Commented-Out Code | PMD `CommentedOutCode` | HIGH |
| **Environment** |
| E1 | Build Requires More Than One Step | CI config inspection — manual | LOW |
| E2 | Tests Require More Than One Step | CI config inspection — manual | LOW |
| **Functions** |
| F1 | Too Many Arguments | Checkstyle `ParameterNumber` | HIGH |
| F2 | Output Arguments | OpenRewrite — mutable parameter modification | MEDIUM |
| F3 | Flag Arguments | OpenRewrite — `boolean` params on non-private methods | HIGH |
| F4 | Dead Function | PMD `UnusedPrivateMethod` + OpenRewrite package-scoped | HIGH |
| **General** |
| G1 | Multiple Languages in One Source File | OpenRewrite — inline SQL/JS string literals | MEDIUM |
| G2 | Obvious Behavior Unimplemented | Manual / Semantic | LOW |
| G3 | Incorrect Behavior at Boundaries | Manual / Test coverage gaps | LOW |
| G4 | Overridden Safeties | SpotBugs — empty catch blocks, `@SuppressWarnings` | HIGH |
| G5 | Duplication | CPD | HIGH |
| G6 | Code at Wrong Level of Abstraction | Manual / Semantic | LOW |
| G7 | Base Classes Depending on Derivatives | OpenRewrite — `instanceof` subclass in superclass | HIGH |
| G8 | Too Much Information | PMD `ExcessivePublicCount`, `GodClass` | MEDIUM |
| G9 | Dead Code | PMD `UnusedLocalVariable`, SpotBugs `URF_UNREAD_FIELD` | HIGH |
| G10 | Vertical Separation | OpenRewrite — declaration distance from first use | MEDIUM |
| G11 | Inconsistency | Manual | LOW |
| G12 | Clutter | PMD `UnusedImports`, `EmptyIfStmt` | HIGH |
| G13 | Artificial Coupling | Manual | LOW |
| G14 | Feature Envy | OpenRewrite — external calls > local field access | LOW |
| G15 | Selector Arguments | OpenRewrite — boolean/enum args selecting behaviour | MEDIUM |
| G16 | Obscured Intent | Manual | LOW |
| G17 | Misplaced Responsibility | PMD `DataClass` | LOW |
| G18 | Inappropriate Static | OpenRewrite — instance methods not using `this` | MEDIUM |
| G19 | Use Explanatory Variables | OpenRewrite — complex inline expressions | MEDIUM |
| G20 | Function Names Should Say What They Do | Manual | LOW |
| G21 | Understand the Algorithm | Manual | LOW |
| G22 | Make Logical Dependencies Physical | Manual | LOW |
| G23 | Prefer Polymorphism to If/Else or Switch/Case | OpenRewrite — type-switching chains | MEDIUM |
| G24 | Follow Standard Conventions | Checkstyle — formatting, brace style | HIGH |
| G25 | Replace Magic Numbers with Named Constants | Checkstyle `MagicNumber` | HIGH |
| G26 | Be Precise | SpotBugs `DM_BOXED_PRIMITIVE_FOR_COMPARE` | MEDIUM |
| G27 | Structure over Convention | Manual | LOW |
| G28 | Encapsulate Conditionals | OpenRewrite — complex inline boolean expressions | MEDIUM |
| G29 | Avoid Negative Conditionals | OpenRewrite — double-negation, `!isNot...()` | HIGH |
| G30 | Functions Should Do One Thing | PMD `CyclomaticComplexity`, Checkstyle `MethodLength` | MEDIUM |
| G31 | Hidden Temporal Couplings | Manual / Narrative | LOW |
| G32 | Don't Be Arbitrary | Manual | LOW |
| G33 | Encapsulate Boundary Conditions | OpenRewrite — inline offset arithmetic | MEDIUM |
| G34 | Functions Should Descend One Level of Abstraction | Manual / Narrative | LOW |
| G35 | Keep Configurable Data at High Levels | OpenRewrite — magic numbers at depth | MEDIUM |
| G36 | Avoid Transitive Navigation | OpenRewrite — Law of Demeter (chain depth ≥ 3) | HIGH |
| **Naming** |
| N1 | Choose Descriptive Names | Checkstyle `LocalVariableName`, `MethodName` | MEDIUM |
| N2 | Choose Names at Appropriate Level of Abstraction | Manual | LOW |
| N3 | Use Standard Nomenclature | Manual / Domain-specific | LOW |
| N4 | Unambiguous Names | Manual | LOW |
| N5 | Use Long Names for Long Scopes | Checkstyle `LocalVariableName` + scope depth | MEDIUM |
| N6 | Avoid Encodings | OpenRewrite — Hungarian notation, `I`-prefix interfaces | HIGH |
| N7 | Names Should Describe Side-Effects | Manual | LOW |
| **Tests** |
| T1 | Insufficient Tests | JaCoCo — line/branch coverage thresholds | HIGH |
| T2 | Use a Coverage Tool | POM/Gradle inspection — JaCoCo plugin presence | HIGH |
| T3 | Don't Skip Trivial Tests | OpenRewrite — `@Disabled`/`@Ignore` without comment | HIGH |
| T4 | An Ignored Test Is a Question about Ambiguity | Same as T3 | HIGH |
| T5 | Test Boundary Conditions | Manual / Coverage gap analysis | LOW |
| T6 | Exhaustively Test Near Bugs | Manual / Bug history | LOW |
| T7 | Patterns of Failure Are Revealing | Manual / Test results | LOW |
| T8 | Test Coverage Patterns Can Be Revealing | JaCoCo uncovered branch visualisation | MEDIUM |
| T9 | Tests Should Be Fast | Surefire timing — flag tests over threshold | HIGH |

### Chapter Codes (patterns without Martin heuristic numbers)

Patterns clearly rooted in a chapter but without a canonical heuristic number
use chapter codes. These appear in the `HeuristicCode` enum alongside the
Martin codes:

| Code | Pattern | Confidence | Detection |
|---|---|---|---|
| Ch3.1 | Method length | HIGH | Recipe |
| Ch3.2 | Try-catch in loop | HIGH | Recipe |
| Ch3.3 | Data clumps | MEDIUM | Recipe (two-pass) |
| Ch6.1 | Primitive obsession | MEDIUM | Recipe |
| Ch7.1 | Catch-log-continue | MEDIUM | Recipe |
| Ch7.2 | Null density | LOW | Recipe |
| Ch10.1 | SRP candidates (composite) | LOW | Recipe |
| Ch10.2 | Large records without builder | HIGH | Recipe |

Chapter codes give genuine traceability without fabricating authority.
They appear in reports and skill file footers only — never in invocation
language or developer-facing documentation.

---

## 2. Core Data Model

### Finding

Everything converges on a single normalised `Finding`. Every adapter,
regardless of source tool, produces this type:

```java
public record Finding(
    HeuristicCode      code,         // G36, F3, Ch7.1, etc.
    String             sourceFile,   // relative to project root; null = project-level
    int                startLine,    // -1 if file-level
    int                endLine,      // -1 if file-level
    String             message,
    Severity           severity,     // ERROR, WARNING, INFO
    Confidence         confidence,   // HIGH, MEDIUM, LOW
    String             tool,         // "pmd", "checkstyle", "openrewrite" etc.
    String             ruleRef,      // original rule ID for traceability
    Map<String,String> metadata      // tool-specific extras
) {
    public static Finding at(HeuristicCode code, String sourceFile,
            int startLine, int endLine, String message,
            Severity severity, Confidence confidence,
            String tool, String ruleRef) {
        return new Finding(code, sourceFile, startLine, endLine,
                           message, severity, confidence, tool, ruleRef, Map.of());
    }

    public static Finding projectLevel(HeuristicCode code, String message,
            Severity severity, Confidence confidence, String tool, String ruleRef) {
        return new Finding(code, null, -1, -1, message,
                           severity, confidence, tool, ruleRef, Map.of());
    }
}

public enum Severity   { ERROR, WARNING, INFO }
public enum Confidence { HIGH, MEDIUM, LOW }
```

### AggregatedReport

```java
public record AggregatedReport(
    List<Finding>       findings,
    Set<HeuristicCode>  coveredCodes,
    Instant             generatedAt,
    String              projectName,
    String              projectVersion
) {
    public Map<HeuristicCode, List<Finding>> byCode() {
        return findings.stream().collect(
            Collectors.groupingBy(Finding::code, TreeMap::new, Collectors.toList()));
    }

    public Map<Severity, List<Finding>> bySeverity() {
        return findings.stream().collect(
            Collectors.groupingBy(Finding::severity));
    }
}
```

---

## 3. FindingSource Interface and Adapter Layer

### Interface

```java
public interface FindingSource {
    String id();           // "pmd", "checkstyle", "openrewrite" etc.
    String displayName();  // human-readable, for reports

    List<Finding> collectFindings(ProjectContext context)
        throws FindingSourceException;

    Set<HeuristicCode> coveredCodes(); // used to build CLAUDE.md scaffold
                                       // even when no violations found

    default boolean isAvailable(ProjectContext context) { return true; }
}
```

### ProjectContext

```java
public record ProjectContext(
    Path        projectRoot,
    String      projectName,
    String      projectVersion,
    String      javaVersion,       // e.g. "21"
    List<Path>  sourceRoots,       // [src/main/java]
    List<Path>  testSourceRoots,   // [src/test/java]
    Path        buildDir,          // build/
    Path        reportsDir         // build/reports/
) {}
```

### Adapter Tree

```
FindingSource
├── PmdFindingSource          // parses build/reports/pmd/main.xml
├── CheckstyleFindingSource   // parses build/reports/checkstyle/main.xml
├── SpotBugsFindingSource     // parses build/reports/spotbugs/main.xml
├── CpdFindingSource          // parses build/reports/cpd/cpd.xml → G5
├── OpenRewriteFindingSource  // runs ScanningRecipes inline, reads DataTable
├── JacocoFindingSource       // parses build/reports/jacoco/test/jacocoTestReport.xml
└── SurefireFindingSource     // parses build/reports/surefire-reports/*.xml
```

Rule-to-heuristic mapping lives entirely inside each adapter.

### PMD Rule → HeuristicCode

| PMD Rule | HeuristicCode | Severity | Confidence |
|---|---|---|---|
| CyclomaticComplexity | G30 | WARNING | MEDIUM |
| NPathComplexity | G30 | WARNING | MEDIUM |
| ExcessiveMethodLength | G30 | WARNING | MEDIUM |
| UnusedPrivateMethod | F4 | WARNING | HIGH |
| UnusedLocalVariable | G9 | INFO | HIGH |
| UnusedImports | G12 | INFO | HIGH |
| ExcessivePublicCount | G8 | WARNING | HIGH |
| CouplingBetweenObjects | G8 | WARNING | MEDIUM |
| AvoidConstantsInterface | J2 | WARNING | HIGH |
| CommentedOutCodeLine | C5 | WARNING | HIGH |
| EmptyCatchBlock | G4 | ERROR | HIGH |
| EmptyIfStmt | G12 | WARNING | HIGH |
| SwitchStmtsShouldHaveDefault | G23 | INFO | MEDIUM |
| TooManyFields | G8 | WARNING | MEDIUM |
| TooManyMethods | G8 | WARNING | MEDIUM |
| GodClass | G8 | ERROR | MEDIUM |
| DataClass | G17 | INFO | LOW |

PMD priority severity mapping: 1–2 → ERROR, 3 → WARNING, 4–5 → INFO.
Override with HeuristicCode-level severity from table above where specified.

### Checkstyle Check → HeuristicCode

| Check (simple name) | HeuristicCode | Confidence |
|---|---|---|
| ParameterNumber | F1 | HIGH |
| MagicNumber | G25 | HIGH |
| LocalVariableName | N1 | MEDIUM |
| MethodLength | G30 | MEDIUM |
| AnonInnerLength | G30 | MEDIUM |
| AvoidStarImport | J1 | HIGH |
| IllegalImport | G12 | HIGH |
| InterfaceIsType | J2 | HIGH |
| VisibilityModifier | G8 | MEDIUM |
| HideUtilityClassConstructor | G18 | HIGH |
| OneTopLevelClass | G12 | HIGH |
| NeedBraces | G28 | MEDIUM |
| LeftCurly / RightCurly | G24 | HIGH |
| WhitespaceAround | G24 | HIGH |
| EmptyLineSeparator | G10 | MEDIUM |
| MethodName / TypeName | N1 | MEDIUM |

### SpotBugs Category/Type → HeuristicCode

| Category / Bug type | HeuristicCode | Severity | Confidence |
|---|---|---|---|
| CORRECTNESS (any) | G4 | ERROR | HIGH |
| BAD_PRACTICE / DE_MIGHT_IGNORE | G4 | ERROR | HIGH |
| BAD_PRACTICE / ST_WRITE_TO_STATIC_FROM_INSTANCE | G18 | WARNING | HIGH |
| STYLE / URF_UNREAD_FIELD | G9 | INFO | HIGH |
| PERFORMANCE / DM_BOXED_PRIMITIVE_FOR_COMPARE | G26 | INFO | HIGH |
| MALICIOUS_CODE / MS_MUTABLE_ARRAY | G8 | WARNING | HIGH |

Rank 1–4 → ERROR, 5–9 → WARNING, 10–20 → INFO.
SpotBugs uses native `@SuppressFBWarnings`; the adapter skips findings already
suppressed in XML output rather than going through `SuppressionIndex`.

### CPD → HeuristicCode

All CPD findings map to G5. One `Finding` per `<file>` element within each
`<duplication>` block, with `otherFile` in metadata. Severity by token count:
≥ 200 tokens → ERROR, 100–199 → WARNING, < 100 → INFO.

### JaCoCo → HeuristicCode

- **T1**: one project-level finding with overall line and branch coverage.
  < 50% → ERROR, 50–74% → WARNING, ≥ 75% → INFO.
- **T8**: per-class findings where line coverage < 50% and class ≥ 10 lines → WARNING.
- **T2**: if report file is absent and project has test sources → project-level ERROR.

### Surefire → HeuristicCode

- **T9**: per-test-method WARNING where execution time > threshold (configurable,
  default 5s). Project-level ERROR if any test exceeds 30s.
- **T3/T4**: per `<skipped/>` element → INFO. Project-level WARNING if > 10%
  of tests are skipped.

---

## 4. OpenRewrite Recipe Specifications

All recipes extend `ScanningRecipe<Accumulator>` with a companion DataTable.
Parse once, run all via `CompositeRecipe`:

```java
List<SourceFile> sourceFiles = parser.parse(sourcePaths, baseDir, ctx);
InMemoryLargeSourceSet lss = new InMemoryLargeSourceSet(sourceFiles);
Recipe composite = new CompositeRecipe(allRecipes);
RecipeRun result = composite.run(lss, ctx);
```

`ScanningRecipe` is two-pass (accumulate then generate) but the parse is
performed once. DataTables from all recipes are harvested from the single
`RecipeRun`.

**Classpath caching:** classpath resolution is expensive in large
multi-module projects. Cache the resolved classpath at the
`OpenRewriteFindingSource` level across incremental builds — parsed
classpath entries do not change between runs unless dependencies change.
This is separate from LST caching and applies to the parser construction
step, not the parse itself.

### FlagArgumentRecipe → F3

`JavaIsoVisitor` on `MethodDeclaration`. For each `boolean` parameter on a
non-`private` method, emit a row. Exclude constructors.
DataTable: `className`, `methodName`, `paramName`, `lineNumber`.

### LawOfDemeterRecipe → G36

`JavaIsoVisitor` on method invocation chains. Detect chains of depth ≥ 3
(e.g. `a.getB().getC().getD()`). Count depth by recursing into `Select`
expressions.
DataTable: `className`, `methodName`, `chain`, `depth`, `lineNumber`.

### NegativeConditionalRecipe → G29

Detect double negation: `!isNot...()` — method name starts with `isNot`/`hasNot`
and the call is negated. Also detect `!!` patterns.
DataTable: `className`, `methodName`, `expression`, `lineNumber`.

### SwitchOnTypeRecipe → G23

Visit `IfStatement` and `SwitchStatement`. For `if/else if` chains of depth
≥ 3 where conditions involve `instanceof`, `.getClass()`, or a field named
`type`/`kind`/`status`, emit a row.
DataTable: `className`, `methodName`, `depth`, `pattern`, `lineNumber`.

### CommentedCodeRecipe → C5

Visit all comment nodes. Score comment body against heuristics: contains `;`,
`{`, `}`, or Java keywords at line starts. Score ≥ 2 matches → emit row.
(PMD also detects C5 — this recipe provides the OpenRewrite alternative.)
DataTable: `sourceFile`, `lineNumber`, `commentPreview`.

### MagicNumberInDepthRecipe → G35

Visit `IntegerLiteralExpr` and `LongLiteralExpr`. Exclude: 0, 1, -1, 2,
values in `@interface`, `case` labels, `static final` field initialisers.
Track call stack depth from class root — emit only when depth ≥ 3.
DataTable: `className`, `methodName`, `value`, `lineNumber`.

### EncapsulateConditionalRecipe → G28

Visit `IfStatement`. If condition is a `BinaryExpr` tree with depth ≥ 2
(more than one logical operator) and is not a boolean-returning method call,
emit a row.
DataTable: `className`, `methodName`, `conditionText`, `depth`, `lineNumber`.

### EnumForConstantsRecipe → J3

Visit `ClassOrInterfaceDeclaration`. Count `static final int` or
`static final String` fields. If ≥ 3 fields share a common prefix
(`STATUS_ACTIVE`, `STATUS_INACTIVE`, `STATUS_PENDING`), emit a row.
Use longest-common-prefix algorithm.
DataTable: `className`, `prefix`, `fieldCount`, `lineNumber`.

### CatchLogContinueRecipe → Ch7.1

Visit `J.Try`. For each catch clause whose body contains only a logging
call (method call on a logger-named variable) or is empty, emit a row.
DataTable: `className`, `methodName`, `exceptionType`, `lineNumber`.

### NullDensityRecipe → Ch7.2

Visit methods. Count null-check expressions (`x == null`, `x != null`,
`Objects.isNull(x)`) per method. Emit when count ≥ threshold (configurable,
default 3).
DataTable: `className`, `methodName`, `nullCheckCount`, `lineNumber`.

### ClassLineLengthRecipe → Ch10.1 (proxy) and Ch10.2

Visit `ClassDeclaration`. Compute line span from `getCoordinates()`.
For Ch10.1 composite: emit when line count > threshold (default 150) as one
proxy signal — requires ≥ 2 composite signals to raise finding.
For a standalone signal at > 300 lines: emit as MEDIUM without requiring
a second signal.
DataTable: `className`, `lineCount`, `lineNumber`.

### LargeRecordRecipe → Ch10.2

Visit `ClassDeclaration` where `getKind() == Kind.RECORD`. Count record
components. If count > threshold (default 4) and no nested `Builder` class
exists, emit a row.
DataTable: `className`, `componentCount`, `lineNumber`.

### OutputArgumentRecipe → F2

Visit `MethodDeclaration`. For each parameter of a mutable reference type
(`Collection`, `Map`, array, or any non-final class), check whether the
method body contains assignments into that parameter's fields or mutating
method calls on it (`add`, `put`, `set`, `remove`). Emit when detected.
Exclude parameters that are clearly input+output by convention (e.g. `StringBuilder`).
DataTable: `className`, `methodName`, `paramName`, `paramType`, `lineNumber`.

### DisabledTestRecipe → T3 / T4

Visit `MethodDeclaration` and `ClassDeclaration` annotated with
`@Disabled` (JUnit 5) or `@Ignore` (JUnit 4). Check whether a reason
string is present in the annotation value. If absent, emit a row.
Also emit when the annotation value is blank or a placeholder (`"TODO"`, `""`).
DataTable: `className`, `methodName`, `annotation`, `hasReason`, `lineNumber`.

### DeadFunctionRecipe → F4 (package-scoped supplement to PMD)

PMD `UnusedPrivateMethod` covers private methods. This recipe covers
package-scoped methods not reachable from the PMD scan. Visits all
non-`private` methods and checks for call sites within the module.

**Important caveat:** this recipe is approximate by design. Without a
full compile step there is no type resolution, so cross-module calls
and reflection-based invocations will produce false positives. Mark all
findings from this recipe `Confidence.LOW` unconditionally. Exclude
`DeadFunctionRecipe` findings from build gate eligibility by default —
they should appear in reports but never cause a build failure without
explicit opt-in in the `thresholds` config.

DataTable: `className`, `methodName`, `visibility`, `lineNumber`.

### BaseClassDependsOnDerivativeRecipe → G7

Visit `ClassDeclaration`. For each `instanceof` expression or direct
class reference in a superclass or interface body, check whether the
referenced type is a known subtype of the declaring class. Emit when
a parent class references a specific subclass by name.
DataTable: `className`, `referencedSubclass`, `context`, `lineNumber`.

### VerticalSeparationRecipe → G10

Visit `MethodDeclaration` bodies. For each local variable declaration,
compute the distance in lines to the first use of that variable. Emit
when distance > threshold (configurable, default 5 lines). Excludes
variables declared at the top of a method that serve as accumulators.
DataTable: `className`, `methodName`, `variableName`, `declarationLine`,
`firstUseLine`, `distance`.

### ExplanatoryVariableRecipe → G19

Visit complex expressions used directly as arguments or conditions
without being assigned to a named variable first. Proxy: detect method
call arguments or `if` conditions that are `BinaryExpr` trees of depth
≥ 3 and are not assigned to an intermediate variable. Emit as a
suggestion to extract to an explanatory variable.
DataTable: `className`, `methodName`, `expressionPreview`, `lineNumber`.

### InlineOffsetArithmeticRecipe → G33

Variant of `MagicNumberInDepthRecipe` focused specifically on boundary
conditions. Detect arithmetic expressions of the form `x + 1`, `x - 1`,
`length - 1`, `size + 1` used directly as array indices, loop bounds,
or substring arguments rather than assigned to a named variable. Emit
when the pattern appears inside a loop or index expression.
DataTable: `className`, `methodName`, `expression`, `lineNumber`.

### InheritConstantsRecipe → J2

Visit `ClassOrInterfaceDeclaration` that `implements` an interface.
Check whether the implemented interface contains only constants (`static
final` fields) and no method signatures. Emit for each such interface —
this is the "constants interface" antipattern where a class inherits
constants by implementing the interface rather than using static imports.
DataTable: `className`, `interfaceName`, `lineNumber`.

### EncodingNamingRecipe → N6

Visit all named elements (fields, methods, parameters, local variables,
classes). Detect: Hungarian notation prefixes (`strName`, `intCount`,
`bIsValid`), `I`-prefix on interface names (`IRepository`, `IService`),
type suffixes (`NameString`, `CountInt`). Emit one row per violation.
DataTable: `elementKind`, `className`, `elementName`, `violationType`,
`lineNumber`.

### InjectBaselineTodoRecipe (baseline pass only)

Not a scanning recipe — a transformation recipe that runs during
`./gradlew cleanCodeBaseline`. Takes a list of `Finding` records already
produced by the scan pass and injects structured TODO comments at each
finding's source location. AST-aware, idempotent (does not duplicate
existing TODO comments for the same code at the same line).

---

## 5. Suppression

### Annotation Design

```java
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)  // no runtime footprint
@Documented
@Repeatable(SuppressCleanCode.List.class)
public @interface SuppressCleanCode {
    HeuristicCode[] value();
    String reason();            // mandatory — no silent suppressions
    String until() default "";  // ISO date — violation after deadline

    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR})
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface List {
        SuppressCleanCode[] value();
    }
}
```

`reason()` is non-defaulted deliberately. A suppression with no rationale
is noise. The `@Repeatable` container allows multiple suppressions on the
same element without array syntax:

```java
@SuppressCleanCode(value = HeuristicCode.F1, reason = "Legacy API", until = "2025-12-01")
@SuppressCleanCode(value = HeuristicCode.G28, reason = "Complex condition unavoidable here")
public Result generate(String a, String b, String c, String d, boolean paginated) { ... }
```

### SuppressionIndex

`RetentionPolicy.SOURCE` means annotations aren't in bytecode. The index
is built by scanning source with **JavaParser** (no full compile needed).
This is a separate parse from the OpenRewrite LST — intentionally so.
JavaParser is lighter and faster than OpenRewrite's full LST parser for
annotation scanning only, and the two parsers serve distinct purposes.
Do not attempt to fold suppression scanning into the OpenRewrite LST
parse — it would work but is heavier than needed for this task.

```java
public class SuppressionIndex {

    public static SuppressionIndex build(Path sourceRoot) {
        // Walk .java files, parse with JavaParser,
        // collect @SuppressCleanCode annotations with target node spans.
        // Handle both single @SuppressCleanCode and @SuppressCleanCode.List
        // container — use ann.toNormalAnnotationExpr() /
        // toSingleMemberAnnotationExpr() as needed.
    }

    public boolean isSuppressed(Finding f) {
        // Check f.sourceFile + f.line falls within annotated span.
        // Expired until= dates → emit META_SUPPRESSION_EXPIRED finding.
    }

    public List<Finding> metaFindings() {
        // META_SUPPRESSION_EXPIRED (ERROR) — past until= date
        // META_SUPPRESSION_NO_REASON (WARNING) — blank or "TODO" reason
        return metaFindings;
    }
}
```

**Known brittleness:** JavaParser's `getBegin()`/`getEnd()` on declarations
is reliable for methods and top-level classes but gets tricky for inner
classes and lambdas. A type-level `@SuppressCleanCode` can be interpreted
two ways — applying to the whole class body, or only to the class
declaration line itself. The current design assumes the former (type-level
suppression covers all findings within the class body). Review this
decision before implementation and document the chosen behaviour in the
annotation Javadoc. Inner classes annotated separately are always treated
as their own suppression scope.

### FindingFilter

Post-filter approach — tools run unsuppressed, the filter removes
suppressed findings after parsing. Keeps the model consistent and tools
dumb. SpotBugs is the exception: retain native `@SuppressFBWarnings` and
skip already-suppressed findings in the XML.

---

## 6. Gradle Plugin Shape

### Module Structure

```
clean-code/
├── annotations/          // SuppressCleanCode, HeuristicCode enum
├── core/                 // Finding, FindingSource, SuppressionIndex,
│                         // FindingFilter, AggregatedReport, ProjectContext
├── adapters/             // One FindingSource per tool
└── plugin/               // Gradle plugin, tasks, extension
```

### Implementation Order

Build bottom-up with tests at each layer before proceeding upward.
The temptation is to wire the Gradle plugin first to see something
working — resist it. The adapter layer is where most bugs will live
and it must be tested in isolation before the plugin wires it together.

Recommended order:
1. `annotations` module — `HeuristicCode` enum, `SuppressCleanCode`
2. `core` module — `Finding`, `FindingSource`, `AggregatedReport`
3. `core` suppression — `SuppressionIndex`, `FindingFilter`
4. `adapters` module — one adapter at a time, each with its own tests
5. `plugin` module — Gradle wiring last, once adapters are verified

### Plugin Configuration

```kotlin
cleanCode {
    failOnViolation = true
    reportFormats   = listOf("html", "json")

    thresholds {
        // Build gate
        error  (G36, maxCount = 0)
        error  (T1,  maxCount = 0)
        warning(G5,  maxCount = 5)
        warning(G30, maxCount = 10)

        // Recipe thresholds
        classLineCount        = 150
        classLineCountAlone   = 300
        publicMethodCount     = 15
        fieldCount            = 10
        injectedDependencies  = 6
        importFanOut          = 8
        methodParameterCount  = 4
        recordComponentCount  = 4
        nullCheckDensity      = 3
    }

    sources {
        pmd        { reportsDir = layout.buildDirectory.dir("reports/pmd") }
        checkstyle { reportsDir = layout.buildDirectory.dir("reports/checkstyle") }
        cpd        { reportsDir = layout.buildDirectory.dir("reports/cpd") }
        spotbugs   { reportsDir = layout.buildDirectory.dir("reports/spotbugs") }
        jacoco     { reportsDir = layout.buildDirectory.dir("reports/jacoco") }
        openrewrite { /* runs inline */ }
    }

    skillsDir = project.file(".claude/skills")  // optional, defaults to this
}
```

### Plugin Tasks

```kotlin
class CleanCodePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("cleanCode", CleanCodeExtension::class.java)

        val analyse = project.tasks.register("analyseCleanCode", AnalyseTask::class.java) {
            it.extension.set(ext)
            it.mustRunAfter("check")
        }

        project.tasks.register("generateClaudeMd", GenerateClaudeMdTask::class.java) {
            it.reportFile.set(
                project.layout.buildDirectory.file("reports/clean-code/findings.json"))
            it.claudeMdFile.set(project.file("CLAUDE.md"))
            it.dependsOn(analyse)
        }

        project.tasks.register("cleanCodeBaseline", BaselineTask::class.java) {
            it.dependsOn(analyse)
        }

        project.tasks.register("cleanCodeExplain", ExplainTask::class.java)

        project.tasks.register("cleanCodeCompare", CompareTask::class.java) {
            it.dependsOn(analyse)
        }

        project.tasks.named("build") {
            it.finalizedBy(analyse)
        }
    }
}
```

`AnalyseTask` normalises all tool XML reports into `Finding` records.
`OpenRewriteFindingSource` is the exception — it runs recipes inline
against the same parsed LST rather than reading a pre-existing report.

### Bundled Rule Sets

The plugin bundles conservative PMD, Checkstyle, and SpotBugs rule sets
as JAR resources. Only rules that map to a `HeuristicCode` are included —
no unmapped rules that produce unfilterable findings:

```
resources/
├── cleancode-pmd-rules.xml
├── cleancode-checkstyle-rules.xml
└── cleancode-spotbugs-filter.xml
```

---

## 7. File Structure

```
project-root/
├── CLAUDE.md                        # generated on every build
├── SKILLS.md                        # human-readable catalogue (hand-maintained)
├── clean-code-baseline.json         # baseline snapshot (source-controlled)
└── .claude/
    └── skills/
        ├── ch3-functions.md
        ├── ch7-exception-handling.md
        ├── ch7-null-handling.md
        ├── ch10-classes.md
        ├── openrewrite-recipes.md
        └── project-conventions.md  # team-specific stub on first run
```

---

## 8. CLAUDE.md Structure

### CLAUDE.md Generation Behaviour

`GenerateClaudeMdTask` distinguishes two section types using HTML comment
markers:

```markdown
<!-- GENERATED: Ch7.1 -->
...findings content...
<!-- /GENERATED -->

<!-- ANNOTATE: G31 -->
...human-written narrative preserved across regenerations...
<!-- /ANNOTATE -->
```

**On regeneration:** only `GENERATED` sections are replaced. `ANNOTATE`
sections are preserved verbatim. Use a line-based parser to detect
section boundaries — do not use regex for multi-line section matching.

**Scaffold behaviour:** `coveredCodes()` from each `FindingSource` is
used to generate empty stub sections for all heuristic codes even when
no violations are found. This ensures CLAUDE.md always has a complete
structure, and human narrative can be added to any section even before
violations appear. Low-confidence and narrative-only codes get `ANNOTATE`
stubs by default; detectable codes get `GENERATED` sections.

**Scaffold stub shape for narrative-only codes:**
```markdown
<!-- ANNOTATE: G31 -->
## Hidden Temporal Couplings

No automated detection — add project-specific notes here.
<!-- /ANNOTATE -->
```

This is the mechanism that makes CLAUDE.md useful for patterns that
cannot be detected statically — the stub section prompts a human to fill
it in, and the content is preserved across every subsequent build.

### Generated vs ANNOTATE Sections

CLAUDE.md has two kinds of section:

- **Generated** — produced entirely by the plugin, overwritten on every
  run. Finding sections, delta table, framework summary.
- **ANNOTATE** — preserved by the plugin across regenerations. Team-authored
  content that enriches the generated sections with human context.

ANNOTATE sections are delimited by `<!-- ANNOTATE -->` markers:

```markdown
## Error Handling Issues [19 findings + 4 TODOs]
> Read `.claude/skills/ch7-exception-handling.md` before addressing these.

<!-- ANNOTATE -->
Note: the pipeline entry point is `AnalysisRunner.run()` — all exceptions
should surface there. The batch loop in `ReportRenderer` is intentional and
uses BatchErrorCollector correctly; do not flag it.
<!-- /ANNOTATE -->

**From analysis:**
- ReportOrchestrator.java:847
```

`GenerateClaudeMdTask` uses a line-based parser (not regex) to detect
`<!-- ANNOTATE -->` blocks and preserve them when regenerating the file.
If the corresponding finding section disappears (all findings resolved),
the ANNOTATE block is removed automatically.

### Framework Detection

`ProjectCharacterisationTask` runs during `generateClaudeMd` and emits a
framework summary into CLAUDE.md. It detects which frameworks are in use
via a `FrameworkRegistry` that maps `group:artifact` coordinates to display
names:

```java
// Known patterns (extend as needed):
// org.springframework.boot:spring-boot-starter  → "Spring Boot"
// org.jooq:jooq                                 → "jOOQ"
// org.duckdb:duckdb_jdbc                        → "DuckDB"
// io.pf4j:pf4j                                  → "PF4J"
// org.junit.jupiter:junit-jupiter               → "JUnit 5"
// com.fasterxml.jackson.core:jackson-databind   → "Jackson"
```

The emitted section tells Claude Code which frameworks are present without
requiring it to infer from imports:

```markdown
## Frameworks in use
Spring Boot 3.2, jOOQ 3.19, DuckDB 0.10, JUnit 5, Jackson 2.17
```

This section is Generated and overwritten on every build.

### CLAUDE.md Structure

```markdown
## Before you start any work in this codebase

1. Read `.claude/skills/SKILLS.md` now, before reading anything else.
   This is mandatory, not optional.
2. When a finding section below points to a skill file, read it before
   acting on that finding.
3. When working on anything not covered by a finding, check SKILLS.md
   for a matching skill before proceeding.

## Frameworks in use
[generated by ProjectCharacterisationTask]

## Current standing vs baseline

| Category | Baseline | Current | Delta |
|---|---|---|---|
| Ch7.1 Catch-log-continue | 23 | 19 | -4 ✓ |
| Ch10.1 SRP candidates | 8 | 9 | +1 ⚠ |
...

## Error Handling Issues [19 findings + 4 TODOs]
> Read `.claude/skills/ch7-exception-handling.md` before addressing these.

<!-- ANNOTATE -->
[optional team-authored context, preserved across regenerations]
<!-- /ANNOTATE -->

**From analysis:**
- ReportOrchestrator.java:847
- UserSessionManager.java:203

**From TODOs [autofix]:**
- ProfilerEngine.java:134 — catch swallows IOException
```

The skill pointer is the **first line** of every finding section,
adjacent to the findings it governs. Never centralised in a header.

The delta table is the most important section for human developers.

---

## 9. Baseline Mode and TODO Generation

### Running the Baseline

```
./gradlew cleanCodeBaseline
```

Three things in a single pass:
1. Snapshot finding counts into `clean-code-baseline.json`
2. Inject structured TODO at every finding site via `InjectBaselineTodoRecipe`
3. Subsequent builds report only findings above baseline

### Structured TODO Format

```java
// TODO Ch7.1 [autofix] (.claude/skills/exception-handling.md): catch swallows ProfilerException — wrap and propagate with context
```

| Field | Source |
|---|---|
| Code | Finding's `HeuristicCode` |
| Action tag | Derived from `Confidence` |
| Skill path | Plugin code→skill registry |
| Reason | Generated from finding metadata |

**Confidence → action tag:**

| Confidence | Tag | Meaning |
|---|---|---|
| HIGH | `[autofix]` | Claude Code may act directly |
| MEDIUM | `[review]` | Claude Code assesses, does not act |
| LOW | `[human]` | Requires judgment — Claude Code must not act |

**`[claude]` author tag** for Claude Code-generated TODOs:

```java
// TODO Ch7.1 [claude] [human-review]: unable to determine if fallback is genuine recovery
```

Prevents another Claude Code instance acting on uncertainty that was
explicitly flagged for human judgment.

**Duplication (G5):** inject at both sites with cross-reference:

```java
// TODO G5 [review] (.claude/skills/ch3-functions.md): duplicated block — see also UserService.java:203
```

**Placement:** immediately before the offending construct — class
declaration for class-level, method signature for method-level,
catch block for Ch7.1. Consistent placement means Claude Code finds
the relevant construct immediately after the TODO.

### TODO as Work Queue

After baseline, the codebase contains a complete tagged inventory:

```
grep -r "TODO Ch7" src/          # all exception handling debt
grep -r "TODO.*autofix" src/     # Claude Code's work queue
grep -r "TODO.*human" src/       # items requiring human judgment
```

### Code → Skill Path Registry

Static registry in the plugin maps every code to its skill file.
Multiple codes mapping to the same file is expected:

```java
Map<HeuristicCode, String> SKILL_PATHS = Map.of(
    Ch7_1,  ".claude/skills/exception-handling.md",
    Ch7_2,  ".claude/skills/ch7-null-handling.md",
    Ch10_1, ".claude/skills/ch10-classes.md",
    Ch10_2, ".claude/skills/ch10-classes.md",
    Ch3_1,  ".claude/skills/ch3-functions.md",
    Ch3_2,  ".claude/skills/ch3-functions.md",
    G5,     ".claude/skills/ch3-functions.md"
);
```

---

## 10. Skill File Design

### Serve Both Audiences

Write for Claude Code first — prescriptive, explicit, with negative
constraints. Ensure content is readable prose a developer can learn from.
Both audiences use the same files.

### Key Structural Principles

**Scope section first.** State what the skill applies to and what it
excludes. Include new code as well as fixing — Claude Code reads scope
literally.

**Split fixing vs new code paths.** Caller search and AutoCloseable
checks on existing blocks only make sense for existing code.

**Inline skill pointers at findings, not centralised index.** Pointer
must be adjacent to the content that triggers it. Long-range associations
across a large document are unreliable.

**Pattern tables beat prose for Claude Code.** Mutually exclusive
patterns with explicit tiebreaker rules are more reliably followed.

**Negative constraints are as important as positive patterns.** Without
do-not rules Claude Code does the nearest plausible thing, which is
often subtly wrong.

**Flag-for-human-review must be concrete:**
```java
// TODO: Ch7.1 — requires human review: [reason]
```

**One finding per task.** Batching makes review harder and a bad fix
affects multiple commits if reverted.

**Test classes need explicit exclusion.** Without it Claude Code applies
production patterns to test code.

**Examples must be labelled theoretical** when they use class names that
don't exist in the codebase:
```java
// Illustrative only — class names are theoretical
// DataAccessException is real (jOOQ)
```

### Depth of Nesting

One level of indirection works reliably. Skill files referencing other
skill files is unreliable. Keep the hierarchy flat. Cross-references are
fine for humans; for Claude Code use them sparingly.

### SKILLS.md

Human-readable catalogue — not a router for Claude Code. Claude Code uses
inline CLAUDE.md pointers. SKILLS.md is for onboarding and maintenance:

```markdown
| Concern | File | When to read |
|---|---|---|
| Error handling & exceptions | ch7-exception-handling.md | Catch blocks, propagation |
| Null handling | ch7-null-handling.md | Null returns, Optional |
| Class design & SRP | ch10-classes.md | Splitting, sizing |
| Function design | ch3-functions.md | Length, parameters, loops |
| Project conventions | project-conventions.md | Naming, approved libraries |
| Adding new recipes | openrewrite-recipes.md | Extending the plugin |
```

---

## 11. Human-Facing Documentation

### Three Tiers of Depth

| Tier | Format | Content |
|---|---|---|
| 1 | Build output | One sentence: what went wrong and where |
| 2 | CLAUDE.md | Paragraph: what the pattern is and the remedy |
| 3 | Skill file | Full treatment: patterns, examples, constraints |

### Build Output

```
New violations introduced (vs baseline):

  ReportOrchestrator.java:847  — catches ProfilerException without propagating
  UserSessionManager.java:203  — catches ConfigException without propagating

  Run ./gradlew cleanCodeExplain --finding=error-handling for guidance.
```

### cleanCodeExplain Task

Prints skill content directly to the terminal. Invoked by concern, not
by code — developers never need to know chapter codes or file paths:

```
./gradlew cleanCodeExplain --finding=error-handling
./gradlew cleanCodeExplain --finding=class-structure
```

### Getting Started

Zero mandatory configuration:

```kotlin
plugins {
    id("com.citi.cleancode") version "1.0.0"
}
```

`./gradlew generateClaudeMd` produces a useful CLAUDE.md immediately.
On first run the plugin scaffolds the skills directory — chapter skill
files copied from plugin resources, `project-conventions.md` stub added.

---

## 12. Claude Code vs Claude.ai — Skill Design Implications

| Concern | Claude.ai | Claude Code |
|---|---|---|
| Correction loop | Conversational, iterative | Autonomous — acts then stops |
| Stakes of ambiguity | Slightly wrong answer | Wrong file committed |
| Instruction following | Suggestive language works | Imperative language required |
| Scope | Naturally bounded by conversation | Must be explicitly constrained |
| Tool calls | Implicit reasoning | Explicit, literal execution |

Write skill files for Claude Code. They will also work for Claude.ai —
additional precision does not hurt a conversational interaction. The
reverse is not true.

---

## 13. Skill Invocation Language

Invoke by intent, not by code:

```
fix the error handling in ProfilerEngine
improve the class structure in ReportOrchestrator
add a builder to QueryDefinition
```

Chapter codes appear only in:
- `HeuristicCode` enum
- Skill file traceability footers
- `clean-code-baseline.json`
- TODO comments injected by the baseline pass

They never appear in build output, `cleanCodeExplain` invocation syntax,
SKILLS.md descriptions, or verbal communication about the plugin.

---

## 14. Comparing Recipe Findings with Claude's Skill-Based Scan

### The Core Idea

OpenRewrite recipes detect patterns structurally — deterministic, fast,
consistent, but limited to static AST analysis. Claude reading source
files directly using skills detects patterns semantically — contextual,
capable of reasoning about intent, without structural guarantees.

Running both and comparing produces a calibrated finding set where they
agree, and a gap analysis where they diverge. The divergences are the
most valuable output.

### The Four Quadrants

| | Recipe finds it | Recipe misses it |
|---|---|---|
| **Claude finds it** | High confidence — both agree | Semantic-only — recipe gap |
| **Claude misses it** | Possible false positive | Clean — neither sees a problem |

**Quadrant 1 (both agree):** Highest confidence. Safe for `[autofix]`
regardless of individual confidence level.

**Quadrant 2 (Claude only):** Most valuable. Things semantic reasoning
sees that structural analysis cannot — intent violations, abstraction
mismatches, feature envy. Recurring Quadrant 2 findings are candidates
for new recipes.

**Quadrant 3 (recipe only):** Worth reviewing. Either the recipe is
triggering on something Claude reads as contextually acceptable, or
Claude is missing something. Feeds threshold tuning.

**Quadrant 4 (neither):** Clean by both measures.

### Claude's Scan Prompt

```
Read the following source files. Using the patterns described in
.claude/skills/ch7-exception-handling.md, identify every location
that violates the exception handling rules. For each finding produce
a structured result in this exact JSON format:

{
  "file": "relative/path/to/File.java",
  "line": 123,
  "code": "Ch7.1",
  "description": "brief description of the specific violation",
  "confidence": "HIGH|MEDIUM|LOW",
  "reasoning": "one sentence explaining why this is a violation"
}

Do not fix anything. Do not output anything other than the JSON array.
```

The `reasoning` field is what makes Quadrant 3 analysis possible —
without it you know Claude disagreed but not why.

### Comparison Output

`build/reports/clean-code/scan-comparison.json`:

```json
{
  "file": "src/main/java/ProfilerEngine.java",
  "line": 203,
  "code": "Ch7.1",
  "recipe": { "found": true, "confidence": "MEDIUM" },
  "claude": {
    "found": true,
    "confidence": "HIGH",
    "reasoning": "Catch block logs ProfilerException and returns void — caller cannot distinguish success from failure"
  },
  "quadrant": 1,
  "recommended_action": "autofix"
}
```

**recommended_action derivation:**

| Quadrant | Recommended action |
|---|---|
| 1 — both agree | `autofix` if either HIGH, `review` otherwise |
| 2 — Claude only | `review` — potential recipe gap |
| 3 — recipe only | `review` — potential threshold issue |
| 4 — neither | omitted |

### Running the Comparison

```
./gradlew cleanCodeCompare
```

Produces `scan-comparison.json` and a human-readable HTML report with
quadrant breakdown as the headline figure.

`cleanCodeCompare` must declare the OpenRewrite LST as a task input
dependency rather than triggering a fresh parse. The LST produced by
`analyseCleanCode` is serialised to `build/cleancode/lst/` and consumed
by both `cleanCodeBaseline` and `cleanCodeCompare`. A second call to
`OpenRewriteFindingSource.collectFindings()` in the compare task must
reuse this serialised LST, not re-parse source files. Re-parsing on
every compare run would double the most expensive step in the pipeline.

### Feedback Loops

**Quadrant 2 → new recipes:** When ≥ 3 instances share a structural
pattern in Claude's `reasoning`, write an OpenRewrite recipe for it.
Run `cleanCodeCompare` again — findings should move to Quadrant 1.

**Quadrant 3 → threshold tuning:** Claude's `reasoning` explains why
it considers the finding benign — the input needed to decide whether
to raise the threshold, add an exclusion condition, or lower confidence.

### Scope

Claude's skill-based scan is a calibration layer, not a build-time check.
Run it:
- At baseline establishment — to populate Quadrant 2 findings recipes
  will never see
- When introducing a new recipe — to validate findings before committing
  to thresholds
- Periodically on hotspot files — high-churn classes where semantic
  drift is most likely

Recipe scanning runs on every build. Claude scanning runs on demand.

---

## 15. Narrative-Only Patterns

These go in the standing instructions block of CLAUDE.md. They cannot
be detected statically but are often the most important patterns for
Claude Code — the ones a human architect would brief a new team member on:

- **G31** Hidden temporal couplings — methods must be called in order;
  nothing in the type system enforces it
- **G34** Functions should descend only one level of abstraction — mixing
  orchestration and implementation in the same method
- **G26** Be precise — returning `List` when one result expected, float
  for money, `Date` instead of typed temporal
- **G14** Feature envy — deeper than static analysis can see reliably

These are placed in CLAUDE.md as standing instructions rather than
finding sections, since there are no recipe findings to attach them to.

---

## 16. Domain-Specific Recipe Tier

Beyond the Clean Code heuristics there is a fourth tier: project-specific
antipatterns that don't exist in the book but follow the same detection
architecture. These use the same `ScanningRecipe` + DataTable shape and
feed into the same CLAUDE.md pipeline.

Examples for the BI platform:

- Raw `DSLContext.fetch()` calls outside the approved profiling query path
  — should always go through the profiler wrapper
- ECharts option objects constructed inline rather than via the `EMap`
  fluent builder
- `ColumnProfile` instances created outside the staged pipeline

These use a project-specific `HeuristicCode` namespace (e.g. `PROJ1`,
`PROJ2`) rather than Martin codes or chapter codes, and point to
`project-conventions.md` as their skill file.

The domain recipe tier is where the plugin pays back its investment most
directly — generic tools can never catch project-specific misuse of
approved APIs, but a custom recipe can. These recipes are also the lowest
false-positive risk because the detection is exact (specific class and
method names) rather than heuristic.
