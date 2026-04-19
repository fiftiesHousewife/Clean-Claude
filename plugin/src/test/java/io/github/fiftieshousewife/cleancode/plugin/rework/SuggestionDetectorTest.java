package io.github.fiftieshousewife.cleancode.plugin.rework;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.AggregatedReport;
import io.github.fiftieshousewife.cleancode.core.Confidence;
import io.github.fiftieshousewife.cleancode.core.Finding;
import io.github.fiftieshousewife.cleancode.core.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuggestionDetectorTest {

    @Test
    void emitsOneSuggestionPerFindingOnTargetFile() {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G30, "src/main/java/Foo.java",
                        42, 67, "method too long", Severity.WARNING, Confidence.HIGH, "openrewrite", "r"),
                Finding.at(HeuristicCode.Ch10_1, "src/main/java/Foo.java",
                        1, 200, "class too long", Severity.WARNING, Confidence.HIGH, "openrewrite", "r"),
                Finding.at(HeuristicCode.G22, "src/main/java/Bar.java",
                        9, 9, "not final", Severity.WARNING, Confidence.HIGH, "checkstyle", "r"));

        final List<Suggestion> suggestions = SuggestionDetector.suggestionsFor(report, "src/main/java/Foo.java");

        assertAll(
                () -> assertEquals(2, suggestions.size(),
                        "findings on other files are filtered out"),
                () -> assertTrue(suggestions.stream().anyMatch(s -> s.code() == HeuristicCode.G30
                                && s.line() == 42 && s.message().contains("too long")),
                        "G30 suggestion carries the method's start line"),
                () -> assertTrue(suggestions.stream().anyMatch(s -> s.code() == HeuristicCode.Ch10_1),
                        "Ch10.1 suggestion present"));
    }

    @Test
    void returnsEmptyWhenTargetFileHasNoFindings() {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G22, "src/main/java/Bar.java",
                        9, 9, "not final", Severity.WARNING, Confidence.HIGH, "checkstyle", "r"));

        final List<Suggestion> suggestions = SuggestionDetector.suggestionsFor(report, "src/main/java/Foo.java");

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void matchesFileByPathSuffix() {
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G30, "core/src/main/java/com/example/Foo.java",
                        10, 80, "long", Severity.WARNING, Confidence.HIGH, "openrewrite", "r"));

        final List<Suggestion> byBaseName = SuggestionDetector.suggestionsFor(report, "Foo.java");
        final List<Suggestion> byRelativePath = SuggestionDetector.suggestionsFor(
                report, "com/example/Foo.java");

        assertAll(
                () -> assertEquals(1, byBaseName.size(),
                        "basename match so callers don't have to guess module prefixes"),
                () -> assertEquals(1, byRelativePath.size(),
                        "suffix match works for package-qualified paths"));
    }

    private static AggregatedReport reportWith(final Finding... findings) {
        final Set<HeuristicCode> covered = EnumSet.noneOf(HeuristicCode.class);
        for (final Finding f : findings) {
            covered.add(f.code());
        }
        return new AggregatedReport(List.of(findings), covered,
                Instant.now(), "test", "0");
    }
}
