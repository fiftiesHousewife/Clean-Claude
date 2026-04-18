package org.fiftieshousewife.cleancode.plugin.rework;

import java.util.List;

/**
 * Builds the string written to {@code claude -p}'s stdin. Three prompt
 * variants (see {@link RunVariant}) let {@link ReworkCompareTask}
 * isolate the value of the refactoring recipe from the value of the
 * compact Gradle-wrapping tools.
 *
 * <p>The prompt carries a pointer to the target file plus structured
 * findings — never the file's contents. The agent uses its Read tool
 * to pull the file lazily.
 */
public final class PromptBuilder {

    private PromptBuilder() {}

    public static String build(final String relativeFilePath,
                               final List<Suggestion> suggestions,
                               final RunVariant variant) {
        return """
                You are reworking a single Java file to address the findings listed below.

                Target file (relative to project root): %s
                Read the file with your Read tool before planning the fix. Do NOT embed its
                contents in your response — the commit diff captures the actual change.

                %s

                Before exiting, emit EXACTLY ONE JSON object as the last thing you write. The
                schema is:

                {
                  "actions":  [{"recipe": "<RecipeClass-or-Edit>", "options": {...}, "why": "<one sentence>"}],
                  "rejected": [{"recipe": "<RecipeClass-or-Edit>", "options": {...}, "why": "<one sentence>"}]
                }

                Findings on this file:
                %s
                """.formatted(
                        relativeFilePath,
                        toolsBlock(variant),
                        renderSuggestions(suggestions));
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

    private static String renderSuggestions(final List<Suggestion> suggestions) {
        if (suggestions.isEmpty()) {
            return "  (no findings — bail out with empty actions and rejected arrays)";
        }
        final StringBuilder rendered = new StringBuilder();
        suggestions.forEach(s -> rendered.append("  - ").append(s.code().name())
                .append(" at L").append(s.line()).append(": ").append(s.message()).append('\n'));
        return rendered.toString().stripTrailing();
    }
}
