package org.fiftieshousewife.cleancode.plugin.rework;

import java.util.List;

/**
 * Builds the string written to {@code claude -p}'s stdin. The prompt no
 * longer embeds the target file's contents — {@code claude -p} has the
 * {@code Read} tool and can pull it in lazily, so we hand it the
 * structured findings and a pointer. Keeps the per-run prompt small and
 * lets the agent decide how much of the file to re-read.
 */
public final class PromptBuilder {

    private PromptBuilder() {}

    public static String build(final String relativeFilePath,
                               final List<Suggestion> suggestions,
                               final boolean includeRecipeTools) {
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
                        toolsBlock(includeRecipeTools),
                        renderSuggestions(suggestions));
    }

    private static String toolsBlock(final boolean includeRecipeTools) {
        if (includeRecipeTools) {
            return """
                    Available refactoring tools (via the `cleancode-refactoring` MCP server):
                      extract_method(file, startLine, endLine, newMethodName)
                          — extracts a contiguous range of top-level statements into a new
                            package-private helper. Returns an error when the range contains
                            break/continue/non-bare return or would need more than one output
                            variable. See docs/extract-method-recipe.md for detail.
                      verify_build(module)
                          — runs `./gradlew :<module>:compileJava` and returns `build OK` or
                            the first few compiler errors. Use after every source edit.
                      run_tests(module, testClass?)
                          — runs `./gradlew :<module>:test` and returns `tests: all passed` or
                            the first few failed test names. Use after the build is green.
                      format(module)
                          — runs `./gradlew :<module>:spotlessApply` once at the end of the
                            session; individual tools do not format, so call it once after
                            your final edit rather than after every intermediate change.

                    Workflow: call the MCP tools for each edit + verification step, then call
                    format once at the end. DO NOT commit or push. If a tool returns an error,
                    record the rejection in your output and move on rather than forcing the
                    change by hand — the point of this loop is to learn what the recipes
                    cannot do yet.""";
        }
        return """
                The refactoring recipes are NOT available for this run. Make every change
                manually with your Edit / Write tools. Run the module's tests after;
                DO NOT commit or push. Record each change you made in the JSON output —
                use \"Edit\" as the recipe name and put a brief description of the change
                in options (e.g. {\"change\": \"extracted lines 42-67 into logLines()\"}).""";
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
