# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the
project uses semantic-ish versioning (the rule set is the public API;
breaking heuristic-code renames bump the minor).

## [0.2.1] — 2026-05-14

The 0.2.0 publish attempt on 2026-05-10 never reached Sonatype — the
local build's connection to the maven-central-build-service was
cancelled by the upstream proxy mid-upload and no deployment landed in
Sonatype Central. The 0.2.0 tag in git history points at the same code
that ships as 0.2.1; the version number bumped only to keep Maven
Central coordinates monotonic. Contents identical to the unpublished
0.2.0.

Hone-and-tighten release. The MCP server, the AI-vs-OpenRewrite
experiment harness, the seeded `sandbox/` demo module, the
`reworkCompare`/`reworkClass` agent-rework infrastructure, and the
optional `claude-review` LLM finding source are all removed. The
published plugin's transitive dependency on the Anthropic Java SDK
goes with them. Six consumer-reported bugs from
`CLEANCODE_FEEDBACK_0.1.4.md` are fixed.

### Breaking

- **Removed `mcp/` module + `.mcp.json`** (JSON-RPC tools server).
- **Removed `sandbox/` module** + the `cleanCodeSelfApply=true` opt-in
  toggle in `settings.gradle.kts`.
- **Removed `reworkCompare` and `reworkClass` Gradle tasks** along with
  the supporting `plugin/.../rework/` agent infrastructure
  (PromptBuilder, ReworkOrchestrator, AgentRunner, RunVariant, …) and
  the `scripts/watch-rework-run.sh` / `rework-class.sh` helpers.
- **Removed `claude-review/` module + the `cleanCode.claudeReview { }`
  extension block + `ANTHROPIC_API_KEY` plumbing.** `com.anthropic:anthropic-java`
  is no longer a transitive of the published plugin. C2, G6, G7, G13,
  G15, G20, G31, N4 are no longer covered by an LLM source — the
  static-analysis layer remains.
- **Removed the AI-vs-OpenRewrite experiment harness:** the `experiment/`
  directory (3.9 MB of baselines + cron logs + manual-pilot artefacts),
  `scripts/run-experiment.sh` + `cron-run-experiment.sh` +
  `experiment-{manual,recipe}-prompt.txt` + `run-single-file.sh` +
  `nightly-compare.sh` + `compare-runs.py`, the
  `.claude/skills/{experiment,experiment-save,experiment-analyse}`
  skills, and `docs/experiment-analysis-plan.md`. Net: ~6,900 source
  lines and ~20,000 generated-artefact lines deleted.

### Added

- **`E3` heuristic — Outdated Major-Version Dependency.** Patch and
  minor bumps stay on `E1`; major bumps surface as `E3` so consumers
  can triage them separately. Environment-dependent (excluded from
  drift-checks alongside `E1` and `T9`).
- **`G37` heuristic — Magic String Literals.** Repeated string
  literals (`"POST"`, `"UTF-8"`, …) now route to `G37` instead of
  `G5`. CPD block-duplication keeps `G5`; the two have different fix
  shapes (extract a constant vs refactor a block) and now have
  different codes.
- **`source` field in `findings.json`** (human-readable display name:
  `"OpenRewrite"`, `"CPD"`, `"JaCoCo"`, …) alongside the existing
  machine-readable `tool` id. Lets `jq` queries group by source
  without re-deriving from `tool`/`ruleRef`.

### Fixed — consumer-reported

All six items in
[`CLEANCODE_FEEDBACK_0.1.4.md`](CLEANCODE_FEEDBACK_0.1.4.md) ship
fixed in this release:

- **E1 no longer recommends alpha/RC/milestone versions.** The
  Ben-Manes report exposes `available.{release,milestone,integration}`;
  the source used to read `milestone` first, so a project on `1.0.0`
  could be told to bump to `2.0.0-alpha.1`. Now reads `release` only —
  pre-release versions never count as 'outdated'.
- **E1 no longer reports cleancode-internal dependencies.** Findings
  like `com.puppycrawl.tools:checkstyle [10.21.4 -> 13.4.2]` came from
  the plugin's bundled classpath, not the consumer's catalog. When
  `gradle/libs.versions.toml` exists, findings are scoped to
  coordinates declared there. When no catalog exists, a deny-list of
  cleancode-internal groups (puppycrawl, pmd, spotbugs, errorprone, …)
  is applied. The catalog parser recognises `module="g:n"`,
  `group="g",name="n"`, and shorthand `"g:n:v"` library declarations.
- **`G5` no longer conflates block-duplication and string-literal
  duplication.** See "Added" above.
- **`source` field in `findings.json` is no longer null.** See "Added"
  above.
- **Project-global findings (e.g. `T1` coverage) now emit JSON `null`
  for `sourceFile`/`startLine`/`endLine`** instead of `-1` sentinels.
  `jq '.startLine // "n/a"'` falls through cleanly; tooling no longer
  needs to special-case the magic number. Reader normalises null back
  to `-1` internally so downstream code only deals with one
  missing-value marker.
- **`SkillFileScaffolder` writeHashFile stack trace (0.1.3 only).**
  Verified: source code is unchanged between 0.1.3 and 0.1.4 and all
  four IOException sites already log-and-continue. The consumer's
  observation that 0.1.4 doesn't trip is environmental (filesystem
  state, permissions), not a code change.

### Changed

- **OpenRewrite bumped to `8.81.10`** (was 8.81.3) — rewrite-core /
  rewrite-java / rewrite-java-25 / rewrite-test.
- **`rewrite-static-analysis` bumped to `2.34.1`**, **`rewrite-logging-frameworks`
  bumped to `3.27.3`**, **`fifties-recipes` bumped to `0.9`** (the
  upcoming `clean-logging` artefact will replace it; see backlog).
- **Phase B mismatch pairs closed.** `BroadCatchRecipe`,
  `SwallowedExceptionRecipe`, `MagicStringRecipe`, `RemoveNestedTernaryRecipe`
  marked "Closed — not paired" in
  `docs/sessions/2026-05-04-recipe-research.md` with per-pair
  rationale. Detector-vs-fixer mismatches, classpath-attribution
  asymmetries, and silent-extraction UX-shift trade-offs make these
  bad candidates for upstream replacement.

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
