# Finding Sources

This document describes each static analysis tool integrated into the Clean Code plugin, the rules it detects, and how each rule maps to a *Clean Code* heuristic. Every finding preserves provenance: the `tool` field identifies the source and `ruleRef` names the specific rule.

For full heuristic descriptions see [HEURISTICS.md](HEURISTICS.md).

---

## Checkstyle

[Checkstyle](https://checkstyle.org/) enforces coding conventions and formatting. The plugin bundles a default configuration if the project has none.

**Tool version:** 10.21.4

| Rule | Heuristic | Severity | Confidence | Documentation |
|---|---|---|---|---|
| AnonInnerLength | [G30](HEURISTICS.md#g30-functions-should-do-one-thing) | WARNING | MEDIUM | [link](https://checkstyle.org/checks/sizes/anoninnerlength.html) |
| AvoidStarImport | [J1](HEURISTICS.md#j1-avoid-long-import-lists-by-using-wildcards) | WARNING | HIGH | [link](https://checkstyle.org/checks/imports/avoidstarimport.html) |
| EmptyBlock | [G4](HEURISTICS.md#g4-overridden-safeties) | WARNING | HIGH | [link](https://checkstyle.org/checks/blocks/emptyblock.html) |
| EmptyLineSeparator | [G10](HEURISTICS.md#g10-vertical-separation) | WARNING | MEDIUM | [link](https://checkstyle.org/checks/whitespace/emptylineseparator.html) |
| FileLength | [Ch10.1](HEURISTICS.md#ch101-classes-should-be-small) | WARNING | MEDIUM | [link](https://checkstyle.org/checks/sizes/filelength.html) |
| FinalLocalVariable | [G22](HEURISTICS.md#g22-make-logical-dependencies-physical) | WARNING | HIGH | [link](https://checkstyle.org/checks/coding/finallocalvariable.html) |
| HideUtilityClassConstructor | [G18](HEURISTICS.md#g18-inappropriate-static) | WARNING | HIGH | [link](https://checkstyle.org/checks/design/hideutilityclassconstructor.html) |
| IllegalImport | [G12](HEURISTICS.md#g12-clutter) | WARNING | HIGH | [link](https://checkstyle.org/checks/imports/illegalimport.html) |
| InterfaceIsType | [J2](HEURISTICS.md#j2-dont-inherit-constants) | WARNING | HIGH | [link](https://checkstyle.org/checks/design/interfaceistype.html) |
| LeftCurly | [G24](HEURISTICS.md#g24-follow-standard-conventions) | WARNING | HIGH | [link](https://checkstyle.org/checks/blocks/leftcurly.html) |
| LineLength | [G24](HEURISTICS.md#g24-follow-standard-conventions) | INFO | HIGH | [link](https://checkstyle.org/checks/sizes/linelength.html) |
| LocalVariableName | [N1](HEURISTICS.md#n1-choose-descriptive-names) | WARNING | MEDIUM | [link](https://checkstyle.org/checks/naming/localvariablename.html) |
| MagicNumber | [G25](HEURISTICS.md#g25-replace-magic-numbers-with-named-constants) | WARNING | HIGH | [link](https://checkstyle.org/checks/coding/magicnumber.html) |
| MethodLength | [G30](HEURISTICS.md#g30-functions-should-do-one-thing) | WARNING | MEDIUM | [link](https://checkstyle.org/checks/sizes/methodlength.html) |
| MethodName | [N1](HEURISTICS.md#n1-choose-descriptive-names) | WARNING | MEDIUM | [link](https://checkstyle.org/checks/naming/methodname.html) |
| NeedBraces | [G24](HEURISTICS.md#g24-follow-standard-conventions) | WARNING | MEDIUM | [link](https://checkstyle.org/checks/blocks/needbraces.html) |
| OneTopLevelClass | [G12](HEURISTICS.md#g12-clutter) | WARNING | HIGH | [link](https://checkstyle.org/checks/design/onetoplevelclass.html) |
| ParameterNumber | [F1](HEURISTICS.md#f1-too-many-arguments) | WARNING | HIGH | [link](https://checkstyle.org/checks/sizes/parameternumber.html) |
| RedundantImport | [G12](HEURISTICS.md#g12-clutter) | INFO | HIGH | [link](https://checkstyle.org/checks/imports/redundantimport.html) |
| RightCurly | [G24](HEURISTICS.md#g24-follow-standard-conventions) | WARNING | HIGH | [link](https://checkstyle.org/checks/blocks/rightcurly.html) |
| SimplifyBooleanExpression | [G28](HEURISTICS.md#g28-encapsulate-conditionals) | WARNING | HIGH | [link](https://checkstyle.org/checks/coding/simplifybooleanexpression.html) |
| SimplifyBooleanReturn | [G28](HEURISTICS.md#g28-encapsulate-conditionals) | WARNING | HIGH | [link](https://checkstyle.org/checks/coding/simplifybooleanreturn.html) |
| TypeName | [N1](HEURISTICS.md#n1-choose-descriptive-names) | WARNING | MEDIUM | [link](https://checkstyle.org/checks/naming/typename.html) |
| UnusedImports | [G12](HEURISTICS.md#g12-clutter) | INFO | HIGH | [link](https://checkstyle.org/checks/imports/unusedimports.html) |
| VisibilityModifier | [G8](HEURISTICS.md#g8-too-much-information) | WARNING | MEDIUM | [link](https://checkstyle.org/checks/design/visibilitymodifier.html) |
| WhitespaceAround | [G24](HEURISTICS.md#g24-follow-standard-conventions) | WARNING | HIGH | [link](https://checkstyle.org/checks/whitespace/whitespacearound.html) |

---

## PMD

[PMD](https://pmd.github.io/) detects common programming flaws including dead code, empty blocks, overcomplicated expressions, and coding style issues.

**Tool version:** 7.9.0

| Rule | Heuristic | Severity | Confidence | Documentation |
|---|---|---|---|---|
| AvoidConstantsInterface | [J2](HEURISTICS.md#j2-dont-inherit-constants) | WARNING | HIGH | [link](https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#avoidconstantsinterface) |
| AvoidReassigningParameters | [G22](HEURISTICS.md#g22-make-logical-dependencies-physical) | WARNING | HIGH | [link](https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#avoidreassigningparameters) |
| CloseResource | [G4](HEURISTICS.md#g4-overridden-safeties) | WARNING | MEDIUM | [link](https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#closeresource) |
| CommentedOutCodeLine | [C5](HEURISTICS.md#c5-commented-out-code) | WARNING | HIGH | [link](https://pmd.github.io/pmd/pmd_rules_java_codestyle.html) |
| CouplingBetweenObjects | [G8](HEURISTICS.md#g8-too-much-information) | WARNING | MEDIUM | [link](https://pmd.github.io/pmd/pmd_rules_java_design.html#couplingbetweenobjects) |
| CyclomaticComplexity | [G30](HEURISTICS.md#g30-functions-should-do-one-thing) | WARNING | MEDIUM | [link](https://pmd.github.io/pmd/pmd_rules_java_design.html#cyclomaticcomplexity) |
| DataClass | [G17](HEURISTICS.md#g17-misplaced-responsibility) | INFO | LOW | [link](https://pmd.github.io/pmd/pmd_rules_java_design.html#dataclass) |
| EmptyCatchBlock | [G4](HEURISTICS.md#g4-overridden-safeties) | ERROR | HIGH | [link](https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#emptycatchblock) |
| EmptyIfStmt | [G12](HEURISTICS.md#g12-clutter) | WARNING | HIGH | [link](https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#emptyifstmt) |
| ExcessiveMethodLength | [G30](HEURISTICS.md#g30-functions-should-do-one-thing) | WARNING | MEDIUM | [link](https://pmd.github.io/pmd/pmd_rules_java_design.html#excessivemethodlength) |
| ExcessivePublicCount | [G8](HEURISTICS.md#g8-too-much-information) | WARNING | HIGH | [link](https://pmd.github.io/pmd/pmd_rules_java_design.html#excessivepubliccount) |
| GodClass | [G8](HEURISTICS.md#g8-too-much-information) | ERROR | MEDIUM | [link](https://pmd.github.io/pmd/pmd_rules_java_design.html#godclass) |
| LooseCoupling | [G8](HEURISTICS.md#g8-too-much-information) | WARNING | MEDIUM | [link](https://pmd.github.io/pmd/pmd_rules_java_design.html#loosecoupling) |
| NPathComplexity | [G30](HEURISTICS.md#g30-functions-should-do-one-thing) | WARNING | MEDIUM | [link](https://pmd.github.io/pmd/pmd_rules_java_design.html#npathcomplexity) |
| SwitchStmtsShouldHaveDefault | [G23](HEURISTICS.md#g23-prefer-polymorphism-to-ifelse-or-switchcase) | INFO | MEDIUM | [link](https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#switchstmtsshouldhavedefault) |
| TooManyFields | [G8](HEURISTICS.md#g8-too-much-information) | WARNING | MEDIUM | [link](https://pmd.github.io/pmd/pmd_rules_java_design.html#toomanyfields) |
| TooManyMethods | [G8](HEURISTICS.md#g8-too-much-information) | WARNING | MEDIUM | [link](https://pmd.github.io/pmd/pmd_rules_java_design.html#toomanymethods) |
| UnusedImports | [G12](HEURISTICS.md#g12-clutter) | INFO | HIGH | [link](https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#unusedimports) |
| UnusedLocalVariable | [G9](HEURISTICS.md#g9-dead-code) | INFO | HIGH | [link](https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#unusedlocalvariable) |
| UnusedPrivateMethod | [F4](HEURISTICS.md#f4-dead-function) | WARNING | HIGH | [link](https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#unusedprivatemethod) |
| UseLocaleWithCaseConversions | [G26](HEURISTICS.md#g26-be-precise) | WARNING | HIGH | [link](https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#uselocalewithcaseconversions) |

---

## SpotBugs

[SpotBugs](https://spotbugs.github.io/) performs bytecode analysis to find bug patterns, null pointer risks, and concurrency issues.

**Tool version:** 4.9.3

| Bug Pattern | Heuristic | Severity | Confidence | Documentation |
|---|---|---|---|---|
| BAD_PRACTICE/BC_UNCONFIRMED_CAST | [G4](HEURISTICS.md#g4-overridden-safeties) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#bc-unconfirmed-cast-bc-unconfirmed-cast) |
| BAD_PRACTICE/CT_CONSTRUCTOR_THROW | [G4](HEURISTICS.md#g4-overridden-safeties) | WARNING | MEDIUM | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#ct-be-wary-of-letting-constructors-throw-exceptions-ct-constructor-throw) |
| BAD_PRACTICE/DE_MIGHT_IGNORE | [G4](HEURISTICS.md#g4-overridden-safeties) | ERROR | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#de-method-might-ignore-exception-de-might-ignore) |
| BAD_PRACTICE/DM_DEFAULT_ENCODING | [G26](HEURISTICS.md#g26-be-precise) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#dm-reliance-on-default-encoding-dm-default-encoding) |
| BAD_PRACTICE/EQ_COMPARETO_USE_OBJECT_EQUALS | [G11](HEURISTICS.md#g11-inconsistency) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#eq-class-defines-compareto-and-uses-object-equals-eq-compareto-use-object-equals) |
| BAD_PRACTICE/ES_COMPARING_STRINGS_WITH_EQ | [G26](HEURISTICS.md#g26-be-precise) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#es-comparison-of-string-objects-using-or-es-comparing-strings-with-eq) |
| BAD_PRACTICE/HE_EQUALS_NO_HASHCODE | [G11](HEURISTICS.md#g11-inconsistency) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#he-class-defines-equals-and-uses-object-hashcode-he-equals-no-hashcode) |
| BAD_PRACTICE/NP_NULL_PARAM_DEREF | [Ch7.2](HEURISTICS.md#ch72-dont-return-null) | ERROR | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#np-null-value-is-guaranteed-to-be-dereferenced-np-null-param-deref) |
| BAD_PRACTICE/OS_OPEN_STREAM | [G4](HEURISTICS.md#g4-overridden-safeties) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#os-method-may-fail-to-close-stream-os-open-stream) |
| BAD_PRACTICE/RV_RETURN_VALUE_IGNORED_BAD_PRACTICE | [G4](HEURISTICS.md#g4-overridden-safeties) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#rv-method-ignores-exceptional-return-value-rv-return-value-ignored-bad-practice) |
| BAD_PRACTICE/ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD | [G18](HEURISTICS.md#g18-inappropriate-static) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#st-write-to-static-field-from-instance-method-st-write-to-static-from-instance-method) |
| CORRECTNESS/NP_ALWAYS_NULL | [Ch7.2](HEURISTICS.md#ch72-dont-return-null) | ERROR | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#np-null-pointer-dereference-np-always-null) |
| CORRECTNESS/NP_NULL_ON_SOME_PATH | [Ch7.2](HEURISTICS.md#ch72-dont-return-null) | ERROR | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#np-possible-null-pointer-dereference-np-null-on-some-path) |
| CORRECTNESS/RE_BAD_SYNTAX_FOR_REGULAR_EXPRESSION | [G4](HEURISTICS.md#g4-overridden-safeties) | ERROR | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#re-invalid-syntax-for-regular-expression-re-bad-syntax-for-regular-expression) |
| CORRECTNESS/RV_RETURN_VALUE_IGNORED | [G4](HEURISTICS.md#g4-overridden-safeties) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#rv-method-ignores-return-value-rv-return-value-ignored) |
| MALICIOUS_CODE/EI_EXPOSE_REP | [G8](HEURISTICS.md#g8-too-much-information) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#ei-may-expose-internal-representation-by-returning-reference-to-mutable-object-ei-expose-rep) |
| MALICIOUS_CODE/EI_EXPOSE_REP2 | [G8](HEURISTICS.md#g8-too-much-information) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#ei2-may-expose-internal-representation-by-incorporating-reference-to-mutable-object-ei-expose-rep2) |
| MALICIOUS_CODE/MS_MUTABLE_ARRAY | [G8](HEURISTICS.md#g8-too-much-information) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#ms-field-is-a-mutable-array-ms-mutable-array) |
| MALICIOUS_CODE/MS_MUTABLE_COLLECTION_PKGPROTECT | [G8](HEURISTICS.md#g8-too-much-information) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#ms-field-is-a-mutable-collection-which-should-be-package-protected-ms-mutable-collection-pkgprotect) |
| MALICIOUS_CODE/MS_SHOULD_BE_FINAL | [G22](HEURISTICS.md#g22-make-logical-dependencies-physical) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#ms-field-isn-t-final-but-should-be-ms-should-be-final) |
| PERFORMANCE/DM_BOXED_PRIMITIVE_FOR_COMPARE | [G26](HEURISTICS.md#g26-be-precise) | INFO | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#dm-boxing-unboxing-to-parse-a-primitive-dm-boxed-primitive-for-compare) |
| PERFORMANCE/DM_NUMBER_CTOR | [G26](HEURISTICS.md#g26-be-precise) | INFO | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#dm-method-invokes-inefficient-number-constructor-use-static-valueof-instead-dm-number-ctor) |
| PERFORMANCE/SIC_INNER_SHOULD_BE_STATIC | [G18](HEURISTICS.md#g18-inappropriate-static) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#sic-should-be-a-static-inner-class-sic-inner-should-be-static) |
| PERFORMANCE/SS_SHOULD_BE_STATIC | [G18](HEURISTICS.md#g18-inappropriate-static) | WARNING | MEDIUM | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#ss-unread-field-should-this-field-be-static-ss-should-be-static) |
| PERFORMANCE/UUF_UNUSED_FIELD | [G9](HEURISTICS.md#g9-dead-code) | INFO | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#uuf-unused-field-uuf-unused-field) |
| PERFORMANCE/WMI_WRONG_MAP_ITERATOR | [G30](HEURISTICS.md#g30-functions-should-do-one-thing) | INFO | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#wmi-inefficient-use-of-keyset-iterator-instead-of-entryset-iterator-wmi-wrong-map-iterator) |
| STYLE/BC_UNCONFIRMED_CAST_OF_RETURN_VALUE | [G4](HEURISTICS.md#g4-overridden-safeties) | WARNING | MEDIUM | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#bc-unconfirmed-cast-of-return-value-bc-unconfirmed-cast-of-return-value) |
| STYLE/DB_DUPLICATE_BRANCHES | [G5](HEURISTICS.md#g5-duplication) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#db-method-uses-the-same-code-for-two-branches-db-duplicate-branches) |
| STYLE/DLS_DEAD_LOCAL_STORE | [G9](HEURISTICS.md#g9-dead-code) | INFO | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#dls-dead-store-to-local-variable-dls-dead-local-store) |
| STYLE/EQ_DOESNT_OVERRIDE_EQUALS | [G11](HEURISTICS.md#g11-inconsistency) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#eq-class-doesn-t-override-equals-in-superclass-eq-doesnt-override-equals) |
| STYLE/NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE | [Ch7.2](HEURISTICS.md#ch72-dont-return-null) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#np-possible-null-pointer-dereference-from-return-value-of-called-method-np-null-on-some-path-from-return-value) |
| STYLE/RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE | [Ch7.2](HEURISTICS.md#ch72-dont-return-null) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#rcn-redundant-nullcheck-of-value-known-to-be-non-null-rcn-redundant-nullcheck-of-nonnull-value) |
| STYLE/RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE | [Ch7.2](HEURISTICS.md#ch72-dont-return-null) | ERROR | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#rcn-nullcheck-of-value-previously-dereferenced-rcn-redundant-nullcheck-would-have-been-a-npe) |
| STYLE/SF_SWITCH_NO_DEFAULT | [G23](HEURISTICS.md#g23-prefer-polymorphism-to-ifelse-or-switchcase) | INFO | MEDIUM | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#sf-switch-statement-found-where-default-case-is-missing-sf-switch-no-default) |
| STYLE/UC_USELESS_CONDITION | [G9](HEURISTICS.md#g9-dead-code) | WARNING | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#uc-condition-has-no-effect-uc-useless-condition) |
| STYLE/URF_UNREAD_FIELD | [G9](HEURISTICS.md#g9-dead-code) | INFO | HIGH | [link](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html#urf-unread-field-urf-unread-field) |

**Category fallback:** Any CORRECTNESS bug not listed above maps to [G4](HEURISTICS.md#g4-overridden-safeties) at ERROR severity.

---

## CPD (Copy-Paste Detector)

[CPD](https://pmd.github.io/pmd/pmd_userdocs_cpd.html) detects duplicated code blocks using token-based analysis.

**Tool version:** 7.9.0 (bundled with PMD)

| Detection | Heuristic | Severity | Confidence |
|---|---|---|---|
| Token-based duplication | [G5](HEURISTICS.md#g5-duplication) | INFO (below threshold), WARNING (above) | HIGH |

Configurable via `cleanCode.thresholds.cpdMinimumTokens` (default: 50).

---

## JaCoCo

[JaCoCo](https://www.jacoco.org/jacoco/) measures test coverage at the line and branch level.

**Tool version:** 0.8.12

| Detection | Heuristic | Severity | Confidence |
|---|---|---|---|
| Overall line coverage below threshold | [T1](HEURISTICS.md#t1-insufficient-tests) | ERROR (< 50%), INFO (>= 50%) | HIGH |
| Per-class coverage gaps | [T8](HEURISTICS.md#t8-test-coverage-patterns-can-be-revealing) | WARNING | MEDIUM |
| JaCoCo report present | [T2](HEURISTICS.md#t2-use-a-coverage-tool) | INFO | HIGH |

---

## Surefire (Gradle Test Results)

Parses JUnit XML test results from `build/test-results/test/`. Despite the name, this adapter reads Gradle's JUnit XML output (same format as Maven Surefire).

| Detection | Heuristic | Severity | Confidence |
|---|---|---|---|
| Skipped test | [T3](HEURISTICS.md#t3-dont-skip-trivial-tests) | INFO | HIGH |
| Slow test (> 5s) | [T9](HEURISTICS.md#t9-tests-should-be-fast) | WARNING | HIGH |
| Very slow test (> 30s) | [T9](HEURISTICS.md#t9-tests-should-be-fast) | ERROR | HIGH |
| High skip percentage (> 10%) | [T3](HEURISTICS.md#t3-dont-skip-trivial-tests) | WARNING | HIGH |

---

## Dependency Updates (Ben-Manes)

[gradle-versions-plugin](https://github.com/ben-manes/gradle-versions-plugin) reports outdated dependencies. This is an **opt-in** source: the plugin checks for the `dependencyUpdates` task at configuration time and wires it into `analyseCleanCode` only if present.

| Detection | Heuristic | Severity | Confidence |
|---|---|---|---|
| Outdated dependency | [E1](HEURISTICS.md#e1-build-requires-more-than-one-step) | INFO | HIGH |

---

## OpenRewrite (Custom Recipes)

40 custom [OpenRewrite](https://docs.openrewrite.org/) `ScanningRecipe` implementations that detect Clean Code patterns via AST analysis.

**Tool version:** 8.40.2

**Important:** Requires JDK 21. See [README.md](README.md#build) for details.

See [HEURISTICS.md](HEURISTICS.md) for the full list of recipes and which heuristic each detects. Recipes are configurable via `cleanCode.thresholds` and individually disableable via `cleanCode.disabledRecipes`.

---

## Claude Review (LLM Assessment)

Uses the [Claude API](https://docs.anthropic.com/en/docs) to assess source files for subjective Clean Code heuristics that require semantic understanding beyond what static analysis can detect. This is an **opt-in** source: it only runs when the `ANTHROPIC_API_KEY` environment variable is set.

**Default model:** claude-sonnet-4-6

**Confidence:** Always LOW — LLM assessments are non-deterministic and advisory.

| Code | Heuristic | What Claude assesses |
|---|---|---|
| [C2](HEURISTICS.md#c2-obsolete-comment) | Obsolete Comment | Comments that no longer match the code they describe |
| [G6](HEURISTICS.md#g6-code-at-wrong-level-of-abstraction) | Code at Wrong Level of Abstraction | Methods or fields that belong in a different class |
| [G7](HEURISTICS.md#g7-base-classes-depending-on-their-derivatives) | Base Classes Depending on Derivatives | Base classes that import or reference subclasses |
| [G13](HEURISTICS.md#g13-artificial-coupling) | Artificial Coupling | Classes coupled for no structural reason |
| [G15](HEURISTICS.md#g15-selector-arguments) | Selector Arguments | Boolean/enum/string params that select behaviour |
| [G20](HEURISTICS.md#g20-function-names-should-say-what-they-do) | Function Names Should Say What They Do | Methods whose names don't communicate intent |
| [G31](HEURISTICS.md#g31-hidden-temporal-couplings) | Hidden Temporal Couplings | Operations that must be called in order but don't enforce it |
| [N4](HEURISTICS.md#n4-unambiguous-names) | Unambiguous Names | Names that could refer to multiple things |

### Configuration

```kotlin
cleanCode {
    claudeReview {
        enabled.set(true)                    // default: true (gated on API key)
        model.set("claude-sonnet-4-6")       // default
        maxFilesPerRun.set(50)               // default — caps API usage per build
        minFileLines.set(10)                 // default — skip trivial files
        codes.set(listOf("G6", "G7", "G13", "G15", "G20", "G31", "C2", "N4"))
        excludePatterns.set(listOf("**/generated/**"))
    }
}
```

### Caching

Results are cached by SHA-256 of file content + enabled codes in `build/claude-review-cache/`. Unchanged files skip the API call entirely. Run `./gradlew clean` to clear the cache.

### Provenance

Each finding includes `tool: "claude-review"` and `metadata.model` identifying which Claude model produced it.
