package io.github.fiftieshousewife.cleancode.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StopTaskTest {

    @Test
    void readPidParsesNumericContent(@TempDir final Path tempDir) throws Exception {
        final Path pidFile = tempDir.resolve("serve.pid");
        Files.writeString(pidFile, "12345\n");

        final Optional<Long> pid = StopTask.readPid(pidFile);

        assertAll(
                () -> assertTrue(pid.isPresent()),
                () -> assertEquals(12345L, pid.get()));
    }

    @Test
    void readPidReturnsEmptyForUnparseableContent(@TempDir final Path tempDir) throws Exception {
        final Path pidFile = tempDir.resolve("serve.pid");
        Files.writeString(pidFile, "not-a-pid");

        assertFalse(StopTask.readPid(pidFile).isPresent());
    }

    @Test
    void readPidReturnsEmptyForBlankFile(@TempDir final Path tempDir) throws Exception {
        final Path pidFile = tempDir.resolve("serve.pid");
        Files.writeString(pidFile, "");

        assertFalse(StopTask.readPid(pidFile).isPresent());
    }

    @Test
    void waitForExitReturnsTrueWhenProcessExitsWithinGrace() throws Exception {
        final Process sleeper = new ProcessBuilder("sh", "-c", "sleep 0.05").start();

        final boolean exited = StopTask.waitForExit(sleeper.toHandle(), 2_000L);

        assertTrue(exited, "process exits well within the 2s grace window");
    }

    @Test
    void waitForExitReturnsFalseWhenProcessOutlivesGrace() throws Exception {
        final Process sleeper = new ProcessBuilder("sh", "-c", "sleep 5").start();
        try {
            final boolean exited = StopTask.waitForExit(sleeper.toHandle(), 200L);

            assertFalse(exited, "process should still be alive after the 200ms grace window");
        } finally {
            sleeper.destroyForcibly();
        }
    }
}
