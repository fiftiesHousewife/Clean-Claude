package io.github.fiftieshousewife.cleancode.plugin.rework;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.AggregatedReport;
import io.github.fiftieshousewife.cleancode.core.Confidence;
import io.github.fiftieshousewife.cleancode.core.Finding;
import io.github.fiftieshousewife.cleancode.core.Severity;
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
            assertTrue(prompt.contains("Read each file with your Read tool"),
                    "prompt tells the agent to Read rather than embedding contents");
            return AgentResult.textOnly(cannedResponse);
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
    void harnessVariantUsesPostRecipeFindingsInPromptNotBaseline()
            throws ReworkOrchestrator.ReworkException, IOException {
        final Path file = seedFile("Foo.java", "class Foo { void x() {} }");
        final AggregatedReport baseline = reportWith(
                findingOn("Foo.java", HeuristicCode.G18, 1, "make static (baseline)"),
                findingOn("Foo.java", HeuristicCode.F2, 5, "output arg (baseline)"));
        final AggregatedReport afterRecipes = reportWith(
                findingOn("Foo.java", HeuristicCode.F2, 5, "output arg (still residual)"));
        final String[] capturedPrompt = {null};
        final AgentRunner capturing = (prompt, timeout) -> {
            capturedPrompt[0] = prompt;
            return AgentResult.textOnly("{\"actions\":[],\"rejected\":[]}");
        };
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(capturing, Duration.ofSeconds(1));

        orchestrator.reworkClasses(List.of(file), projectRoot, baseline,
                ReworkMode.AGENT_DRIVEN, RunVariant.HARNESS_RECIPES_THEN_AGENT,
                ReworkOrchestrator.Options.withFeedbackLoop(() -> afterRecipes, 0, projectRoot));

        assertAll(
                () -> assertTrue(capturedPrompt[0].contains("output arg (still residual)"),
                        "post-recipe residual findings reach the agent"),
                () -> assertFalse(capturedPrompt[0].contains("make static (baseline)"),
                        "baseline findings the recipes fixed must NOT reach the agent"));
    }

    @Test
    void nonHarnessVariantsIgnorePostRecipeAnalyser()
            throws ReworkOrchestrator.ReworkException, IOException {
        final Path file = seedFile("Foo.java", "class Foo {}");
        final AggregatedReport baseline = reportWith(
                findingOn("Foo.java", HeuristicCode.G18, 1, "make static (baseline)"));
        final String[] capturedPrompt = {null};
        final AgentRunner capturing = (prompt, timeout) -> {
            capturedPrompt[0] = prompt;
            return AgentResult.textOnly("{\"actions\":[],\"rejected\":[]}");
        };
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(capturing, Duration.ofSeconds(1));

        orchestrator.reworkClasses(List.of(file), projectRoot, baseline,
                ReworkMode.AGENT_DRIVEN, RunVariant.MCP_RECIPES,
                ReworkOrchestrator.Options.withFeedbackLoop(() -> {
                    throw new AssertionError("supplier must not be invoked for MCP_RECIPES pre-agent");
                }, 0, projectRoot));

        assertTrue(capturedPrompt[0].contains("make static (baseline)"),
                "non-harness variants use the baseline findings unchanged");
    }

    @Test
    void feedbackLoopHandsIntroducedFindingsBackToAgent()
            throws ReworkOrchestrator.ReworkException, IOException {
        final Path file = seedFile("Foo.java", "class Foo {}");
        final AggregatedReport baseline = reportWith(
                findingOn("Foo.java", HeuristicCode.G18, 1, "pre-agent finding"));
        final AggregatedReport afterAgent = reportWith(
                findingOn("Foo.java", HeuristicCode.G18, 1, "pre-agent finding"),
                findingOn("Foo.java", HeuristicCode.Ch7_1, 5, "agent introduced a catch-log-continue"));
        final List<String> prompts = new java.util.ArrayList<>();
        final AgentRunner capturing = (prompt, timeout) -> {
            prompts.add(prompt);
            return AgentResult.textOnly("{\"actions\":[],\"rejected\":[]}");
        };
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(capturing, Duration.ofSeconds(1));

        orchestrator.reworkClasses(List.of(file), projectRoot, baseline,
                ReworkMode.AGENT_DRIVEN, RunVariant.VANILLA,
                ReworkOrchestrator.Options.withFeedbackLoop(() -> afterAgent, 1, projectRoot));

        assertAll(
                () -> assertEquals(2, prompts.size(), "one initial call plus one retry"),
                () -> assertTrue(prompts.get(1).contains("RETRY PASS"),
                        "retry prompt carries the retry banner"),
                () -> assertTrue(prompts.get(1).contains("agent introduced a catch-log-continue"),
                        "retry prompt lists the introduced finding"),
                () -> assertFalse(prompts.get(1).contains("pre-agent finding"),
                        "retry prompt does NOT re-list findings that existed before the agent ran"));
    }

    @Test
    void feedbackLoopStopsEarlyWhenNoIntroducedFindings()
            throws ReworkOrchestrator.ReworkException, IOException {
        final Path file = seedFile("Foo.java", "class Foo {}");
        final AggregatedReport baseline = reportWith(
                findingOn("Foo.java", HeuristicCode.G18, 1, "pre-agent finding"));
        final int[] invocations = {0};
        final AgentRunner counting = (prompt, timeout) -> {
            invocations[0]++;
            return AgentResult.textOnly("{\"actions\":[],\"rejected\":[]}");
        };
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(counting, Duration.ofSeconds(1));

        orchestrator.reworkClasses(List.of(file), projectRoot, baseline,
                ReworkMode.AGENT_DRIVEN, RunVariant.VANILLA,
                ReworkOrchestrator.Options.withFeedbackLoop(() -> baseline, 3, projectRoot));

        assertEquals(1, invocations[0], "no retry when re-analysis finds no introduced findings");
    }

    @Test
    void harnessRetryReinvokesRecipePassOnTheAgentsOutput()
            throws ReworkOrchestrator.ReworkException, IOException {
        // Seed a file whose shape will ALSO match a recipe after the retry boundary.
        // MakeMethodStaticRecipe is deterministic and fires whenever a non-static
        // method with no instance-state references exists.
        final Path file = seedFile("com/example/Foo.java", """
                package com.example;
                public final class Foo {
                    public int add(final int a, final int b) {
                        final int s = a + b;
                        final int d = s * 2;
                        return d;
                    }
                }
                """);
        final AggregatedReport baseline = reportWith(
                findingOn("com/example/Foo.java", HeuristicCode.G18, 1, "add is static-able"));
        final AggregatedReport postFirstPass = reportWith(
                findingOn("com/example/Foo.java", HeuristicCode.G29, 1,
                        "residual introduced by agent"));
        final AgentRunner noopAgent = (prompt, timeout) ->
                AgentResult.textOnly("{\"actions\":[],\"rejected\":[]}");
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(noopAgent, Duration.ofSeconds(1));

        final ReworkReport report = orchestrator.reworkClasses(List.of(file), projectRoot, baseline,
                ReworkMode.AGENT_DRIVEN, RunVariant.HARNESS_RECIPES_THEN_AGENT,
                ReworkOrchestrator.Options.withFeedbackLoop(() -> postFirstPass, 1, projectRoot));

        final long harnessPassCount = report.actionsTaken().stream()
                .filter(a -> "HarnessRecipePass".equals(a.recipe()))
                .count();
        assertTrue(harnessPassCount >= 1,
                "main pass must record at least one HarnessRecipePass action; got "
                        + report.actionsTaken());
        assertTrue(Files.readString(file).contains("static"),
                "MakeMethodStaticRecipe should have made `add` static during the pass");
    }

    @Test
    void recipesOnlyVariantSkipsTheAgentEntirely()
            throws ReworkOrchestrator.ReworkException, IOException {
        final Path file = seedFile("Foo.java", "class Foo {}");
        final AgentRunner mustNotRun = (prompt, timeout) -> {
            throw new AssertionError("RECIPES_ONLY must not invoke the agent");
        };
        final ReworkOrchestrator orchestrator = new ReworkOrchestrator(mustNotRun, Duration.ofSeconds(1));

        final ReworkReport report = orchestrator.reworkClasses(List.of(file), projectRoot,
                reportWith(findingOn("Foo.java", HeuristicCode.G18, 1, "make static")),
                ReworkMode.AGENT_DRIVEN, RunVariant.RECIPES_ONLY);

        assertAll(
                () -> assertEquals(ReworkMode.AGENT_DRIVEN, report.mode()),
                () -> assertTrue(report.usage().isEmpty(),
                        "RECIPES_ONLY has no agent invocation, so no usage to record"),
                () -> assertTrue(report.rejected().isEmpty(),
                        "RECIPES_ONLY cannot reject findings — the recipe either fires or doesn't"));
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
