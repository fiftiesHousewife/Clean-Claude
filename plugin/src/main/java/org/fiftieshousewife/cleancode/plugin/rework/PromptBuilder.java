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
                    Available refactoring recipes, each invocable via Gradle:
                      ./gradlew :refactoring:extractMethod \\
                          -Pfile=<path> -PstartLine=<N> -PendLine=<M> -PnewMethodName=<name>
                      ./gradlew :refactoring:moveMethod \\
                          -Pfile=<path> -PmethodName=<name> -PtargetFqn=<fully-qualified-class>
                    See docs/extract-method-recipe.md for usage detail and rejection reasons.

                    Workflow: make your changes, run the module's tests, but DO NOT commit or push.
                    If a recipe rejects, record the rejection in your output and move on rather than
                    forcing the change by hand — the point of this loop is to learn what the recipes
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
