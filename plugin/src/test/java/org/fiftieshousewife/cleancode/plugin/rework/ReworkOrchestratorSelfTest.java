package org.fiftieshousewife.cleancode.plugin.rework;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.AggregatedReport;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check of the rework flow against a frozen copy of one of
 * the project's own large classes ({@code AnalyseTask.java}). Proves
 * that: the orchestrator reads real Java content off disk without
 * choking, the prompt builder can handle a full source file, the
 * suggestion detector matches findings back to the path, and the
 * agent-driven round trip parses a realistic canned response into the
 * commit-message body.
 *
 * <p>The fixture lives in {@code src/test/resources/selftest/} and is a
 * literal copy of the production class at the time this test was added.
 * When the production class changes, the fixture does not — keeping the
 * self-test stable.
 */
class ReworkOrchestratorSelfTest {

    private static final String FIXTURE_RESOURCE = "/selftest/AnalyseTask.java.fixture";
    private static final String RELATIVE_PATH =
            "plugin/src/main/java/org/fiftieshousewife/cleancode/plugin/AnalyseTask.java";

    @TempDir
    Path projectRoot;

    @Test
    void suggestOnlyAgainstFrozenAnalyseTaskProducesFileSpecificSuggestions()
            throws IOException, ReworkOrchestrator.ReworkException {
        final Path file = seedFixture();
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G30, RELATIVE_PATH, 20, 82,
                        "method too long (62 lines)", Severity.WARNING, Confidence.HIGH,
                        "openrewrite", "r"),
                Finding.at(HeuristicCode.Ch10_1, RELATIVE_PATH, 1, 120,
                        "class orchestrates too many steps", Severity.WARNING, Confidence.HIGH,
                        "openrewrite", "r"));

        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(
                (prompt, timeout) -> {
                    throw new AssertionError("SUGGEST_ONLY must not invoke the agent");
                },
                Duration.ofSeconds(1));
        final ReworkReport result = orchestrator.reworkClass(
                file, projectRoot, report, ReworkMode.SUGGEST_ONLY);

        assertAll(
                () -> assertEquals(2, result.suggestions().size(),
                        "both findings on this file surface as suggestions"),
                () -> assertTrue(result.commitMessageBody().contains("G30"),
                        "commit body names the heuristic"),
                () -> assertTrue(result.commitMessageBody().contains("method too long"),
                        "the finding's message is in the body for the reviewer"),
                () -> assertFalse(result.commitMessageBody().contains("## Actions"),
                        "no actions header when the agent hasn't run"));
    }

    @Test
    void agentDrivenRoundTripCapturesActionAndWhyFromTheRealFixture()
            throws IOException, ReworkOrchestrator.ReworkException {
        final Path file = seedFixture();
        final AggregatedReport report = reportWith(
                Finding.at(HeuristicCode.G30, RELATIVE_PATH, 20, 82,
                        "method too long", Severity.WARNING, Confidence.HIGH, "openrewrite", "r"));

        final String[] capturedPrompt = {""};
        final AgentRunner fakeAgent = (prompt, timeout) -> {
            capturedPrompt[0] = prompt;
            return """
                    I looked at analyse() and the content-classification block stood out.
                    ```json
                    {"actions":[{"recipe":"ExtractMethodRecipe",
                                 "options":{"file":"AnalyseTask.java",
                                            "startLine":51,"endLine":60,
                                            "newMethodName":"buildFindingSources"},
                                 "why":"that eight-line list of finding sources is one concept"}],
                     "rejected":[]}
                    ```
                    """;
        };
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(
                fakeAgent, Duration.ofMinutes(1));
        final ReworkReport result = orchestrator.reworkClass(
                file, projectRoot, report, ReworkMode.AGENT_DRIVEN);

        assertAll(
                () -> assertTrue(capturedPrompt[0].contains("class AnalyseTask"),
                        "the full source of the fixture made it into the prompt"),
                () -> assertTrue(capturedPrompt[0].contains("G30 at L20"),
                        "the finding appears in the prompt's findings block"),
                () -> assertEquals(1, result.actionsTaken().size()),
                () -> assertEquals("buildFindingSources",
                        result.actionsTaken().getFirst().options().get("newMethodName")),
                () -> assertTrue(result.commitMessageBody().contains("that eight-line list of finding sources"),
                        "agent's why is preserved into the commit body"));
    }

    private Path seedFixture() throws IOException {
        final Path target = projectRoot.resolve(RELATIVE_PATH);
        Files.createDirectories(target.getParent());
        try (var in = ReworkOrchestratorSelfTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            if (in == null) {
                throw new IOException("missing fixture on classpath: " + FIXTURE_RESOURCE);
            }
            Files.write(target, in.readAllBytes());
        }
        return target;
    }

    private static AggregatedReport reportWith(final Finding... findings) {
        final Set<HeuristicCode> covered = EnumSet.noneOf(HeuristicCode.class);
        for (final Finding f : findings) {
            covered.add(f.code());
        }
        return new AggregatedReport(List.of(findings), covered,
                Instant.now(), "self-test", "0");
    }
}
