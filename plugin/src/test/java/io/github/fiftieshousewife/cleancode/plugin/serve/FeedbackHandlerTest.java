package io.github.fiftieshousewife.cleancode.plugin.serve;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackHandlerTest {

    @Test
    void appendsFreshFeedbackEntryWithTimestampAndContext(@TempDir Path projectRoot) throws Exception {
        final FeedbackHandler handler = new FeedbackHandler(projectRoot,
                () -> Instant.parse("2026-05-03T10:00:00Z"));

        final Path saved = handler.append("Recipe X is too noisy",
                Map.of("code", "G18", "file", "Foo.java", "line", "42"));
        final String body = Files.readString(saved);

        assertAll(
                () -> assertEquals(projectRoot.resolve("clean-code-feedback.md"), saved),
                () -> assertTrue(body.contains("## 2026-05-03T10:00:00Z"),
                        "timestamp must be the file's primary section heading"),
                () -> assertTrue(body.contains("- **code**: G18"),
                        "context is rendered as a bullet list"),
                () -> assertTrue(body.contains("> Recipe X is too noisy"),
                        "the message is rendered as a Markdown blockquote"));
    }

    @Test
    void appendsAdditionalEntryWithoutLosingPriorContent(@TempDir Path projectRoot) throws Exception {
        final FeedbackHandler handler = new FeedbackHandler(projectRoot,
                () -> Instant.parse("2026-05-03T10:00:00Z"));
        handler.append("first", Map.of());
        handler.append("second", Map.of());

        final String body = Files.readString(projectRoot.resolve("clean-code-feedback.md"));
        assertAll(
                () -> assertTrue(body.contains("> first")),
                () -> assertTrue(body.contains("> second"),
                        "second entry must be appended, not overwritten"));
    }

    @Test
    void rejectsBlankMessage(@TempDir Path projectRoot) {
        final FeedbackHandler handler = new FeedbackHandler(projectRoot);
        assertThrows(IllegalArgumentException.class, () -> handler.append("   ", Map.of()));
    }

    @Test
    void escapesPipesAndBackticksInContextValues(@TempDir Path projectRoot) throws Exception {
        final FeedbackHandler handler = new FeedbackHandler(projectRoot,
                () -> Instant.parse("2026-05-03T10:00:00Z"));
        handler.append("noisy",
                Map.of("file", "src/Foo.java | original | latest", "snippet", "`code`"));

        final String body = Files.readString(projectRoot.resolve("clean-code-feedback.md"));
        assertAll(
                () -> assertTrue(body.contains("src/Foo.java \\| original \\| latest"),
                        "pipes are escaped so they don't break Markdown table layout"),
                () -> assertTrue(body.contains("\\`code\\`"),
                        "backticks are escaped to prevent inline-code rendering"));
    }
}
