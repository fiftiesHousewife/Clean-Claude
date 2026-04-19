---
name: clean-code
description: Apply the Clean Code plugin to the current project. Runs the analysis, produces per-file fix briefs, and hands off to agents that fix findings one file at a time. Use when the user asks to "clean up", "reduce warnings", "apply Clean Code", or fix Robert Martin heuristic violations.
argument-hint: (no arguments — infers project layout)
---

# Clean Code

Apply the Clean Code plugin to the current project and drive fixes through per-file agents.

## Phase 1: Install (first run only)

If this machine has never published the plugin:

```bash
git -C "$(dirname "$(git rev-parse --show-toplevel)")/CleanClaude" rev-parse HEAD >/dev/null 2>&1 \
    && (cd "$(dirname "$(git rev-parse --show-toplevel)")/CleanClaude" && ./gradlew --quiet publishToMavenLocal)
mkdir -p ~/.gradle/init.d
cp "$(dirname "$(git rev-parse --show-toplevel)")/CleanClaude/scripts/cleancode.init.gradle.kts" ~/.gradle/init.d/ 2>/dev/null || true
```

Check ``~/.gradle/init.d/cleancode.init.gradle.kts`` exists. If you don't have the CleanClaude repo locally, tell the user and stop.

## Phase 2: Apply to the current project

1. Confirm the target project is a Gradle build (presence of `settings.gradle.kts` or `settings.gradle`).
2. Add the plugin to the target's top-level `build.gradle.kts`:
   ```kotlin
   plugins {
       id("io.github.fiftieshousewife.cleancode") version "1.0-SNAPSHOT"
   }
   ```
   If the project is multi-module, apply to every submodule that has Java sources. Using a convention plugin in `build-logic/` is preferable to copy-pasting.
3. Verify `pluginManagement.repositories` in `settings.gradle.kts` includes `mavenLocal()` and `gradlePluginPortal()`. If the init script is installed this is automatic; otherwise add them explicitly.

## Phase 3: Run the analysis

```bash
./gradlew analyseCleanCode cleanCodeFixPlan
```

Per-file briefs appear at `build/reports/clean-code/fix-briefs/` in each module. `_INDEX.md` lists them all.

## Phase 4: Drive fixes by delegating to agents

For each file with findings:

1. Read its brief at `build/reports/clean-code/fix-briefs/<ClassName>.md`.
2. Spawn a general-purpose Agent whose prompt IS the brief contents. One agent per file. Do not edit files yourself.
3. After the agent completes, run `./gradlew :<module>:test` for that module.
4. Commit. Message: `fix: <ClassName> (<codes>)`.

**Anti-goal:** never make code worse to satisfy a metric. If a fix would hurt readability, leave the finding and document it in the final summary.

## Phase 5: Report

When all briefs are addressed (or skipped with rationale), print:

- Count of findings fixed
- Count intentionally skipped, with reasons
- Test status per module
- Before / after finding counts from `./gradlew analyseCleanCode`

## For unattended / scheduled runs

Use `scripts/run-experiment.sh <manual|recipe> <run-number>` from the CleanClaude repo. The script wraps Phases 3–5 in a non-interactive `claude -p` invocation suitable for cron or the `schedule` skill.
