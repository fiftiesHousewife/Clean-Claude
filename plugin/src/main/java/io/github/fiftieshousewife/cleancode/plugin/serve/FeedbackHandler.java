package io.github.fiftieshousewife.cleancode.plugin.serve;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Appends user feedback to {@code clean-code-feedback.md} at the
 * project root. Captures everything in one local Markdown file so the
 * user can review and commit on their own schedule; no GitHub or HTTP
 * integration on this side.
 *
 * <p>Each entry includes the submission timestamp and any context the
 * UI passes through (typically the heuristic code, file, line, and
 * finding message that the user was looking at when they clicked
 * &quot;Request a change&quot;).
 */
public final class FeedbackHandler {

    private static final String FILE_NAME = "clean-code-feedback.md";

    private final Path projectRoot;
    private final java.util.function.Supplier<Instant> clock;

    public FeedbackHandler(final Path projectRoot) {
        this(projectRoot, Instant::now);
    }

    FeedbackHandler(final Path projectRoot, final java.util.function.Supplier<Instant> clock) {
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Path append(final String message, final Map<String, String> context) throws IOException {
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("feedback message must not be blank");
        }
        final Path file = projectRoot.resolve(FILE_NAME);
        Files.writeString(file, render(message, context),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        return file;
    }

    private String render(final String message, final Map<String, String> context) {
        final StringBuilder body = new StringBuilder();
        body.append("\n## ").append(clock.get().toString()).append('\n');
        if (context != null && !context.isEmpty()) {
            body.append("\n### Context\n\n");
            context.forEach((k, v) -> body.append("- **").append(escapeMarkdown(k))
                    .append("**: ").append(escapeMarkdown(v)).append('\n'));
        }
        body.append("\n### Message\n\n");
        for (final String line : message.split("\\r?\\n", -1)) {
            body.append("> ").append(escapeMarkdown(line)).append('\n');
        }
        body.append('\n');
        return body.toString();
    }

    private static String escapeMarkdown(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("`", "\\`");
    }
}
