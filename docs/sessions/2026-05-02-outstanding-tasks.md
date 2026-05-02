# Outstanding tasks — handoff 2026-05-02

Context for whoever picks this up next. Self-contained: you should not need to read prior session transcripts.

## State at start of next session

- Branch: `main`, working tree clean.
- Last commit: `a413a5e` — README + handoff doc updates for `cleanCodeServe`. Plus uncommitted work below.
- Version `0.1.2` published to **mavenLocal across all modules**; **not yet** tagged in git or published to Maven Central.
- `./gradlew check` green end-to-end.
- Self-apply harness lives at `/tmp/cleanclaude-selfanalysis-1777721438/` (per `feedback_publish_all_modules.md` memory). Run `./gradlew publishToMavenLocal` first; harness is excluded from sweeps.
- Triage UI (`cleanCodeServe`) is functional end-to-end — verified via the harness (build-script edit + suppress-finding edit landed and re-analysis fired).
- **NEW: P0 #3 (cleanCodeStop + in-page Stop button) is complete and smoke-tested via the harness — see "Done since last handoff" below. Not yet committed.**

## Done since last handoff

- `cleanCodeServe` writes `build/clean-code/serve.pid` before binding the port and deletes it on shutdown (Ctrl-C, `/api/shutdown`, or `cleanCodeStop`).
- `ReportServer` exposes `POST /api/shutdown` which acks 200 and triggers the shutdown callback after a 150 ms delay (so the response gets out before the server stops).
- New `cleanCodeStop` task reads the PID file, sends SIGTERM, waits 5 s, escalates to SIGKILL. Idempotent on a stale or missing PID file.
- HTML staging bar gains a 🛑 **Stop server** button (next to Confirm/Discard); on click it confirms, POSTs `/api/shutdown`, and replaces the bar with "Server stopped — re-run `./gradlew cleanCodeServe`".
- Tests: `HtmlReportWriterTest#emitsStopServerButtonThatPostsToShutdownEndpoint`, `ReportServerTest#shutdownEndpointInvokesShutdownCallback` (+ non-POST rejection), `StopTaskTest` covering `readPid` parsing edge cases and `waitForExit` exit-vs-timeout behaviour using `sh -c sleep`.

## Quick reference

```bash
# Triage UI (manual, blocking)
./gradlew cleanCodeServe                     # opens http://localhost:7070

# Stop the running serve task
lsof -nP -iTCP:7070 -sTCP:LISTEN | awk 'NR>1 {print $2}' | xargs kill

# Republish locally after changes
./gradlew publishToMavenLocal

# Re-run the self-apply harness against latest mavenLocal
cd /tmp/cleanclaude-selfanalysis-1777721438 && ./gradlew analyseCleanCode
```

---

## Tasks, ordered by priority

### P0 — release/CI gaps

#### 1. Tag and push 0.1.2 to Maven Central
**Why:** Heuristic fixes (`170834b`), opt-in rules + Sources panel (`1c24d33`), plumbing fix + LegacyTypesRecipe (`924fa9a`), flood-triage (`eab395e`), the entire `cleanCodeServe` UX (`39ab020`/`24b0457`/`0dcf4fb`/`b64b814`), and the docs (`a413a5e`) are all in `main` but only available via mavenLocal. External consumers cannot get any of this.

**Steps:**
1. Confirm `./gradlew check` green on a clean clone.
2. `git tag -a v0.1.2 -m "0.1.2"` and `git push --follow-tags origin main`.
3. `./gradlew publishToMavenCentral` (vanniktech publish; auto-release was set to `false` in the publish config — staging repo needs manual close on the Sonatype Central Portal).
4. Verify staging repo, then close + release.
5. After the artifact propagates (~15 min), bump the README plugin coordinate examples from any old version to `0.1.2` and remove `mavenLocal()` from the "Apply to another project" snippet.

**Gotchas:**
- The publish config disables `automaticRelease`. You must log into the Sonatype Portal and manually close + release.
- `pom.withXml` strips `<dependencies>` so the published POM has none — the shadow jar bundles them all. Don't accidentally re-enable dep generation when touching `plugin/build.gradle.kts`.
- Per `feedback_publish_all_modules.md`: always run **root** `publishToMavenLocal`, never per-module — partial publishes have silently broken consumers before.

---

#### 2. TestKit self-apply harness in CI
**Why:** Today the plugin's correctness against itself is only validated manually via the `/tmp/cleanclaude-selfanalysis-*` script. CI doesn't run `analyseCleanCode` or `cleanCodeServe` against the plugin's own source — regressions in the staging UI markup or the report rendering land silently until manual smoke-test.

**Approach:**
1. New `:plugin:integrationTest` source set, JUnit 5 + Gradle TestKit.
2. Build a fixture that mounts the plugin's own `*/src/main/java` directories via `srcDirs(...)` (mirroring the `/tmp/cleanclaude-selfanalysis-1777721438/build.gradle.kts` setup).
3. Two test scenarios:
   - `analyseCleanCodeProducesReport`: run `analyseCleanCode`, parse `findings.json`, assert non-zero findings, assert all expected source IDs appear in the Sources panel (openrewrite + checkstyle + pmd at minimum).
   - `cleanCodeServeAcceptsBatch`: launch `cleanCodeServe` task in background, `curl -X POST /api/apply-changes` with a single `disableRecipe`, assert build.gradle.kts is mutated and re-analysis fires; shut down via shutdown endpoint (see task #3).
4. Wire into `.github/workflows/ci.yml`.

**Gotchas:**
- `TestKit + Java toolchain` quirks: pin Gradle and JDK versions or expect Heisenfailures.
- Background-server tests are slow — keep them in a dedicated source set so unit tests stay fast.
- Tests should not call `publishToMavenLocal`. Use `withPluginClasspath()` to run the plugin under test directly.

---

#### 3. `cleanCodeStop` task + in-page Stop button — **DONE 2026-05-02 (uncommitted)**
Implemented as described. Smoke-tested via the harness: `cleanCodeServe` writes the PID, `POST /api/shutdown` cleanly tears down (port closes, PID deleted, BUILD SUCCESSFUL), `cleanCodeStop` from a second daemon SIGTERMs the serve daemon successfully. Note: the PID written is the Gradle daemon's PID, not the serve thread's — killing it terminates the daemon, but that is fine because the daemon is fully occupied by the serve task and a fresh daemon spins up on the next gradle invocation.

---

### P1 — Triage UX completion

#### 4. Per-finding 🔧 Fix button + per-section Fix all N
**Why:** The current triage UI lets users **suppress** noise but not **fix** it. For codes that have a deterministic refactoring recipe, the user should be one click away from the actual fix.

**Approach:**
1. Build `core/.../RefactoringRegistry.java` mapping `HeuristicCode → List<String>` of recipe class names. Source recipe→code links from class javadocs and existing recipe wiring:
   - G29 → `CollapseSiblingGuardsRecipe`, `InvertNegativeConditionalRecipe`
   - G31 → `ChainConsecutiveBuilderCallsRecipe`
   - G34 → `DeleteSectionCommentsRecipe`
   - G18 → `MakeMethodStaticRecipe`
   - F2 → `ReturnInsteadOfMutateArgRecipe`
   - G4 → `UseTryWithResourcesRecipe`
   - G22 → `AddFinalRecipe` (only useful when the user has opted into FinalLocalVariable)
   - G12 → `ShortenFullyQualifiedReferencesRecipe`, `DeleteUnusedImportRecipe`
   - G25/G35 → `ExtractConstantRecipe`/`ExtractClassConstantRecipe`
   - G28 → `RemoveNestedTernaryRecipe`
   - N1 → `RenameShortNameRecipe`
   - F3 → `SplitFlagArgumentRecipe`
   - G14 → `EncapsulateBoundaryRecipe`, `MergeInlineValidationRecipe`
   - G16 → `ReduceVisibilityRecipe`
   - G19 → `ExtractExplanatoryVariableRecipe`
   - Ch7_1 → `DeleteMumblingLogRecipe`
   - G26 (locale opt-in) → `AddLocaleRecipe`
2. Verify each mapping by reading the recipe's class javadoc; commit the mapping with the source-of-truth comments next to each entry.
3. New `applyRefactoring` change kind in `PendingChange`. Params: `{code, file?, line?}`. With file → run recipes against just that file via `HarnessRecipePass.applyToFile(...)`. Without file → run against all files (current `analyseWithStates` source set).
4. `HtmlReportWriter`:
   - Per-finding 🔧 **Fix** button in the actions cell, only emitted when `RefactoringRegistry.hasRecipeFor(code)`. Carries `data-code`/`data-file`/`data-line`.
   - Per-section 🔧 **Fix all N** button next to ❌ Disable / ⚙️ Tune in `appendCodeActions`.
5. Modal flow same as Suppress; reason becomes part of the commit message comment if we ever add `git commit` integration (not in scope yet — just stage it for now).
6. `ChangeApplier` handles `applyRefactoring` by calling a new `RefactoringApplier` that delegates to `HarnessRecipePass.applyToFile` for single-file or `HarnessRecipePass.apply(allFiles)` for bulk.
7. After apply, the existing re-run-and-reload path already shows the post-fix state.

**Files:**
- `core/src/main/java/.../RefactoringRegistry.java` (new)
- `plugin/src/main/java/.../serve/RefactoringApplier.java` (new)
- `plugin/src/main/java/.../serve/ChangeApplier.java` — new switch case
- `core/src/main/java/.../HtmlReportWriter.java` — new buttons in `appendCodeActions` and `buildSuppressButton` siblings
- Tests: `RefactoringRegistryTest`, `RefactoringApplierTest`, plus `HtmlReportWriterTest` cases for the new buttons

**Gotchas:**
- `HarnessRecipePass` is in the `plugin/.../rework/` package; scope-check that it's accessible. If not, hoist the apply-to-file helper to a shared support package.
- Recipes mutate disk. The applier must run **after** any `BuildScriptEditor` / `SourceFileEditor` edits in the same batch, or order them so suppression annotations don't get rewritten by a recipe.
- Recipes can fail silently. The applier should diff the file before/after and report `0 changes` as a soft error in the staging-bar response.

---

#### 5. 💬 Request-a-change-to-the-plugin button
**Why:** While triaging, users frequently spot finding-level false positives, missing recipes, or doc gaps. Today there's no in-app channel to capture that thought before they lose it.

**Approach:**
1. Top-right of the report header, next to the IDE picker: **💬 Request a change** button.
2. Click → modal with one textarea ("What would make this report more useful?") + optional context fields auto-filled when invoked from a finding row (code, file, line, message).
3. POST `/api/feedback` with `{ message, context }`.
4. Server appends to `${projectRoot}/clean-code-feedback.md` with timestamp + branch + version + message + context. Creates the file if missing.
5. After successful submit, modal shows "Saved to clean-code-feedback.md — review before commit". No GitHub integration yet (keeps it offline / private).

**Files:**
- `plugin/src/main/java/.../serve/FeedbackHandler.java` (new) — appends Markdown.
- `plugin/src/main/java/.../serve/ReportServer.java` — new `/api/feedback` context.
- `core/src/main/java/.../HtmlReportWriter.java` — header button, modal, JS handler.

**Gotchas:**
- Don't auto-commit the feedback file; let the user review.
- Don't include user-supplied content in any redirect/URL — pure server-side append only, escape for Markdown.
- File grows monotonically; no rotation needed but flag in docs.

---

### P2 — heuristic quality

#### 6. G12: skip static-final field accesses (#6 in pending)
**Why:** `LoggerNames.LOG4J2_LOGGER` and similar named constants get flagged as fully-qualified type references. The `.class` literal fix landed in `eab395e`, but the constant-access path is still over-eager.

**Approach:** In `recipes/.../FullyQualifiedReferenceRecipe.java#isFullyQualifiedTypeReference`, also return `false` when the field-access target's resolved type is `static final`. JavaType.Variable carries the modifier flags.

**Acceptance:** new test `doesNotFlagStaticFinalConstantAccess` covering `LoggerNames.LOG4J2_LOGGER`-style code.

---

#### 7. CPD G5 visitor-scaffolding suppress (#8 in pending)
**Why:** OpenRewrite recipe-implementation visitors are structurally similar (mandatory `visitX` method scaffolding). CPD reports them as duplication. False positives in `recipes/` package.

**Approach:** package-level `@SuppressCleanCode({HeuristicCode.G5})` on `recipes/package-info.java` (already exists for some recipes); verify it covers all sub-packages. Alternatively expose a per-package CPD min-token override on the extension.

**Acceptance:** running `analyseCleanCode` against the plugin's own `recipes/` produces no G5 findings from intra-package visitor copies.

---

#### 8. N6 real-word detection for variable names (#14 in pending)
**Why:** Today the recipe flags variables shorter than the configured min-length but doesn't catch e.g. `tmpHldr`, `prsr`, `reslt` which are full-length but unreadable.

**Approach:**
1. Bundle a wordlist (SCOWL or similar; ~50k common words ≈ 500 KB).
2. Tokenize identifier (camelCase split, snake_case split).
3. Flag if more than half the tokens aren't in the wordlist or aren't single letters / common abbreviations (`id`, `url`, `csv`, etc.).
4. New recipe `RealWordVariableRecipe` mapped to N6.

**Gotchas:**
- Domain-specific terms (`Slf4j`, `OpenRewrite`, `Anthropic`) need an allowlist or context heuristic.
- Test framework names (`junit`, `mockito`) often appear in identifiers — allowlist.

---

### P3 — quality / re-baseline

#### 9. Re-baseline after triage
**Why:** After `eab395e` the OpenRewrite finding count dropped from 1161 to ~? on the plugin's own source. Re-run, capture the new baseline, and decide which categories still warrant follow-up work.

**Approach:**
1. `cd /tmp/cleanclaude-selfanalysis-1777721438 && ./gradlew analyseCleanCode`.
2. `python3 -c "..."` style breakdown by code (script in this session's transcript).
3. If any single code is still > 50 findings, treat as a triage candidate: either tighten the recipe or raise the threshold.
4. Update the README's "Configuration" example with the new sensible defaults if anything changes.

---

## Tooling notes carried forward (memory)

- Always publish from root: `./gradlew publishToMavenLocal` (never per-module).
- Sandbox excluded from whole-codebase sweeps — see `feedback_publish_all_modules.md`.
- Recipe sweep workflow: `./gradlew :plugin:runHarnessPass -Pdir=<root>`; publish mavenLocal first; sandbox excluded; don't commit output.
- Do not commit auto-generated `findings.json` / `findings.html`.
- Be opinionated with rankings; ask only on high-blast-radius choices.

## Architecture cheat-sheet for the triage UI

```
ServeTask (long-running, blocks until Ctrl-C)
  ├─ runs SandboxAnalysis.analyseWithStates
  ├─ writes findings.html via HtmlReportWriter
  └─ ReportServer (localhost:7070, com.sun.net.httpserver.HttpServer)
        ├─ GET  /                  serves findings.html fresh from disk
        ├─ GET  /api/state         current ConfigSnapshot (Gson)
        └─ POST /api/apply-changes ApplyChangesRequest → ChangeApplier
                                    ├─ BuildScriptEditor (line-based on build.gradle.kts)
                                    ├─ SourceFileEditor  (JavaParser, JAVA_21 level)
                                    └─ [future] RefactoringApplier (HarnessRecipePass)
                                   then re-run analyseWithStates + rewrite findings.html
                                   client soft-reloads
```

The JS in `appendStagingScript` accumulates clicks in `localStorage` under `cleanCodePendingChanges`; **Confirm** sends them as one batch. Server-down detection happens at page load by `fetch('/api/state')` failing — buttons disable with a tooltip.
