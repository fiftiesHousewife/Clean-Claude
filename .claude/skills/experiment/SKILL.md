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
     Fix all Clean Code findings in this project. The plugin has already been run —
     findings are in each module's build/reports/clean-code/findings.json.

     Rules:
     - Fix findings in priority order: errors first, then warnings
     - Do NOT use the refactoring module recipes — fix each finding manually
     - Run tests after each batch of fixes to verify nothing breaks
     - Commit after each heuristic code is fully addressed
     - When done, run the analysis again and report the remaining count
     ```

   - If `recipe`:
     ```
     Fix all Clean Code findings in this project using the refactoring recipes first.

     Step 1: Apply refactoring recipes from the refactoring module to fix mechanical
     findings (G22, G24, G10, G12/J1, G25, T1, G19/G28, G33, G16, G26).
     Run each recipe via OpenRewrite, verify tests pass.

     Step 2: Fix remaining findings manually, in priority order (errors first).

     Step 3: Run the analysis again and report the remaining count.

     Rules:
     - Commit after each recipe application
     - Commit after each manual heuristic code batch
     - Run tests between commits
     ```

**Stop here.** Do not proceed to Phase 2 until the user invokes `/experiment-save`.