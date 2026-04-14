package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SurefireFindingSourceTest {

    private SurefireFindingSource source;

    @BeforeEach
    void setUp() {
        source = new SurefireFindingSource();
    }

    @Test
    void id_returnsSurefire() {
        assertEquals("surefire", source.id());
    }

    @Test
    void displayName_returnsHumanReadable() {
        assertEquals("Surefire", source.displayName());
    }

    @Test
    void coveredCodes_containsT3T4T9() {
        Set<HeuristicCode> covered = source.coveredCodes();
        assertTrue(covered.containsAll(Set.of(HeuristicCode.T3, HeuristicCode.T4, HeuristicCode.T9)));
    }

    @Test
    void collectFindings_producesT9ForSlowTests(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixtures(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        // slowTest at 8.2s > 5s threshold
        assertTrue(findings.stream().anyMatch(f ->
                f.code() == HeuristicCode.T9
                        && f.message().contains("slowTest")
                        && f.severity() == Severity.WARNING));
    }

    @Test
    void collectFindings_producesT9ErrorForVerySlowTests(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixtures(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        // verySlowTest at 32s > 30s threshold — project-level ERROR
        assertTrue(findings.stream().anyMatch(f ->
                f.code() == HeuristicCode.T9
                        && f.severity() == Severity.ERROR
                        && f.message().contains("verySlowTest")));
    }

    @Test
    void collectFindings_producesT3ForSkippedTests(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixtures(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        assertTrue(findings.stream().anyMatch(f ->
                (f.code() == HeuristicCode.T3 || f.code() == HeuristicCode.T4)
                        && f.message().contains("skippedTest")));
    }

    @Test
    void collectFindings_noFindingForFastTests(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixtures(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        assertFalse(findings.stream().anyMatch(f -> f.message().contains("fastTest")));
    }

    @Test
    void collectFindings_setsToolToSurefire(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixtures(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        assertTrue(findings.stream().allMatch(f -> "surefire".equals(f.tool())));
    }

    @Test
    void collectFindings_parsesMultipleReportFiles(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixtures(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        // Should have findings from both FooTest and BarTest
        assertTrue(findings.stream().anyMatch(f -> f.message().contains("slowTest")));
        assertTrue(findings.stream().anyMatch(f -> f.message().contains("verySlowTest")));
    }

    @Test
    void collectFindings_messageIncludesExecutionTime(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixtures(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        Finding slow = findings.stream()
                .filter(f -> f.message().contains("slowTest") && f.code() == HeuristicCode.T9)
                .findFirst().orElseThrow();

        assertTrue(slow.message().contains("8.2"), "Expected time in message: " + slow.message());
    }

    @Test
    void isAvailable_returnsFalseWhenNoReportFiles(@TempDir Path tempDir) {
        ProjectContext ctx = new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(), List.of(), tempDir, tempDir.resolve("reports"));

        assertFalse(source.isAvailable(ctx));
    }

    @Test
    void collectFindings_projectLevelWarning_whenHighSkipPercentage(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixtures(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        // 1 skipped out of 5 total = 20% > 10% threshold
        assertTrue(findings.stream().anyMatch(f ->
                (f.code() == HeuristicCode.T3 || f.code() == HeuristicCode.T4)
                        && f.severity() == Severity.WARNING
                        && f.sourceFile() == null));
    }

    private ProjectContext contextWithFixtures(Path tempDir) throws IOException {
        Path reportsDir = tempDir.resolve("reports");
        Path surefireDir = reportsDir.resolve("surefire-reports");
        Files.createDirectories(surefireDir);

        copyFixture("/surefire/TEST-com.example.FooTest.xml",
                surefireDir.resolve("TEST-com.example.FooTest.xml"));
        copyFixture("/surefire/TEST-com.example.BarTest.xml",
                surefireDir.resolve("TEST-com.example.BarTest.xml"));

        return new ProjectContext(
                tempDir, "test-project", "1.0", "21",
                List.of(), List.of(Path.of("src/test/java")),
                tempDir.resolve("build"), reportsDir);
    }

    private void copyFixture(String resourcePath, Path target) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Test fixture not found: " + resourcePath);
            Files.copy(is, target);
        }
    }
}
