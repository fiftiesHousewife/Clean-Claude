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
                files' contents in your response — the commit diff captures the actual change.""");
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
        };
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
                  extract_method(file, startLine, endLine, newMethodName)
                      — extracts a contiguous range of top-level statements into a new
                        package-private helper. Returns an error when the range does not
                        map cleanly; record the rejection and try a narrower range.
                  verify_build(module)
                      — `./gradlew :<module>:compileJava` → `build OK` or a compact error.
                  run_tests(module, testClass?)
                      — `./gradlew :<module>:test` → `tests: all passed` or failed names.
                  format(module)
                      — `./gradlew :<module>:spotlessApply`; call once at the end.

                Prefer the recipe tool over manual Edit where the shape of the change is
                an extraction; fall back to Edit only when the recipe rejects. DO NOT
                commit or push.""";
    }
}
