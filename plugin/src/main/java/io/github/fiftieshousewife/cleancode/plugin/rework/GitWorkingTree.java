package io.github.fiftieshousewife.cleancode.plugin.rework;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around the handful of {@code git} commands
 * {@link ReworkCompareTask} needs between the paired runs. Each
 * method accepts a single {@link Path} (one-file shortcut) or a
 * {@link List} (batched runs).
 */
public final class GitWorkingTree {

    private static final int COMMAND_TIMEOUT_SECONDS = 30;

    private final Path projectRoot;

    public GitWorkingTree(final Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public boolean isClean(final Path file) throws GitException {
        return isClean(List.of(file));
    }

    public boolean isClean(final List<Path> files) throws GitException {
        return run(command("status", List.of("--porcelain"), files)).stdout().isBlank();
    }

    public String diff(final Path file) throws GitException {
        return diff(List.of(file));
    }

    public String diff(final List<Path> files) throws GitException {
        return run(command("diff", List.of(), files)).stdout();
    }

    public void restore(final Path file) throws GitException {
        restore(List.of(file));
    }

    public void restore(final List<Path> files) throws GitException {
        final CommandResult result = run(command("restore", List.of(), files));
        if (result.exitCode() != 0) {
            throw new GitException("git restore failed: " + result.stdout());
        }
    }

    private static String[] command(final String subcommand, final List<String> flags,
                                    final List<Path> files) {
        final List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.add(subcommand);
        cmd.addAll(flags);
        cmd.add("--");
        files.forEach(file -> cmd.add(file.toString()));
        return cmd.toArray(new String[0]);
    }

    private CommandResult run(final String[] command) throws GitException {
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
