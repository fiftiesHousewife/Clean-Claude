---
name: experiment
description: Run a manual or recipe-assisted fix experiment run. Usage /experiment <manual|recipe> <run-number>
disable-model-invocation: true
argument-hint: <manual|recipe> <1|2|3>
allowed-tools: Bash(git *) Bash(rm *) Bash(cp *) Bash(mkdir *) Bash(export *) Bash(cat *) Bash(jq *) Bash(wc *) Bash(./gradlew *)
---

# Experiment: Manual vs Recipe-Assisted Fix

Parse `$ARGUMENTS` as `<approach> <run-number>` where approach is `manual` or `recipe` and run-number is 1, 2, or 3.

## Phase 1: Setup

Run these steps in order:

1. **Validate arguments** — abort with a clear message if approach is not `manual` or `recipe`, or run-number is not 1-3.

2. **Check for clean working tree** — run `git status --porcelain`. If there are uncommitted changes, abort and tell the user to commit or stash first.

3. **Record the baseline commit** on main before branching:
   ```bash
   BASELINE=$(git rev-parse HEAD)
   ```

4. **Create experiment branch**:
   ```bash
   git checkout -b experiment/<approach>-<n> $BASELINE
   ```

5. **Clear token logs**:
   ```bash
   rm -f .claude/tool-log.jsonl .claude/session-log.jsonl
   ```

6. **Confirm ready** — tell the user:
   > Branch `experiment/<approach>-<n>` created from `$BASELINE`.
   > Token logs cleared.
   >
   > **Next:** exit this session, then start a fresh one with:
   > ```bash
   > CLAUDE_TASK_LABEL="<approach>-fix-<n>" claude
   > ```
   > Paste the prompt below into that session.

7. **Print the prompt** the user should paste into the fresh session:

   - If `manual`:
     ```
     Fix Clean Code findings in this project. The plugin has already run — per-file
     briefs are in each module's build/reports/clean-code/fix-briefs/ directory.
     build/reports/clean-code/fix-briefs/_INDEX.md is the top-level list.

     Protocol:
     - For each file with findings, spawn a general-purpose Agent whose prompt IS
       the brief. One agent per file. Do not edit files yourself.
     - Do NOT use refactoring module recipes — each agent fixes its findings manually.
     - Run `./gradlew :<module>:test` after each batch of agent commits.
     - Commit after each file is done. Message: "fix: <ClassName> (<codes>)".
     - Stop only when `./gradlew analyseCleanCode` reports zero non-suppressed findings,
       OR when every remaining finding is documented in a final summary as "intentionally
       skipped because <reason>".

     Anti-goal:
     - Never make the code worse to satisfy a metric. If squeezing under a line-count
       or blank-line threshold would hurt readability, leave the finding and document
       it. The goal is clearer code, not a green report.
     ```

   - If `recipe`:
     ```
     Fix Clean Code findings in this project using the refactoring recipes first.

     Step 1: Apply refactoring recipes from the refactoring module to fix mechanical
     findings (G22, G24, G10, G12/J1, G25, T1, G19/G28, G33, G16, G26).
     Commit after each recipe.

     Step 2: For each file that still has findings after Step 1, spawn a general-purpose
     Agent whose prompt IS the per-file brief at
     build/reports/clean-code/fix-briefs/<ClassName>.md. One agent per file.

     Step 3: Run the analysis again and report the remaining count.

     Protocol:
     - Run `./gradlew :<module>:test` after each batch.
     - Commit after each recipe and after each file fix. Message includes the codes
       addressed.
     - Stop only when analyseCleanCode reports zero non-suppressed findings, OR when
       every remaining finding is documented as intentionally skipped.

     Anti-goal:
     - Never make the code worse to satisfy a metric. If squeezing under a threshold
       would hurt readability, leave the finding and document it.
     ```

**Stop here.** Do not proceed to Phase 2 until the user invokes `/experiment-save`.