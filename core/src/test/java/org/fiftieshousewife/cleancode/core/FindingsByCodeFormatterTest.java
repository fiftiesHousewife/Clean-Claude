package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindingsByCodeFormatterTest {

    @Test
    void formatsFileLocationWithStartLine() {
        final Finding finding = Finding.at(HeuristicCode.F3, "src/Foo.java", 42, 42,
                "msg", Severity.WARNING, Confidence.HIGH, "tool", "F3");

        assertEquals("src/Foo.java:42", FindingsByCodeFormatter.formatLocation(finding));
    }

    @Test
    void formatsFileLocationWithoutStartLineWhenLineIsNotPositive() {
        final Finding finding = Finding.at(HeuristicCode.F3, "src/Foo.java", 0, 0,
                "msg", Severity.WARNING, Confidence.HIGH, "tool", "F3");

        assertEquals("src/Foo.java", FindingsByCodeFormatter.formatLocation(finding));
    }

    @Test
    void formatsProjectLevelFindingAsProjectLocation() {
        final Finding finding = Finding.projectLevel(HeuristicCode.T1,
                "msg", Severity.ERROR, Confidence.HIGH, "tool", "T1");

        assertEquals("(project)", FindingsByCodeFormatter.formatLocation(finding));
    }

    @Test
    void appendCodeGroupIncludesHeaderSummaryAndEachFinding() {
        final StringBuilder out = new StringBuilder();
        final Finding first = Finding.at(HeuristicCode.F3, "A.java", 1, 1,
                "first message", Severity.WARNING, Confidence.HIGH, "tool", "F3");
        final Finding second = Finding.at(HeuristicCode.F3, "B.java", 2, 2,
                "second message", Severity.WARNING, Confidence.HIGH, "tool", "F3");

        FindingsByCodeFormatter.appendCodeGroup(out, HeuristicCode.F3, List.of(first, second));

        final String rendered = out.toString();
        assertAll(
                () -> assertTrue(rendered.contains("F3:")),
                () -> assertTrue(rendered.contains("(2)")),
                () -> assertTrue(rendered.contains("A.java:1")),
                () -> assertTrue(rendered.contains("first message")),
                () -> assertTrue(rendered.contains("B.java:2")),
                () -> assertTrue(rendered.contains("second message"))
        );
    }
}
