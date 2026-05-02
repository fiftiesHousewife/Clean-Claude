# Triage UX expansion — plan

Adds four UX features to the `cleanCodeServe` triage report. Each is independently shippable; ordered to land most-foundational first.

## Scope (in order of implementation)

### 1. Confidence rating display + filter

**Why:** Findings carry a `Confidence` (HIGH/MEDIUM/LOW) but the UI never surfaces it. With ~1000 findings on a real codebase, users want to triage HIGH-confidence items first and skim/skip LOW.

**Render:**
- New `Confidence` column in every per-code table (between Severity and Location).
- Pill style: `HIGH` filled green, `MEDIUM` outlined orange, `LOW` outlined grey.
- Per-code summary line gets a `H/M/L` breakdown (e.g. `(42) — H:30 M:8 L:4`).

**Filter:**
- Top-of-page filter bar (next to IDE picker): three checkboxes "High" "Medium" "Low" (all checked by default).
- Toggling hides/shows `<tr data-confidence="...">` rows; whole `<details>` collapses if empty.
- Filter state persists in `localStorage` under `cleanCodeConfidenceFilter`.

**Files:**
- `core/.../HtmlReportWriter.java` — add `data-confidence` attribute on each `<tr>`, new column header, pill cell, summary breakdown, top-bar filter, JS handler.
- `core/.../HtmlReportWriterTest.java` — new test asserting pill markup + filter checkbox + per-code H/M/L summary.

---

### 2. Code snippet preview (collapsible)

**Why:** Users have to click into IDE to see the offending code. A 5-line snippet inline shaves seconds per finding × 1000 findings = hours saved.

**Render:**
- Each finding row gets an expander `▸ snippet` after the message.
- Click → row expands a `<tr class="snippet-row">` underneath with `<pre>` of lines `[startLine-2 … endLine+2]` from the source file.
- Snippet is loaded **at report-write time** by `HtmlReportWriter` reading the file from disk; no runtime fetch needed (keeps the report a single self-contained file).
- Cap at 12 lines total to avoid bloating the HTML; if the range exceeds, truncate with `…` markers and bold the offending line(s).
- Escape HTML entities; preserve indentation.

**Files:**
- New `core/.../SnippetReader.java` — given `Finding + projectRoot`, returns a small record `{ String snippet, int firstLineNumber, int focalLine }`. Quietly returns null on read failure (e.g. project-level findings, deleted files).
- `core/.../HtmlReportWriter.java` — wire snippet markup, add `<details class="snippet">` with `<summary>` toggle and `<pre>` body; CSS for line-number gutter and focal-line highlight.
- `core/.../SnippetReaderTest.java` — covers happy path, missing file, line out of range.
- `HtmlReportWriterTest` — assert snippet markup is emitted when a real `projectRoot` is passed.

**Gotchas:**
- Adds ~100 bytes per finding to the report. 1000 findings ≈ 100 KB extra — acceptable.
- For findings without `sourceFile` or `startLine <= 0` (project-level), skip snippet entirely.

---

### 3. 🔧 Fix button (per-finding, per-section, multi-select)

**Why:** Today users can `suppress` and `disable` but not actually fix. For codes with a deterministic recipe, one click should run the recipe.

**Architecture:**

```
HtmlReportWriter
  ├─ buildFixButton(finding, code)        per-finding 🔧
  └─ appendCodeActions adds 🔧 Fix all N  per-section
ApplyChangesRequest accepts kind="applyRefactoring"
  ├─ params: { code, file? }              file present → single, absent → bulk
ChangeApplier
  └─ new case "applyRefactoring" → RefactoringApplier
RefactoringApplier (new, in plugin/serve/)
  ├─ Looks up recipes via RefactoringRegistry.recipesFor(code)
  ├─ file present → HarnessRecipePass.applyToFile(file, recipes)
  └─ file absent → walks all *.java under projectRoot, applies in batch
RefactoringRegistry (new, in core/ — needs to be visible from both core and plugin/serve)
  └─ HeuristicCode → List<Recipe supplier>
```

**Per-finding 🔧 button:**
- Emitted only when `RefactoringRegistry.hasRecipeFor(code)`.
- Carries `data-code`, `data-file`, `data-line` (line is informational; recipes apply to whole file).
- Modal flow same as Suppress: prompt for reason. Adds a `applyRefactoring` change to the staging batch.

**Per-section 🔧 Fix all N button:**
- Emitted next to ❌ Disable / ⚙️ Tune.
- Triggers `applyRefactoring` with no `file` param → bulk across all matching files.

**Multi-select / batching:**
- Multiple per-finding 🔧 clicks accumulate in the staging bar like everything else.
- ChangeApplier must order: BuildScript edits → SourceFile suppressions → applyRefactoring (so recipes don't rewrite the suppression annotations a user just inserted).
- Within `applyRefactoring`, group by file: per-file fixes run once per file even if multiple findings click 🔧 (the recipe runs the full file anyway).

**RefactoringRegistry mapping** (verify each by reading the recipe's class javadoc; commit with a comment-citation next to each entry):
| Code | Recipe(s) |
|------|-----------|
| G29 | `CollapseSiblingGuardsRecipe`, `InvertNegativeConditionalRecipe` |
| G31 | `ChainConsecutiveBuilderCallsRecipe` |
| G34 | `DeleteSectionCommentsRecipe` |
| G18 | `MakeMethodStaticRecipe` |
| F2 | `ReturnInsteadOfMutateArgRecipe` |
| G4 | `UseTryWithResourcesRecipe` |
| G12 | `ShortenFullyQualifiedReferencesRecipe`, `DeleteUnusedImportRecipe` |
| G25 / G35 | `ExtractConstantRecipe` / `ExtractClassConstantRecipe` |
| G28 | `RemoveNestedTernaryRecipe` |
| N1 | `RenameShortNameRecipe` |
| F3 | `SplitFlagArgumentRecipe` |
| G14 | `EncapsulateBoundaryRecipe`, `MergeInlineValidationRecipe` |
| G16 | `ReduceVisibilityRecipe` |
| G19 | `ExtractExplanatoryVariableRecipe` |
| Ch7_1 | `DeleteMumblingLogRecipe` |
| G26 | `AddLocaleRecipe` (locale opt-in only) |

**Files:**
- New `core/.../RefactoringRegistry.java` — maps codes to recipe class names (using string identifiers since `core` doesn't depend on `refactoring`).
- New `plugin/.../serve/RefactoringApplier.java` — instantiates and runs recipes, collects diff.
- `plugin/.../serve/ChangeApplier.java` — new `applyRefactoring` case + reordered apply pipeline.
- `plugin/.../serve/PendingChange.java` — new kind constant; param schema docs.
- `core/.../HtmlReportWriter.java` — `buildFixButton`, augment `appendCodeActions`, JS handler emits `applyRefactoring` change kind.
- Tests: `RefactoringRegistryTest`, `RefactoringApplierTest`, `ChangeApplierTest` extension, `HtmlReportWriterTest` cases.

**Gotchas:**
- `core` has no dependency on `refactoring` — RefactoringRegistry stores `String` class names; `RefactoringApplier` (in `plugin`, has `refactoring` on classpath) uses `Class.forName(...)` + `getConstructor().newInstance()` to instantiate.
- Recipes that need a `Set<String> superCalledNames` (only `MakeMethodStaticRecipe`) need RefactoringApplier to handle that argument specially.
- Recipes can fail silently. Applier must diff before/after — if no change, surface "no fix applied — recipe didn't match anything" as a soft error in the staging bar.
- Order in ChangeApplier: build-script edits → suppress (descending line) → applyRefactoring. Keeps the current suppress-then-recipe ordering correct.

---

### 4. 💬 Raise a bug button

**Why:** Users spot recipe false-positives and doc gaps while triaging. Today they have nowhere to capture it.

**Render:**
- Header bar gains 💬 **Request a change** button (next to IDE picker).
- Click → modal with one textarea ("What would make this report more useful?") + auto-filled context if invoked from a finding row (code, file, line, message).
- Each finding row also gets a small 💬 icon next to 🔧/Suppress.

**Server:**
- `POST /api/feedback` with body `{ message, context? }`.
- `FeedbackHandler` appends to `${projectRoot}/clean-code-feedback.md`. Format:
  ```markdown
  ## YYYY-MM-DD HH:MM:SSZ — branch <X> — version <Y>
  ### Context
  - code: G19
  - file: …
  - line: …
  - finding: …
  ### Message
  > whatever the user typed
  ```
- Creates the file if missing. No GitHub integration yet; just an offline review-and-commit log.
- Modal shows "Saved to clean-code-feedback.md — review before commit." after success.

**Files:**
- New `plugin/.../serve/FeedbackHandler.java`.
- `plugin/.../serve/ReportServer.java` — new context.
- `core/.../HtmlReportWriter.java` — header button, per-row icon, modal, JS handler.
- Tests: `FeedbackHandlerTest`, `ReportServerTest` adds a feedback case.

**Gotchas:**
- Don't auto-commit. Sanitise input as Markdown (escape pipes, code blocks if input contains backticks).
- Don't echo input back into a redirect or HTML — pure server-side append only.
- File grows monotonically; no rotation (note in README).

---

## Build/test budget

Each scope ships with green `./gradlew check` (unit + integration) and a self-apply harness pass. Commit per scope.

Approximate effort:
- 1: ~2 hrs (all CSS/JS, light Java)
- 2: ~2 hrs (new SnippetReader + HTML wiring)
- 3: ~5 hrs (registry + applier + ordering + tests)
- 4: ~2 hrs (handler + UI + tests)

## Out of scope

- Line-targeted fixes (recipes apply per-file).
- Auto-commit for feedback.
- Auto-creating GitHub issues from feedback.
- Per-recipe parameterisation in the UI.

## Status

- [ ] 1. Confidence rating + filter
- [ ] 2. Code snippet preview
- [ ] 3. 🔧 Fix button
- [ ] 4. 💬 Raise-a-bug button
