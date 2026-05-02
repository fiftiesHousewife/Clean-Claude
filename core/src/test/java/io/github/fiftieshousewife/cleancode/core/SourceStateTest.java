package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceStateTest {

    @Test
    void classifiesUnavailableSourceRegardlessOfFindings() {
        final SourceState state = SourceState.classify("pmd", "PMD", false, List.of());

        assertAll(
                () -> assertEquals(SourceState.Status.NOT_AVAILABLE, state.status()),
                () -> assertEquals(0, state.findingCount()));
    }

    @Test
    void classifiesAvailableSourceWithoutFindingsAsRanNoFindings() {
        final SourceState state = SourceState.classify("openrewrite", "OpenRewrite", true, List.of());

        assertEquals(SourceState.Status.RAN_NO_FINDINGS, state.status());
    }

    @Test
    void classifiesAvailableSourceWithFindingsAsProduced() {
        final Finding f = new Finding(HeuristicCode.G4, "Foo.java", 1, 1, "x",
                Severity.ERROR, Confidence.HIGH, "openrewrite", "EmptyBlock", Map.of());

        final SourceState state = SourceState.classify("openrewrite", "OpenRewrite", true, List.of(f));

        assertAll(
                () -> assertEquals(SourceState.Status.PRODUCED_FINDINGS, state.status()),
                () -> assertEquals(1, state.findingCount()));
    }
}
