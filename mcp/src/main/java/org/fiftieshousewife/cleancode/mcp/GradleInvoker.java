package org.fiftieshousewife.cleancode.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs {@code ./gradlew} in the project root for {@link VerifyBuildTool}
 * and {@link RunTestsTool}. Keeps ProcessBuilder ceremony out of the
 * individual tools, and lets tests swap in a fake by injecting a
 * {@code runner}.
 */
public final class GradleInvoker {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);
    private static final String GRADLEW = "./gradlew";
    private static final String PLAIN_CONSOLE = "--console=plain";
    private static final String QUIET = "-q";

    public record Result(int exitCode, String output) {}

    @FunctionalInterface
    public interface Runner {
        Result run(List<String> command, Path workingDir, Duration timeout) throws IOException, InterruptedException;
    }

    private final Path projectRoot;
    private final Runner runner;

    public GradleInvoker(final Path projectRoot) {
        this(projectRoot, GradleInvoker::runProcess);
    }

    public GradleInvoker(final Path projectRoot, final Runner runner) {
        this.projectRoot = projectRoot;
        this.runner = runner;
    }

    public Result invoke(final String... gradleArgs) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>();
        command.add(GRADLEW);
        command.add(PLAIN_CONSOLE);
        command.add(QUIET);
        for (final String arg : gradleArgs) {
            command.add(arg);
        }
        return runner.run(command, projectRoot, DEFAULT_TIMEOUT);
    }

    private static Result runProcess(final List<String> command, final Path workingDir,
                                     final Duration timeout) throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(command)
                .directory(workingDir.toFile())
                .redirectErrorStream(true)
                .start();
        final boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("gradle command timed out after " + timeout + ": " + String.join(" ", command));
        }
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(process.exitValue(), output);
    }
}
