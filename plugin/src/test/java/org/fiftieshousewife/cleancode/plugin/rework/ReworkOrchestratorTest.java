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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReworkOrchestratorTest {

    @TempDir
    Path projectRoot;

    @Test
    void suggestOnlyModeReturnsDetectorOutputWithoutCallingAgent()
            throws ReworkOrchestrator.ReworkException, IOException {
        final Path file = seedFile("src/main/java/Foo.java", "class Foo {}");
        final AgentRunner mustNotRun = (prompt, timeout) -> {
            throw new AssertionError("SUGGEST_ONLY must not invoke the agent");
        };
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(mustNotRun, Duration.ofSeconds(1));

        final ReworkReport report = orchestrator.reworkClass(file, projectRoot,
                reportWith(findingOn("src/main/java/Foo.java", HeuristicCode.G30, 10, "long")),
                ReworkMode.SUGGEST_ONLY);

        assertAll(
                () -> assertEquals(ReworkMode.SUGGEST_ONLY, report.mode()),
                () -> assertEquals(1, report.suggestions().size()),
                () -> assertTrue(report.actionsTaken().isEmpty()),
                () -> assertTrue(report.commitMessageBody().contains("## Suggestions")));
    }

    @Test
    void agentDrivenModePipesPromptToRunnerAndParsesActionsBack()
            throws ReworkOrchestrator.ReworkException, IOException {
        final Path file = seedFile("Foo.java", "class Foo { void x() {} }");
        final String cannedResponse = """
                {"actions":[{"recipe":"ExtractMethodRecipe",
                             "options":{"file":"Foo.java","startLine":1,"endLine":1,"newMethodName":"split"},
                             "why":"demonstrates the round trip"}],
                 "rejected":[]}
                """;
        final AgentRunner canned = (prompt, timeout) -> {
            assertTrue(prompt.contains("Foo.java"), "prompt carries the relative file path");
            assertTrue(prompt.contains("class Foo"), "prompt includes the file contents");
            return cannedResponse;
        };
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(canned, Duration.ofSeconds(1));

        final ReworkReport report = orchestrator.reworkClass(file, projectRoot,
                reportWith(findingOn("Foo.java", HeuristicCode.G30, 1, "long")),
                ReworkMode.AGENT_DRIVEN);

        assertAll(
                () -> assertEquals(1, report.actionsTaken().size()),
                () -> assertEquals("ExtractMethodRecipe", report.actionsTaken().getFirst().recipe()),
                () -> assertTrue(report.commitMessageBody().contains("demonstrates the round trip"),
                        "agent's why is echoed into the commit message body"));
    }

    @Test
    void agentFailureSurfacesAsReworkException() throws IOException {
        final Path file = seedFile("Foo.java", "class Foo {}");
        final AgentRunner failing = (prompt, timeout) -> {
            throw new AgentRunner.AgentRunnerException("claude not installed");
        };
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(failing, Duration.ofSeconds(1));

        final ReworkOrchestrator.ReworkException thrown =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ReworkOrchestrator.ReworkException.class,
                        () -> orchestrator.reworkClass(file, projectRoot,
                                emptyReport(), ReworkMode.AGENT_DRIVEN));
        assertTrue(thrown.getMessage().contains("claude not installed"),
                "the underlying cause is preserved for the caller to surface");
    }

    private Path seedFile(final String relative, final String contents) throws IOException {
        final Path absolute = projectRoot.resolve(relative);
        Files.createDirectories(absolute.getParent() == null ? projectRoot : absolute.getParent());
        Files.writeString(absolute, contents);
        return absolute;
    }

    private static Finding findingOn(final String path, final HeuristicCode code,
                                     final int line, final String message) {
        return Finding.at(code, path, line, line, message,
                Severity.WARNING, Confidence.HIGH, "test", "r");
    }

    private static AggregatedReport emptyReport() {
        return new AggregatedReport(List.of(), EnumSet.noneOf(HeuristicCode.class),
                Instant.now(), "test", "0");
    }

    private static AggregatedReport reportWith(final Finding... findings) {
        final Set<HeuristicCode> covered = EnumSet.noneOf(HeuristicCode.class);
        for (final Finding f : findings) {
            covered.add(f.code());
        }
        return new AggregatedReport(List.of(findings), covered,
                Instant.now(), "test", "0");
    }
}
