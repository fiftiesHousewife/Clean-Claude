package io.github.fiftieshousewife.cleancode.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeverityTest {

    @Test
    void allSeveritiesExist() {
        assertDoesNotThrow(() -> {
            Severity.valueOf("ERROR");
            Severity.valueOf("WARNING");
            Severity.valueOf("INFO");
        });
    }

    @Test
    void severityCount() {
        assertEquals(3, Severity.values().length);
    }
}
