package org.fiftieshousewife.cleancode.plugin.rework;

import java.util.List;

/**
 * Builds the string written to {@code claude -p}'s stdin. Keeps the
 * instructions, the target file, the findings, and the required JSON
 * response schema in one place so the prompt can be eyeballed without
 * digging through the orchestrator. Pure; tests drive it directly.
 */
public final class PromptBuilder {

    private PromptBuilder() {}

    public static String build(final String relativeFilePath, final String fileContents,
                               final List<Suggestion> suggestions) {
        return """
                You are reworking a single Java file to address the findings listed below.

                Available recipes, each invocable via Gradle:
                  ./gradlew :refactoring:extractMethod \\
                      -Pfile=<path> -PstartLine=<N> -PendLine=<M> -PnewMethodName=<name>
                  ./gradlew :refactoring:moveMethod \\
                      -Pfile=<path> -PmethodName=<name> -PtargetFqn=<fully-qualified-class>
                See docs/extract-method-recipe.md for usage detail and rejection reasons.

                Workflow: make your changes, run the module's tests, but DO NOT commit or push.
                If a recipe rejects, record the rejection in your output and move on rather than
                forcing the change by hand — the point of this loop is to learn what the recipes
                cannot do yet.

                Before exiting, emit EXACTLY ONE JSON object as the last thing you write. The
                schema is:

                {
                  "actions":  [{"recipe": "<RecipeClass>", "options": {...}, "why": "<one sentence>"}],
                  "rejected": [{"recipe": "<RecipeClass>", "options": {...}, "why": "<one sentence>"}]
                }

                Target file (relative to project root): %s

                ---
                %s
                ---

                Findings on this file:
                %s
                """.formatted(relativeFilePath, fileContents, renderSuggestions(suggestions));
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
