package org.fiftieshousewife.cleancode.plugin.rework;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around the handful of {@code git} commands
 * {@link ReworkCompareTask} needs between the paired runs. Kept
 * separate so the comparison task doesn't interleave orchestrator
 * logic with ProcessBuilder ceremony.
 */
public final class GitWorkingTree {

    private static final int COMMAND_TIMEOUT_SECONDS = 30;

    private final Path projectRoot;

    public GitWorkingTree(final Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public boolean isClean(final Path file) throws GitException {
        return run("git", "status", "--porcelain", "--", file.toString()).stdout().isBlank();
    }

    public String diff(final Path file) throws GitException {
        return run("git", "diff", "--", file.toString()).stdout();
    }

    public void restore(final Path file) throws GitException {
        final CommandResult result = run("git", "restore", "--", file.toString());
        if (result.exitCode() != 0) {
            throw new GitException("git restore failed: " + result.stdout());
        }
    }

    private CommandResult run(final String... command) throws GitException {
        try {
            final Process process = new ProcessBuilder(command)
                    .directory(projectRoot.toFile())
                    .redirectErrorStream(true)
                    .start();
            final boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new GitException("git command timed out: " + String.join(" ", command));
            }
            final String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new CommandResult(process.exitValue(), out);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new GitException("git command failed: " + String.join(" ", command), e);
        }
    }

    private record CommandResult(int exitCode, String stdout) {}

    public static final class GitException extends Exception {
        private static final long serialVersionUID = 1L;
        public GitException(final String message) {
            super(message);
        }
        public GitException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
