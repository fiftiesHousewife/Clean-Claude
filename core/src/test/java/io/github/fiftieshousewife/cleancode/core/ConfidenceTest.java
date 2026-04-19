package io.github.fiftieshousewife.cleancode.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfidenceTest {

    @Test
    void allConfidencesExist() {
        assertDoesNotThrow(() -> {
            Confidence.valueOf("HIGH");
            Confidence.valueOf("MEDIUM");
            Confidence.valueOf("LOW");
        });
    }

    @Test
    void confidenceCount() {
        assertEquals(3, Confidence.values().length);
    }
}
