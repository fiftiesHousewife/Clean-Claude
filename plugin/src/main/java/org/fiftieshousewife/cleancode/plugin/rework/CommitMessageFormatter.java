package org.fiftieshousewife.cleancode.plugin.rework;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the Markdown body that goes underneath the subject line of a
 * rework commit. Each action becomes a bullet with the recipe name, a
 * compact {@code key=value} option rendering, and the agent's one-line
 * {@code why}. Rejections and suggestions get their own sections —
 * omitted entirely when empty, so the output never carries blank
 * headings.
 */
public final class CommitMessageFormatter {

    private static final String ACTIONS_HEADER = "## Actions";
    private static final String REJECTED_HEADER = "## Not attempted";
    private static final String SUGGESTIONS_HEADER = "## Suggestions";

    private CommitMessageFormatter() {}

    public static String format(final List<AgentAction> actions,
                                final List<AgentRejection> rejected,
                                final List<Suggestion> suggestions) {
        final StringBuilder body = new StringBuilder();
        appendActions(body, actions);
        appendRejections(body, rejected);
        appendSuggestions(body, suggestions);
        return body.toString().stripTrailing();
    }

    private static void appendActions(final StringBuilder body, final List<AgentAction> actions) {
        if (actions.isEmpty()) {
            return;
        }
        body.append(ACTIONS_HEADER).append('\n');
        actions.forEach(a -> body.append("- ")
                .append(a.recipe()).append('(').append(formatOptions(a.options())).append(')')
                .append(" — ").append(a.why()).append('\n'));
        body.append('\n');
    }

    private static void appendRejections(final StringBuilder body, final List<AgentRejection> rejected) {
        if (rejected.isEmpty()) {
            return;
        }
        body.append(REJECTED_HEADER).append('\n');
        rejected.forEach(r -> body.append("- ")
                .append(r.recipe()).append('(').append(formatOptions(r.options())).append(')')
                .append(" — ").append(r.why()).append('\n'));
        body.append('\n');
    }

    private static void appendSuggestions(final StringBuilder body, final List<Suggestion> suggestions) {
        if (suggestions.isEmpty()) {
            return;
        }
        body.append(SUGGESTIONS_HEADER).append('\n');
        suggestions.forEach(s -> body.append("- ")
                .append(s.code().name()).append(" at L").append(s.line())
                .append(": ").append(s.message()).append('\n'));
        body.append('\n');
    }

    private static String formatOptions(final Map<String, Object> options) {
        return options.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
