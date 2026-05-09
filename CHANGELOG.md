# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the
project uses semantic-ish versioning (the rule set is the public API;
breaking heuristic-code renames bump the minor).

## [0.1.4] — 2026-05-09

### Fixed — heuristic false positives

All seven fixes were reported via `CLEANCODE_PLUGIN_FEEDBACK.md` from a
v0.1.3 consumer (`io.github.fiftieshousewife:system-out-to-lombok-log4j`)
and ship with regression tests pulled directly from that file.

- **G18.1** — count inherited instance methods as instance binding.
  Previously, an unqualified call (e.g. `getCursor()` inherited from
  `JavaIsoVisitor`) whose type the parser couldn't resolve silently
  passed the "no instance state" check. Visitors extending OpenRewrite
  parents were flagged G18 even though declaring those methods static
  would break the inherited override.
- **G18.2** — skip candidates targeted by `Class::method` method
  references anywhere in the corpus. Smarter than the literal
  "private-only" cheap fallback proposed in the feedback: keeps the
  rule's signal on package-private/public methods that nothing
  references, drops it where an unbound method-reference SAM would
  break.
- **G19.1** — skip stream-chain arguments whose intermediate hops are
  structurally explained. `.filter(imp -> !isJulLoggerFqn(imp))` no
  longer flags the chain as missing an explanatory variable; the
  lambda body is the *result* of a G19 refactor, not a candidate for
  one.
- **G19.2** — skip string-concat returns inside `get[A-Z].*` methods.
  A multi-line `+`-joined string-literal return inside a getter is its
  own explanation; extracting to `private static final DESCRIPTION`
  adds indirection without benefit.
- **G19.3** — skip uniform `||`/`&&` chains of identical-shape calls.
  Five `X.matches(method) || Y.matches(method) || ...` reads as "is
  this an SLF4J log call"; the operator does the explaining.
- **G30.1** — coalesce same-shape guard clauses. Three consecutive
  `if (!precondition) return Optional.empty();` guards are one
  composite precondition, not three behaviours. Distinct-shape guards
  (`throw` vs `return -1` vs `return 0`) still fire.
- **J3.1** — skip enum suggestion for private single-use constants in
  a single expression. Five private `MethodMatcher` constants each
  referenced once in one boolean expression don't pay back the enum
  overhead (`values()`, constructor, lookup).

### Fixed — task lookup

- **`cleanCodeExplain --finding=G18`** now resolves. Previously only
  four friendly aliases (`error-handling`, `nulls`, `classes`,
  `functions`) were accepted; raw heuristic codes from the build report
  fell through to "No skill file found" even though
  `SkillPathRegistry` had the mappings. The task now parses raw codes
  and normalises dot/hyphen separators (`Ch7.1` → `Ch7_1` enum
  constant). A new test asserts every code in the registry is
  resolvable, so the alias path and registry can't drift apart.

### Added

- **OpenRewrite recipe modules** wired into the version catalog and
  aggregated where applicable: `rewrite-static-analysis 2.34.0`,
  `rewrite-logging-frameworks 3.27.2`, `rewrite-testing-frameworks
  3.35.2`, `rewrite-migrate-java 3.34.0`.
- **Forbidden APIs** as a warn-only build-time gate (`jdk-system-out`,
  `jdk-deprecated`, `jdk-internal` signature bundles).
- **`Slf4jBestPractices`** composite recipe added to
  `HarnessRecipePass.deterministicRecipes`.
- **PMD `AvoidFieldNameMatchingMethodName`** enabled via a curated
  ruleset.
- **Recipe-diff harness**
  (`refactoring/src/test/.../upstreamdiff/RecipeDiffHarness`) that
  classifies each fixture into `bothNoOp` / `equivalent` / `divergent`
  / `onlyOurs` / `onlyUpstream`. Drives the per-pair Phase B
  evaluation rather than blind deletion.
- **Diff baselines** locked for `UseTryWithResources` (strict subset
  under no-classpath parsing — keep ours), `AddFinal`
  (`FinalizeMethodArguments` + `FinalizeLocalVariables` are a strict
  superset — keep ours), `RestoreInterruptFlag`
  (`InterruptedExceptionHandling` is a strict superset — keep ours).

### Changed

- **`DeleteUnusedImportRecipe`** removed; the registry uses
  `org.openrewrite.java.RemoveUnusedImports` directly.
- **`ShortenFullyQualifiedReferencesRecipe`** removed; harness/registry
  use `org.openrewrite.java.ShortenFullyQualifiedTypeReferences`.
- **`T9` (slow tests)** now treated as environment-dependent alongside
  `E1` — counts vary with machine load and JVM warm-up, not source
  changes. Excluded from the deterministic drift-checked summary so
  `dogfood` doesn't fail on busy CI runs.

### Documentation

- README — Suppressions section now lists `@SuppressWarnings("CleanCode:Gxx")`
  alongside `@SuppressCleanCode`. Adds a "What the localhost:7070
  buttons do" subsection mapping each button to the file it persists
  to (source file for Suppress, `build.gradle.kts` for Disable and
  Tune).

## [0.1.3] and earlier

No changelog was kept prior to 0.1.4. Refer to the git history.

[0.1.4]: https://github.com/fiftiesHousewife/Clean-Claude/releases/tag/v0.1.4
