package io.github.fiftieshousewife.cleancode.mcp;

import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Drives Gradle for {@link VerifyBuildTool}, {@link RunTestsTool}, and
 * {@link FormatTool} via the Tooling API's {@link ProjectConnection}.
 * One connection is opened for the server's lifetime so every tool call
 * reuses the same daemon with no {@code ./gradlew} wrapper JVM startup
 * per invocation. Tests inject a {@link Runner} lambda and never touch
 * the Tooling API at all.
 */
public final class GradleInvoker implements AutoCloseable {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);
    private static final String PLAIN_CONSOLE = "--console=plain";
    private static final String QUIET = "-q";
    // Every MCP Gradle invocation passes this so sandbox targets resolve.
    // settings.gradle.kts only includes `:sandbox` when the flag is set; other
    // modules ignore the property, so it's safe to pass unconditionally.
    private static final String SANDBOX_OPT_IN = "-PcleanCodeSelfApply=true";

    public record Result(int exitCode, String output) {}

    @FunctionalInterface
    public interface Runner {
        Result run(List<String> args, Duration timeout) throws IOException;
    }

    private final Runner runner;
    private final ProjectConnection connection;

    public GradleInvoker(final Path projectRoot) {
        this.connection = GradleConnector.newConnector()
                .forProjectDirectory(projectRoot.toFile())
                .connect();
        this.runner = this::runViaTooling;
    }

    public GradleInvoker(final Runner runner) {
        this.connection = null;
        this.runner = runner;
    }

    public Result invoke(final String... gradleArgs) throws IOException {
        final List<String> args = new ArrayList<>();
        args.add(PLAIN_CONSOLE);
        args.add(QUIET);
        args.add(SANDBOX_OPT_IN);
        for (final String arg : gradleArgs) {
            args.add(arg);
        }
        return runner.run(args, DEFAULT_TIMEOUT);
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.close();
        }
    }

    private Result runViaTooling(final List<String> args, final Duration timeout) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            connection.newBuild()
                    .withArguments(args)
                    .setStandardOutput(out)
                    .setStandardError(out)
                    .run();
            return new Result(0, out.toString(StandardCharsets.UTF_8));
        } catch (BuildException e) {
            return new Result(1, out.toString(StandardCharsets.UTF_8));
        } catch (GradleConnectionException e) {
            throw new IOException("gradle tooling-api failure: " + e.getMessage(), e);
        }
    }
}
