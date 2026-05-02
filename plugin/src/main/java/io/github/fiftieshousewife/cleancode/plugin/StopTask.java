package io.github.fiftieshousewife.cleancode.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Stops a running {@code cleanCodeServe} task by reading its PID file and
 * sending SIGTERM, escalating to SIGKILL if the process is still alive
 * after a grace window.
 *
 * <p>The serve task writes its PID to {@code build/clean-code/serve.pid}
 * before binding the port and deletes it on shutdown. If no PID file is
 * present we report nothing-to-do; that is not an error.
 */
@DisableCachingByDefault(because = "performs a side-effecting process kill and inspects on-disk state")
public abstract class StopTask extends DefaultTask {

    static final long TERM_GRACE_MILLIS = 5_000L;
    static final long POLL_INTERVAL_MILLIS = 100L;

    @TaskAction
    public void stop() {
        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        final Path pidFile = buildDir.resolve("clean-code/serve.pid");

        if (!Files.exists(pidFile)) {
            getLogger().lifecycle("No running cleanCodeServe found (no PID file at {}).", pidFile);
            return;
        }

        final Optional<Long> pid = readPid(pidFile);
        if (pid.isEmpty()) {
            getLogger().warn("PID file at {} is unreadable; deleting.", pidFile);
            deletePidFile(pidFile);
            return;
        }

        final Optional<ProcessHandle> handle = ProcessHandle.of(pid.get());
        if (handle.isEmpty() || !handle.get().isAlive()) {
            getLogger().lifecycle("PID {} is not running; cleaning up stale PID file.", pid.get());
            deletePidFile(pidFile);
            return;
        }

        final ProcessHandle process = handle.get();
        getLogger().lifecycle("Sending SIGTERM to cleanCodeServe (pid={}).", pid.get());
        process.destroy();

        if (waitForExit(process, TERM_GRACE_MILLIS)) {
            getLogger().lifecycle("cleanCodeServe stopped.");
            deletePidFile(pidFile);
            return;
        }

        getLogger().warn("cleanCodeServe did not exit after {} ms; sending SIGKILL.", TERM_GRACE_MILLIS);
        process.destroyForcibly();
        waitForExit(process, TERM_GRACE_MILLIS);
        deletePidFile(pidFile);
    }

    static Optional<Long> readPid(final Path pidFile) {
        try {
            final String content = Files.readString(pidFile).trim();
            if (content.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(content));
        } catch (IOException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    static boolean waitForExit(final ProcessHandle process, final long graceMillis) {
        final long deadline = System.currentTimeMillis() + graceMillis;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                return true;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return !process.isAlive();
            }
        }
        return !process.isAlive();
    }

    private void deletePidFile(final Path pidFile) {
        try {
            Files.deleteIfExists(pidFile);
        } catch (IOException e) {
            getLogger().warn("Could not delete PID file at {}: {}", pidFile, e.getMessage());
        }
    }
}
