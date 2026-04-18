package org.fiftieshousewife.cleancode.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractMethodToolTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsCleanlyAndRewritesFileOnDisk() throws IOException {
        final Path file = tempDir.resolve("Greeter.java");
        Files.writeString(file, """
                package com.example;
                public class Greeter {
                    public void greet(String name) {
                        if (name == null) {
                            return;
                        }
                        System.out.println(name);
                    }
                }
                """);

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", file.toString(),
                "startLine", 4,
                "endLine", 6,
                "newMethodName", "isMissing"));

        assertAll(
                () -> assertFalse(result.isError(), "happy path reports success"),
                () -> assertTrue(result.text().contains("extracted isMissing"),
                        "result names the helper"),
                () -> assertTrue(Files.readString(file).contains("boolean isMissing"),
                        "the recipe's output landed on disk"));
    }

    @Test
    void surfacesRecipeRejectionAsErrorWithReason() throws IOException {
        final Path file = tempDir.resolve("HasReturn.java");
        Files.writeString(file, """
                package com.example;
                public class HasReturn {
                    public int find(int[] xs, int target) {
                        for (int x : xs) {
                            if (x == target) {
                                return x;
                            }
                        }
                        return -1;
                    }
                }
                """);
        final String before = Files.readString(file);

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", file.toString(),
                "startLine", 4,
                "endLine", 8,
                "newMethodName", "scan"));

        assertAll(
                () -> assertTrue(result.isError(),
                        "a recipe rejection surfaces as an error result"),
                () -> assertTrue(result.text().contains("extract_method rejected:")
                                || result.text().contains("control-flow escape"),
                        "the error text names the kind of rejection, got: " + result.text()),
                () -> assertTrue(Files.readString(file).equals(before),
                        "rejected extractions leave the file byte-identical"));
    }

    @Test
    void extractsCleanlyWhenJavadocContainsClassKeyword() throws IOException {
        final Path file = tempDir.resolve("Greeter.java");
        Files.writeString(file, """
                package com.example;
                /**
                 * Greets callers by name. Note: this class holds no state — every
                 * call is independent and the class behaves as a pure utility.
                 */
                public class Greeter {
                    public void greet(String name) {
                        if (name == null) {
                            return;
                        }
                        System.out.println(name);
                    }
                }
                """);

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", file.toString(),
                "startLine", 8,
                "endLine", 10,
                "newMethodName", "isMissing"));

        assertAll(
                () -> assertFalse(result.isError(),
                        "Javadoc containing 'class holds' must not defeat source-path matching"),
                () -> assertTrue(Files.readString(file).contains("boolean isMissing")));
    }

    @Test
    void extractsCleanlyAgainstPipelineFixtureShape() throws IOException {
        final Path file = tempDir.resolve("PipelineFixture.java");
        Files.writeString(file, """
                package org.fiftieshousewife.cleancode.sandbox;

                import java.util.ArrayList;
                import java.util.List;

                /**
                 * Pure-extract-method fixture. {@link #run} is a long orchestrator
                 * method carrying five distinct phases, each cohesive and of extractable
                 * size; the class holds instance state (so G18 doesn't fire) and the
                 * method has a void return (so F2 / mutation-vs-return doesn't fire).
                 * There are no fully-qualified references, no magic string literals,
                 * no section comments — so G12 / G25 / G34 don't fire either.
                 */
                public final class PipelineFixture {

                    private final StringBuilder audit = new StringBuilder();
                    private final List<String> warnings = new ArrayList<>();

                    public void run(final List<String> events, final int threshold) {
                        audit.append("pipeline start\\n");
                        audit.append("input size: ").append(events.size()).append('\\n');
                        audit.append("threshold: ").append(threshold).append('\\n');
                        audit.append("warnings reset\\n");
                        warnings.clear();

                        final List<String> validated = new ArrayList<>();
                        for (final String event : events) {
                            if (event == null || event.isBlank()) {
                                warnings.add("skipped blank event");
                                continue;
                            }
                            validated.add(event.trim());
                        }
                        audit.append("validated: ").append(validated.size()).append('\\n');
                    }
                }
                """);

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", file.toString(),
                "startLine", 22,
                "endLine", 26,
                "newMethodName", "startAudit"));

        assertAll(
                () -> assertFalse(result.isError(),
                        "expected success, got: " + result.text()),
                () -> assertTrue(Files.readString(file).contains("startAudit(")));
    }

    @Test
    void extractsLines64to67AgainstRealPipelineFixtureOnDisk() throws IOException {
        final Path realFile = Path.of(System.getProperty("user.dir"))
                .resolve("../sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/PipelineFixture.java")
                .normalize();
        if (!Files.exists(realFile)) {
            return;
        }
        final Path copy = tempDir.resolve("PipelineFixture.java");
        Files.writeString(copy, Files.readString(realFile));

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", copy.toString(),
                "startLine", 64,
                "endLine", 67,
                "newMethodName", "writeAuditSummary"));

        assertFalse(result.isError(),
                "real on-disk PipelineFixture at lines 64-67 must extract. got: " + result.text());
    }

    @Test
    void extractsWhenCallerPassesRelativePath() throws IOException {
        final Path file = tempDir.resolve("sandbox/src/main/Greeter.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                package com.example;
                public class Greeter {
                    public void greet(String name) {
                        if (name == null) {
                            return;
                        }
                        System.out.println(name);
                    }
                }
                """);
        final Path cwd = Path.of("").toAbsolutePath();
        final Path relative = cwd.relativize(file.toAbsolutePath());

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", relative.toString(),
                "startLine", 4,
                "endLine", 6,
                "newMethodName", "isMissing"));

        assertAll(
                () -> assertFalse(result.isError(),
                        "relative path like `sandbox/.../Foo.java` — the shape agents pass — must "
                                + "not defeat source-path matching. got: " + result.text()),
                () -> assertTrue(Files.readString(file).contains("boolean isMissing")));
    }

    @Test
    void extractsPipelineEndPhaseLines64to67() throws IOException {
        final Path file = tempDir.resolve("PipelineFixture.java");
        Files.writeString(file, """
                package org.fiftieshousewife.cleancode.sandbox;

                import java.util.ArrayList;
                import java.util.List;

                /**
                 * Pure-extract-method fixture. {@link #run} is a long orchestrator
                 * method carrying five distinct phases, each cohesive and of extractable
                 * size; the class holds instance state (so G18 doesn't fire) and the
                 * method has a void return (so F2 / mutation-vs-return doesn't fire).
                 * There are no fully-qualified references, no magic string literals,
                 * no section comments — so G12 / G25 / G34 don't fire either. The only
                 * live findings should be G30 (method too long) and Ch10.1 (class too
                 * long), which is exactly the shape extract_method was built for.
                 */
                public final class PipelineFixture {

                    private final StringBuilder audit = new StringBuilder();
                    private final List<String> warnings = new ArrayList<>();
                    private int eventsProcessed = 0;
                    private int eventsSkipped = 0;

                    public void run(final List<String> events, final int threshold) {
                        audit.append("pipeline start\\n");
                        audit.append("input size: ").append(events.size()).append('\\n');
                        audit.append("threshold: ").append(threshold).append('\\n');
                        audit.append("warnings reset\\n");
                        warnings.clear();

                        final List<String> validated = new ArrayList<>();
                        for (final String event : events) {
                            if (event == null || event.isBlank()) {
                                warnings.add("skipped blank event");
                                eventsSkipped++;
                                continue;
                            }
                            validated.add(event.trim());
                        }
                        audit.append("validated: ").append(validated.size()).append('\\n');

                        final List<String> normalized = new ArrayList<>();
                        for (final String event : validated) {
                            final String lower = event.toLowerCase();
                            final String collapsed = lower.replaceAll("\\\\s+", " ");
                            final String stripped = collapsed.replaceAll("[^a-z0-9 ]", "");
                            normalized.add(stripped);
                        }
                        audit.append("normalized: ").append(normalized.size()).append('\\n');

                        int accepted = 0;
                        int rejected = 0;
                        for (final String event : normalized) {
                            if (event.length() < threshold) {
                                rejected++;
                                warnings.add("rejected short event: " + event);
                                continue;
                            }
                            accepted++;
                            eventsProcessed++;
                        }
                        audit.append("accepted: ").append(accepted).append('\\n');
                        audit.append("rejected: ").append(rejected).append('\\n');

                        audit.append("pipeline end\\n");
                        audit.append("warnings: ").append(warnings.size()).append('\\n');
                        audit.append("processed (cumulative): ").append(eventsProcessed).append('\\n');
                        audit.append("skipped (cumulative): ").append(eventsSkipped).append('\\n');
                    }
                }
                """);

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", file.toString(),
                "startLine", 64,
                "endLine", 67,
                "newMethodName", "writeAuditSummary"));

        assertAll(
                () -> assertFalse(result.isError(),
                        "end-phase audit appends have no break/continue/return and no outer locals "
                                + "are reassigned — expected success, got: " + result.text()));
    }

    @Test
    void reportsBadArgumentsWhenLineNumberIsNotNumeric() throws IOException {
        final Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, "class Foo {}");

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", file.toString(),
                "startLine", "not-a-number",
                "endLine", 5,
                "newMethodName", "x"));

        assertAll(
                () -> assertTrue(result.isError()),
                () -> assertTrue(result.text().contains("bad arguments")));
    }

    @Test
    void successResponseReportsFileLengthDelta() throws IOException {
        final Path file = tempDir.resolve("Greeter.java");
        Files.writeString(file, """
                package com.example;
                public class Greeter {
                    public void greet(String name) {
                        if (name == null) {
                            return;
                        }
                        System.out.println(name);
                    }
                }
                """);

        final ToolResult result = new ExtractMethodTool().call(Map.of(
                "file", file.toString(),
                "startLine", 4,
                "endLine", 6,
                "newMethodName", "isMissing"));

        assertAll(
                () -> assertFalse(result.isError()),
                () -> assertTrue(result.text().contains("lines"),
                        "response mentions line-count delta so agent can skip re-Read: " + result.text()),
                () -> assertTrue(result.text().contains("->"),
                        "response shows before->after length: " + result.text()),
                () -> assertTrue(result.text().contains("earlier line numbers unchanged"),
                        "agent can safely extract the next (lower-numbered) range: " + result.text()));
    }

    @Test
    void schemaDeclaresAllFourRequiredProperties() {
        final var schema = new ExtractMethodTool().inputSchema();
        final var required = schema.getAsJsonArray("required");
        assertAll(
                () -> assertTrue(required.contains(new com.google.gson.JsonPrimitive("file"))),
                () -> assertTrue(required.contains(new com.google.gson.JsonPrimitive("startLine"))),
                () -> assertTrue(required.contains(new com.google.gson.JsonPrimitive("endLine"))),
                () -> assertTrue(required.contains(new com.google.gson.JsonPrimitive("newMethodName"))));
    }
}
