# Dogfood triage — 2026-05-03

Sweep over `analyseCleanCode` against the plugin's own source. Total:
**1464 findings** across 38 codes. This document records what we **agreed
with and fixed**, what we **agreed with but deferred**, and what we
**disagreed with** (with proposed recipe fixes).

## Already fixed this session (committed)

| Issue | Fix |
| --- | --- |
| G23 (StringSwitch) snippet on class line | New `lineOfSwitchInMethod` source scan; `mapStringSwitch` resolves to the switch line |
| Ch7.1 + G4 double-counting empty catches | `CatchLogContinueRecipe` skips empty bodies (G4 owns them); adapter drops Ch7.1 findings whose (file, line) collides with a G4 finding |
| G16 (NestedTernary) snippet on class line | Route through `findingForMethod` |
| G14 / G19 / G33 / G34 / N7 snippet on class line | Same — `findingForMethod` for all five |
| C3 false positive on `// line comment` (single-param method, common-word param) | Require ≥2 params before treating param-name overlap as the only signal |
| C3 false positive on long elaborating comments that mention method words | Cap comment length at method-word + 2 on the `allWordsPresent` path |

## Findings — agree, deferred (real but bigger work)

These are legitimate smells but require structural changes, not recipe
tuning.

### G30 — 272 findings (Functions Should Do One Thing)

Mostly checkstyle `MethodLength` (max 50). Real smells; the fix is to
extract subordinate methods. **Don't tune the recipe.** Tackle in a
focused refactor pass per file, biggest first
(`CleanCodePlugin.apply()`, `ServeTask.serve()`, `PromptBuilder.recipesBlock()`).

### G24 — 154 findings (Standard Conventions / LineLength)

Real — just lots of lines >120 chars. Mechanical fix; can be batched
with a line-wrap pass. Lower priority than structural smells.

### G10 — 102 findings (Vertical Separation)

Real — declarations far from first use. Per-method local-variable
move-down. Mechanical but tedious.

### Ch10_1 — 94 findings (Class length > 150 lines)

Real SRP violations in long files (`HtmlReportWriter`,
`SkillFileScaffolder`, `ReworkCompareTask`, `SandboxAnalysis`,
`OpenRewriteFindingSource` itself). Each needs a split. **Don't tune the
threshold** — the threshold is the standard the project sets for itself.

### F2 — 92 findings (Output args / void mutation)

Real, but most cases in the plugin's own recipes (`visitMethodInvocation`
mutating an accumulator) are an OpenRewrite *idiom*, not a smell. The
recipe registers a writeback mutator. Worth a per-class
`@SuppressCleanCode({F2}, reason = "OpenRewrite ScanningRecipe accumulator
pattern")` on the recipe classes. Action: extend
`SuppressCleanCode` to cover the package-info or top-level recipe
classes systematically.

### G5 — 90 findings (Duplication / repeated string literals)

Mostly real — `"{{classLineCount}}"`, `"{{recordComponentCount}}"`
appear twice across template constants. Easy fix: extract to constants.
A few are false positives where the same literal appears in
non-substitutable contexts (e.g. test expectations) — but the volume is
small.

### G34 — 66 findings (Section comments)

Real — `// === Section ===` style markers. The fix recipe
`DeleteSectionCommentsRecipe` exists. Run as a batch.

### G28 — 64 findings (Encapsulate complex condition)

Real. `if (a && b && c)` style — extract to a named predicate. Recipe
`ExtractExplanatoryVariableRecipe` covers it.

### G31 — 63 findings (Hidden temporal couplings / consecutive void calls)

Mixed:
- **Real** when there's a hidden ordering invariant. E.g. `setX(); setY();
  setZ();` where Y depends on X.
- **False positives** when the calls are independent and order doesn't
  matter — e.g. `project.getPlugins().apply(...)` is the standard
  Gradle plugin-application pattern; there is no order dependency.

**Suggested recipe improvement:** require the consecutive calls to
target the **same receiver** before firing. Cross-receiver call
sequences (`a.f(); b.g(); c.h()`) are usually independent.

### Ch7_2 — 55 findings (Excessive null checks)

Real. Use `Optional`, `Map.getOrDefault`, or fail-fast at boundaries.

### F1 — 45 findings (ParameterNumber)

Real — methods with >4 parameters. Each is a candidate for a parameter
object (record).

### G19 — 40 findings (Missing explanatory variable)

Real after the anchor fix — now they correctly land on the method.
Per-finding decision: is the expression so opaque it deserves a name?

### C2 — 39 findings (Obsolete comment references)

The recipe is reasonable; finding count is real. Many comments reference
class names that *used to* exist (e.g. `// uses MCP`, `// see
SystemOutToSlf4jRecipeNoDeps`). Most should be deleted. A few are domain
acronyms (MCP = Model Context Protocol) that aren't class names — see
disagreement below.

### G35 — 34 findings (Magic numbers)

Real. `1000.0` for ms→s conversion is a classic example — extract
`MILLIS_PER_SECOND`.

### G12 — 33 findings (Unused / fully-qualified imports)

After this session's per-occurrence + FQN fixes, the remaining G12s
are mostly **unused imports** flagged by checkstyle `UnusedImports`.
Easy fix: delete them. The fix recipe
`ShortenFullyQualifiedReferencesRecipe` handles the FQN side; we still
need a `DeleteUnusedImportRecipe` (mentioned in the P1 backlog).

### J1 — 22 findings (Wildcard imports)

Real — `import java.util.*;` etc. Easy mechanical fix.

### T1 — 22 findings (Private methods that should be testable)

Real. Drop `private` to package-private on the listed methods.

### G4 — 23 findings (Swallowed exceptions, unchecked casts)

Real. `SuppressionIndex.build()` has two empty catches that should
either log or carry a `@SuppressCleanCode` with a reason. The
`@SuppressWarnings("unchecked")` cases require redesign, not
suppression.

## Findings — DISAGREE (recipe needs work)

### G31 — temporal coupling on `getPlugins().apply(...)` chains

`CleanCodePlugin.apply` and `applyStaticAnalysisPlugins` flag chains of
`project.getPlugins().apply(X.class); project.getPlugins().apply(Y.class);
...`. These are **independent plugin applications** — Gradle does the
ordering by topological sort of the plugin metadata. Order at the call
site is irrelevant.

**Suggested recipe fix:** in `TemporalCouplingRecipe`, require that the
consecutive void calls share the **same receiver chain** AND the
receiver is a non-static field/local (not a freshly-built chain like
`project.getPlugins()`). Calls of the form `framework.method(...)`
repeated with different arguments are usually a registration pattern,
not temporal coupling.

### F2 — output-argument flag on OpenRewrite visitor accumulators

`visitMethodInvocation`, `visitSwitch`, `visitCatch` are required by
the OpenRewrite `JavaIsoVisitor` contract. They take a mutable
accumulator parameter (a `Set<String>`, `List<Integer>`) and append to
it. The recipe's flag is technically correct but the pattern is
mandated by the framework — there's no "return the result instead"
option.

**Suggested recipe fix:** in `OutputArgumentRecipe`, skip method
declarations whose enclosing class extends `JavaIsoVisitor` (or has a
`@Override` annotation on a method called `visit*`). Alternative: ship
a package-level `@SuppressCleanCode({F2})` on `recipes/package-info.java`.

### G5 — duplication on template placeholder strings

`SkillFileScaffolder` flags `"{{classLineCount}}"` twice — once in the
template-fill loop and once in the docstring. They're **the same
string** but in different roles (placeholder vs. doc reference). A
"named constant" wouldn't actually de-duplicate; one of them is text in
a comment.

**Suggested recipe fix:** `MagicStringRecipe` should ignore literals
that appear inside Javadoc / line comments. Currently it's only
skipping comment lines via the snippet anchor, not at detection time.

### C3 — `// uses MCP` flagged as mumbling reference

After today's two fixes, the false-positive count dropped from 16 → 6.
The remaining 6 are all in the same shape: comments inside
`OpenRewriteFindingSource.lineOfClass` and a few similar methods that
talk about *positions* within the source file using words from the
method name. The recipe is now in a reasonable place; remaining cases
are borderline.

### C2 — `MCP` is not a class name

`HarnessRecipePassCli.java:32` flags a comment like
`// MCP server forwards stdin` as referencing an out-of-scope class.
**MCP** is an acronym (Model Context Protocol), not a Java type.

**Suggested recipe fix:** in `ObsoleteCommentRecipe`, only flag
references that look like Java identifiers — at minimum, require the
referenced word to either:
- Be present in the project's class index (we already build one), AND/OR
- Match the convention of an UpperCamelCase token of length ≥4.

A 3-letter all-caps acronym like `MCP` should never be flagged unless
there's a `class MCP` somewhere.

### G30 — checkstyle `MethodLength` mapped to "Functions Should Do One Thing"

The mapping is intentional per `HEURISTICS.md` (G30 covers
function-size smells). But the surface message is `[MethodLength]
Method apply length is 102 lines (max allowed is 50)` — checkstyle's
phrasing — which doesn't read like a Clean Code message. The user has
to translate.

**Suggested adapter improvement:** in `CheckstyleFindingSource`,
rewrite the message for `MethodLength` to read
`Method 'X' is N lines — split into single-purpose methods` so it
matches the rest of the G30 messages from
`WhitespaceSplitMethodRecipe` / `ImperativeLoopRecipe`.

## Summary

- **Recipe fixes shipped:** 6 (3 anchor fixes, 1 dedup, 2 C3 tightenings)
- **Total findings:** 1465 → 1464 (the C3 fixes removed 10 false
  positives; G34 increased by 8 because previously-collapsed duplicates
  now anchor at distinct methods — net is 1 less)
- **Codebase cleanups deferred:** G30 / G24 / G10 / Ch10_1 — structural
  refactors better tackled per-file
- **Recipe tunings to consider next:**
  1. `TemporalCouplingRecipe` should require same-receiver chains
  2. `OutputArgumentRecipe` should skip OpenRewrite visitor methods
  3. `MagicStringRecipe` should ignore literals inside comments
  4. `ObsoleteCommentRecipe` should not flag tokens that aren't Java
     identifiers in the project's class index
  5. `CheckstyleFindingSource` should rewrite `MethodLength` messages
     into Clean Code phrasing
