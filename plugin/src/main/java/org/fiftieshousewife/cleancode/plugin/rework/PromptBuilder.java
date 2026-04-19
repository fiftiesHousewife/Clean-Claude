package org.fiftieshousewife.cleancode.plugin.rework;

import java.util.List;

/**
 * Builds the string written to {@code claude -p}'s stdin. Three prompt
 * variants (see {@link RunVariant}) let {@link ReworkCompareTask}
 * isolate the value of the refactoring recipe from the value of the
 * compact Gradle-wrapping tools. Handles batched multi-file sessions:
 * the agent gets every {@link FileTarget} in one prompt, reads each
 * with its Read tool, and emits a single JSON summary covering every
 * action across every file.
 */
public final class PromptBuilder {

    private PromptBuilder() {}

    public static String build(final List<FileTarget> targets, final RunVariant variant) {
        return """
                You are reworking %d Java file%s to address the findings listed below.

                %s

                %s

                Before exiting, emit EXACTLY ONE JSON object as the last thing you write. The
                schema is:

                {
                  "actions":  [{"file": "<relative-path>", "recipe": "<RecipeClass-or-Edit>",
                                "options": {...}, "why": "<one sentence>"}],
                  "rejected": [{"file": "<relative-path>", "recipe": "<RecipeClass-or-Edit>",
                                "options": {...}, "why": "<one sentence>"}]
                }

                Findings:
                %s
                """.formatted(
                        targets.size(),
                        targets.size() == 1 ? "" : "s",
                        renderTargets(targets),
                        toolsBlock(variant),
                        renderFindings(targets));
    }

    private static String renderTargets(final List<FileTarget> targets) {
        final StringBuilder block = new StringBuilder("Target files (relative to project root):\n");
        targets.forEach(target -> block.append("  - ").append(target.relativePath()).append('\n'));
        block.append("""
                Read each file with your Read tool before planning the fix. Do NOT embed the
                files' contents in your response — the commit diff captures the actual change.

                Stay strictly within the target files listed above. Do NOT Read, Edit, Write,
                or Glob anywhere under `refactoring/`, `mcp/`, `plugin/`, `build-logic/`, or
                any other module — those are not part of the fix. If a tool misbehaves, treat
                that as a signal to switch strategies, not to investigate.""");
        return block.toString();
    }

    private static String renderFindings(final List<FileTarget> targets) {
        final StringBuilder block = new StringBuilder();
        targets.forEach(target -> {
            block.append("### ").append(target.relativePath()).append('\n');
            if (target.suggestions().isEmpty()) {
                block.append("  (no findings on this file — leave it unchanged)\n");
            } else {
                target.suggestions().forEach(s -> block.append("  - ").append(s.code().name())
                        .append(" at L").append(s.line()).append(": ").append(s.message()).append('\n'));
            }
        });
        return block.toString().stripTrailing();
    }

    private static String toolsBlock(final RunVariant variant) {
        return switch (variant) {
            case VANILLA -> vanillaBlock();
            case MCP_GRADLE_ONLY -> gradleOnlyBlock();
            case MCP_RECIPES -> recipesBlock();
            case HARNESS_RECIPES_THEN_AGENT -> harnessRecipesThenAgentBlock();
        };
    }

    private static String harnessRecipesThenAgentBlock() {
        return """
                IMPORTANT CONTEXT — this file has ALREADY been processed by the deterministic
                refactoring recipes before you received it. The Findings section at the bottom
                of this prompt has been RE-GENERATED after the recipe pass, so every finding
                listed is still live. You will not see findings for changes the recipes
                already made; do not spend turns looking for work that isn't there. The
                following classes of change are done already:
                  • G18 methods with no instance state have already been made static.
                  • G29 sibling guard clauses with identical bodies have already been merged.
                  • G30/G33 `if (v > CAP) v = CAP;` idioms are already `Math.min(v, CAP);`.
                  • G30 retry-style `for` loops that append a constant have already been
                    rewritten as `list.addAll(Collections.nCopies(n, x));`.
                  • G31 consecutive fluent-builder calls on the same receiver are chained.
                  • G34 `// Phase N:` and `// Step N:` section markers are gone.
                  • Ch7.1 `catch (InterruptedException)` blocks now restore the interrupt flag.

                Your job is the REMAINING findings — the ones the recipes can't do:
                  • Converting output-argument mutations to return values (F2).
                  • Splitting flag-argument methods that dispatch on a boolean (F3).
                  • Replacing if/else-if string ladders with enums (G23).
                  • Converting imperative loops to streams where the body does more than
                    a constant add (G30).
                  • Naming extracted helpers, choosing record shapes, and any finding
                    that is a judgement call.

                Available MCP tools (via the `cleancode-refactoring` server): extract_methods,
                extract_method, verify_build, run_tests, format. Same preconditions as the
                recipes variant: extract_methods when ≥2 extractions in one file.

                If extract_method / extract_methods returns ANY error, fall back to Edit
                immediately. DO NOT diagnose the recipe. DO NOT read under refactoring/,
                mcp/, plugin/, build-logic/.

                DO NOT commit or push.""";
    }

    private static String vanillaBlock() {
        return """
                No MCP tools are available for this run. Make every change manually with
                your Edit / Write tools; run the module's tests and verify the build via
                Bash (e.g. `./gradlew :<module>:compileJava`, `./gradlew :<module>:test`).
                DO NOT commit or push. Record each change in the JSON output — use "Edit"
                as the recipe name with a brief description of the change in options
                (e.g. {"change": "extracted lines 42-67 into logLines()"}).""";
    }

    private static String gradleOnlyBlock() {
        return """
                Available MCP tools (via the `cleancode-refactoring` server) — use these
                for build and test; DO NOT call `extract_method` for this run:
                  verify_build(module)
                      — `./gradlew :<module>:compileJava` → `build OK` or a compact error.
                  run_tests(module, testClass?)
                      — `./gradlew :<module>:test` → `tests: all passed` or failed names.
                  format(module)
                      — `./gradlew :<module>:spotlessApply`; call once at the end.

                Make refactorings manually with Edit / Write. The point of this variant is
                to isolate the value of compact Gradle-wrapping output from the value of
                the refactoring recipe itself. DO NOT commit or push.""";
    }

    private static String recipesBlock() {
        return """
                Available MCP tools (via the `cleancode-refactoring` server):
                  extract_methods(file, ranges=[{startLine, endLine, newMethodName}, ...])
                      — batch extraction. Applies every range in one tool call,
                        transactionally (all-or-nothing). Internally bottom-up so the
                        line numbers you pass always refer to the ORIGINAL file.
                        PREFER THIS when you have 2+ extractions planned on the same file
                        — it collapses N turns of context into one and keeps line numbers
                        mentally stable for you.

                  extract_method(file, startLine, endLine, newMethodName)
                      — singular version. Use only when you have exactly one extraction
                        to do on a file; otherwise use extract_methods.

                      Both extract tools share the same preconditions:
                        USE them when ALL of these hold:
                          • The fix is a pure statement-range extraction — pulling a
                            contiguous run of top-level statements into a helper, with no
                            other change.
                          • The range is ≥ 3 statements and a cohesive phase (e.g. a
                            section of a too-long method, a loop body, a guard block).
                          • The change does NOT also convert mutation to return, does NOT
                            introduce a record return type, and does NOT change access
                            modifiers.

                        The presence of stylistic findings on the SAME file is NOT a reason
                        to skip the extraction — do the extraction with the recipe, then fix
                        the stylistic findings with Edit on the resulting file. Treat the two
                        independently.

                        DO NOT use extract_method when the finding ITSELF is stylistic:
                          • G12 FQN, G18 static-modifier, G22 missing-final, G25
                            repeated-string, J1 unused-import — those are one-line Edits.
                            (Stylistic findings don't call for extraction on their own; but
                            their presence on a file doesn't block an unrelated extraction.)
                          • The finding calls for converting an output parameter to a
                            return value (F2) — use Edit, rewrite the signature.
                          • The finding calls for stream conversion (G30 loop→stream) —
                            use Edit with a .stream() pipeline.
                          • The method is already short (< 15 lines) with no clear
                            sub-phase — there is nothing to extract.

                  verify_build(module)
                      — `./gradlew :<module>:compileJava` → `build OK` or a compact error.
                  run_tests(module, testClass?)
                      — `./gradlew :<module>:test` → `tests: all passed` or failed names.
                  format(module)
                      — `./gradlew :<module>:spotlessApply`; call once at the end.

                Call extract_methods (batch) when you have ≥2 extractions planned on one
                file; otherwise call extract_method. Fall back to Edit for everything else.

                If either extract tool returns ANY error — including "recipe rejected the
                range" — fall back to a manual Edit immediately and move on. DO NOT diagnose
                the recipe, DO NOT read ExtractMethodRecipe.java or any other refactoring/
                or mcp/ source, DO NOT write debug tests. The recipe is not part of the fix.

                DO NOT commit or push.""";
    }
}
