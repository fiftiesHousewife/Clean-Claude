package io.github.fiftieshousewife.cleancode.plugin.serve;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PendingChangeTest {

    @Test
    void requiresNonBlankReason() {
        final IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                () -> new PendingChange("disableRecipe", Map.of("code", "G30"), ""));
        final IllegalArgumentException nullReason = assertThrows(IllegalArgumentException.class,
                () -> new PendingChange("disableRecipe", Map.of("code", "G30"), null));
        assertEquals("reason is required for every staged change", blank.getMessage());
        assertEquals("reason is required for every staged change", nullReason.getMessage());
    }

    @Test
    void requiresNonBlankKind() {
        assertThrows(IllegalArgumentException.class,
                () -> new PendingChange("", Map.of("code", "G30"), "x"));
    }

    @Test
    void requireParamSurfacesMissingFieldByName() {
        final PendingChange change = new PendingChange("suppressFinding",
                Map.of("file", "Foo.java"), "false positive");
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> change.requireParam("line"));
        assertEquals("suppressFinding change is missing required param: line", thrown.getMessage());
    }
}
