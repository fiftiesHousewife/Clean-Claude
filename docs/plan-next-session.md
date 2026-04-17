# Plan: next session handoff

State at hand-off, outstanding work, and how to pick it up from a clean Claude Code session.

## What's done

- **Pilot run complete.** Branch `experiment/manual-pilot` on `origin`, 101 commits, 152 files, 1,395 → 341 findings. Run report at `experiment/manual-pilot-analysis.md` on that branch.
- **Plans on `main`:**
  - `docs/experiment-analysis-plan.md` — reusable template for analysing any run
  - `docs/plan-review-and-severity.md` — Claude Review coverage expansion and severity rebalance
  - `docs/plan-next-session.md` — this document
- **Tooling on `main`:**
  - `scripts/run-experiment.sh` — one-command runner, auto-resumes, filters logs by task label, emits an analysis stub
  - `scripts/cleancode-dogfood.init.gradle.kts` — self-applies the plugin to the project's modules for analysis
  - 11 Claude Code skills under `.claude/skills/clean-code-*/SKILL.md`
  - Per-package `@SuppressCleanCode` on `recipes/` and `refactoring/`
  - `FixBriefGenerator` → per-class briefs at `build/reports/clean-code/fix-briefs/`

## Pilot findings that drive the remaining work

1. The 93 E1 dep-update findings never got fixed. Briefs were generated at `_project-level.md` and skipped by the driver because of the `_` prefix.
2. Agents skipped reading the skill file their brief pointed at — brief wording was a hint, not an instruction.
3. Line-length violations sneaked into the agent output (>120 chars). Checkstyle had it at one level (WARNING); no ERROR escalation and no auto-fix.
4. G19 detection keeps flagging expressions even after they're extracted to a named variable.
5. Session 2 was 2× faster than session 1 because the Gradle daemon was warm, not because of Anthropic prompt-cache cross-session inheritance (10 h gap exceeded the 5 min / 1 h TTL).

## Outstanding work

Every item below has a task in the harness task list. Ordered by priority.

### A. High-leverage, ships on `main` before the next run

**A1. Rename `_project-level.md` brief** (task #2)
- Edit `core/.../FixBriefGenerator.java`: constant `"_project-level.md"` → `"project-level-findings.md"`.
- Update `FixBriefGeneratorTest` expectations.
- Update the experiment prompts in `scripts/experiment-*-prompt.txt` to explicitly instruct: *"Every file under `fix-briefs/` other than `_INDEX.md` is an actionable brief. Do not skip any."*

**A2. G19 over-detection bug** (task #3)
- Read `recipes/.../MissingExplanatoryVariableRecipe.java`.
- Hypothesis: the scanner walks the original expression site without checking whether the surrounding statement is now a variable declaration assigned from that expression. Add a check that returns early if the enclosing node is a `VariableDeclarationFragment` whose initialiser IS the flagged expression.
- Add a fixture test with an already-extracted case; confirm no finding.

**A3. Two-level severity for line length** (tasks #5, #6)
- Checkstyle `LineLength`: emit WARNING at >120 (current), ERROR at >150. Needs a severity map in our Checkstyle adapter since Checkstyle itself only has one severity per rule.
- Consider enabling Spotless or google-java-format so agents cannot introduce >120 lines in the first place.
- Also update OpenRewrite severity map per `docs/plan-review-and-severity.md`.

**A4. Agent preloads skill before editing** (task #7)
- Edit `FixBriefGenerator.renderBrief` to replace `"> Read X before addressing these."` with `"> **You MUST Read this file first — before any Edit or Write tool call:** X"`.
- Stronger still: add an opening instruction to the generated brief: *"Your first N tool calls must be Reads of the skill paths cited below. Do not call Edit or Write before every skill has been read in full."*

**A5. Fully encapsulate SpotBugs** (task #15)
- Goal: `plugins { id("org.fiftieshousewife.cleancode") version "1.0-SNAPSHOT" }` should work from Maven Local with no extra repo setup in the consumer's `settings.gradle.kts`.
- Approach: add the Gradle Shadow plugin to `:plugin`, shade `com.github.spotbugs:spotbugs-gradle-plugin` + its runtime deps into the plugin jar, apply programmatically with `project.getPluginManager().apply(SpotBugsPlugin.class)` (already works at runtime if the classes are on our classloader).
- Preserve `META-INF/gradle-plugins/com.github.spotbugs.properties` so the plugin ID resolves.
- Remove the "Apply to another project" section from `README.md` once it works; delete `scripts/cleancode-init.gradle.kts` (the one consumers were copying — not to be confused with `scripts/cleancode-dogfood.init.gradle.kts` which stays for our own analysis).
- Acceptance test: create a fresh throwaway Gradle project with only `mavenLocal()` in `pluginManagement`, apply our plugin, run `./gradlew tasks` — `spotbugsMain` must appear.

### B. Planning + investigation (commit decisions to `main`)

**B1. Agent suppression policy** (task #9) — decision required. Recommendation: agents may add `@SuppressCleanCode` only when the skill file explicitly lists the code as "suppressible with reason"; otherwise they must document in the final summary as "intentionally skipped".

**B2. Second-pass step in runner** (task #8) — after the main loop, re-run `analyseCleanCode + cleanCodeFixPlan`. If new findings appeared (regression from first-round fixes), run one more pass limited to those. Cap at two passes.

**B3. Import-ordering detection** (task #10) — likely enable Checkstyle `ImportOrder` rather than build a new recipe. One-line config change.

**B4. HEURISTICS.md formal examples** (task #11) — each heuristic gets a violation block + corrected block + false-positive example, matching the template in skill files.

**B5. Skill body audit** (task #12) — several skill files omit codes that `SkillPathRegistry` routes to them. Walk each skill file, check its declared code list against the registry, fill gaps with worked examples.

**B6. Ben-Manes upgrade capability** (task #13) — Ben-Manes is detection only; it does not modify files. Options: (a) write a `UpdateVersionCatalogRecipe` that reads the Ben-Manes JSON and rewrites `libs.versions.toml`; (b) adopt `refreshVersions` plugin which DOES auto-update; (c) keep the agent-driven path via `clean-code-dependency-updates` skill. Pick one; (b) is likely the cleanest.

### C. Goes on `experiment/manual-pilot`

**C1. Pippa reclassification list** (task #17) — add a "Codes Pippa should re-review" section to `experiment/manual-pilot-analysis.md`. List the codes where the agent's output quality differed markedly from the severity currently assigned — candidates for reclassification.

### D. Scheduling

**D1. Schedule experiments at 01:10** (task #14)
- Clean `manual-1` run: `scripts/run-experiment.sh manual 1`
- `recipe-1` run: `scripts/run-experiment.sh recipe 1`
- Use the `schedule` skill (`/schedule create --cron "10 1 * * *" --prompt "run scripts/run-experiment.sh manual 1"`), or `cron` directly. Decide before scheduling whether to run both on the same night or on consecutive nights.
- Pre-bump the 93 E1 deps manually before scheduling — saves noise in the comparison. `./gradlew dependencyUpdates`, update `libs.versions.toml`, one commit.

## Branch discipline

- **`main`** — all code fixes, all plans. Push freely.
- **`experiment/manual-pilot`** — pilot run record. Only analysis updates and logs.
- When fixes land on `main`, a future experiment branch will naturally pick them up because `run-experiment.sh` creates its branch from `main`.
- Don't merge the pilot branch into `main`.

## How to resume in a fresh session

Paste this into a new Claude Code session:

> Read `docs/plan-next-session.md` in this repo. Start with the items under "High-leverage, ships on `main` before the next run" (A1 through A5), using a `git worktree` on `main` so the `experiment/manual-pilot` branch in the main working tree is not disturbed. After each item: commit with a clear message, push to `origin/main`, mark the corresponding task complete, and tell me before moving on to the next.

That prompt gives the next agent everything it needs.
