package org.fiftieshousewife.cleancode.claudereview;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeReviewFindingSourceTest {

    private ClaudeReviewFindingSource source;

    @BeforeEach
    void setUp() {
        final ClaudeReviewConfig config = new ClaudeReviewConfig(
                true, "", "claude-sonnet-4-6",
                Set.of(HeuristicCode.G6, HeuristicCode.G20, HeuristicCode.N4),
                new ClaudeReviewConfig.FileSelection(50, 10, List.of()));
        source = new ClaudeReviewFindingSource(config);
    }

    @Test
    void parsesValidJsonResponse() {
        final String json = """
                [
                  {"code": "G6", "startLine": 15, "endLine": 20, "message": "SQL query in controller"},
                  {"code": "G20", "startLine": 42, "endLine": 42, "message": "Method 'process' does not describe its action"}
                ]
                """;

        final List<Finding> findings = source.parseFindings(json, "src/main/java/Foo.java");

        assertAll(
                () -> assertEquals(2, findings.size()),
                () -> assertEquals(HeuristicCode.G6, findings.get(0).code()),
                () -> assertEquals(15, findings.get(0).startLine()),
                () -> assertEquals("SQL query in controller", findings.get(0).message()),
                () -> assertEquals("src/main/java/Foo.java", findings.get(0).sourceFile()),
                () -> assertEquals(Severity.WARNING, findings.get(0).severity()),
                () -> assertEquals(Confidence.LOW, findings.get(0).confidence()),
                () -> assertEquals("claude-review", findings.get(0).tool())
        );
    }

    @Test
    void parsesEmptyArrayResponse() {
        final List<Finding> findings = source.parseFindings("[]", "Foo.java");

        assertTrue(findings.isEmpty());
    }

    @Test
    void returnEmptyForMalformedJson() {
        final List<Finding> findings = source.parseFindings("this is not json", "Foo.java");

        assertTrue(findings.isEmpty());
    }

    @Test
    void skipsUnknownHeuristicCodes() {
        final String json = """
                [{"code": "ZZZZ", "startLine": 1, "endLine": 1, "message": "unknown"}]
                """;

        final List<Finding> findings = source.parseFindings(json, "Foo.java");

        assertTrue(findings.isEmpty());
    }

    @Test
    void skipsCodesNotInEnabledSet() {
        final String json = """
                [{"code": "G31", "startLine": 1, "endLine": 1, "message": "temporal coupling"}]
                """;

        final List<Finding> findings = source.parseFindings(json, "Foo.java");

        assertTrue(findings.isEmpty());
    }

    @Test
    void skipsInvalidLineNumbers() {
        final String json = """
                [{"code": "G6", "startLine": 0, "endLine": 0, "message": "bad line"}]
                """;

        final List<Finding> findings = source.parseFindings(json, "Foo.java");

        assertTrue(findings.isEmpty());
    }

    @Test
    void extractsJsonArrayFromMarkdownFences() {
        final String json = """
                ```json
                [{"code": "G20", "startLine": 5, "endLine": 5, "message": "vague name"}]
                ```
                """;

        final List<Finding> findings = source.parseFindings(json, "Foo.java");

        assertEquals(1, findings.size());
    }

    @Test
    void idReturnsClaudeReview() {
        assertEquals("claude-review", source.id());
    }

    @Test
    void coveredCodesMatchesConfig() {
        final Set<HeuristicCode> covered = source.coveredCodes();

        assertTrue(covered.containsAll(Set.of(HeuristicCode.G6, HeuristicCode.G20, HeuristicCode.N4)));
        assertEquals(3, covered.size());
    }

    @Test
    void isNotAvailableWithoutApiKey() {
        assertFalse(source.isAvailable(null));
    }
}
