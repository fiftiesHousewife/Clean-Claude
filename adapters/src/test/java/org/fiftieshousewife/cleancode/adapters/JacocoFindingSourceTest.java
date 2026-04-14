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

class JacocoFindingSourceTest {

    private JacocoFindingSource source;

    @BeforeEach
    void setUp() {
        source = new JacocoFindingSource();
    }

    @Test
    void id_returnsJacoco() {
        assertEquals("jacoco", source.id());
    }

    @Test
    void displayName_returnsHumanReadable() {
        assertEquals("JaCoCo", source.displayName());
    }

    @Test
    void coveredCodes_containsT1T2T8() {
        Set<HeuristicCode> covered = source.coveredCodes();
        assertTrue(covered.containsAll(Set.of(HeuristicCode.T1, HeuristicCode.T2, HeuristicCode.T8)));
    }

    @Test
    void collectFindings_producesProjectLevelT1Finding(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/jacoco/jacocoTestReport.xml",
                "jacoco/test/jacocoTestReport.xml");
        List<Finding> findings = source.collectFindings(ctx);

        List<Finding> t1 = findings.stream()
                .filter(f -> f.code() == HeuristicCode.T1)
                .toList();

        assertEquals(1, t1.size());
        assertNull(t1.getFirst().sourceFile());
    }

    @Test
    void collectFindings_t1SeverityError_whenBelow50Percent(@TempDir Path tempDir) throws Exception {
        // 57/(57+133) = 30% — ERROR
        ProjectContext ctx = contextWithFixture(tempDir, "/jacoco/jacocoTestReport.xml",
                "jacoco/test/jacocoTestReport.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding t1 = findings.stream()
                .filter(f -> f.code() == HeuristicCode.T1)
                .findFirst().orElseThrow();

        assertEquals(Severity.ERROR, t1.severity());
    }

    @Test
    void collectFindings_t1SeverityWarning_whenBetween50And74(@TempDir Path tempDir) throws Exception {
        // 60/(60+40) = 60% — WARNING
        ProjectContext ctx = contextWithFixture(tempDir, "/jacoco/medium-coverage.xml",
                "jacoco/test/jacocoTestReport.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding t1 = findings.stream()
                .filter(f -> f.code() == HeuristicCode.T1)
                .findFirst().orElseThrow();

        assertEquals(Severity.WARNING, t1.severity());
    }

    @Test
    void collectFindings_t1SeverityInfo_when75OrAbove(@TempDir Path tempDir) throws Exception {
        // 95/(95+5) = 95% — INFO
        ProjectContext ctx = contextWithFixture(tempDir, "/jacoco/high-coverage.xml",
                "jacoco/test/jacocoTestReport.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding t1 = findings.stream()
                .filter(f -> f.code() == HeuristicCode.T1)
                .findFirst().orElseThrow();

        assertEquals(Severity.INFO, t1.severity());
    }

    @Test
    void collectFindings_producesT8ForLowCoverageClasses(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/jacoco/jacocoTestReport.xml",
                "jacoco/test/jacocoTestReport.xml");
        List<Finding> findings = source.collectFindings(ctx);

        // Bar has 5/(5+80) = ~6% and 85 total lines — should get T8
        List<Finding> t8 = findings.stream()
                .filter(f -> f.code() == HeuristicCode.T8)
                .toList();

        assertTrue(t8.stream().anyMatch(f -> f.sourceFile().contains("Bar")));
    }

    @Test
    void collectFindings_noT8ForSmallClasses(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/jacoco/jacocoTestReport.xml",
                "jacoco/test/jacocoTestReport.xml");
        List<Finding> findings = source.collectFindings(ctx);

        // Tiny has 5 total lines (3+2) — should NOT get T8
        List<Finding> t8 = findings.stream()
                .filter(f -> f.code() == HeuristicCode.T8)
                .toList();

        assertFalse(t8.stream().anyMatch(f -> f.sourceFile().contains("Tiny")));
    }

    @Test
    void collectFindings_producesT2Error_whenReportMissing(@TempDir Path tempDir) throws Exception {
        // No report file, but has test sources
        Path testSrc = tempDir.resolve("src/test/java");
        Files.createDirectories(testSrc);

        ProjectContext ctx = new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(), List.of(testSrc),
                tempDir.resolve("build"), tempDir.resolve("build/reports"));

        List<Finding> findings = source.collectFindings(ctx);

        Finding t2 = findings.stream()
                .filter(f -> f.code() == HeuristicCode.T2)
                .findFirst().orElseThrow();

        assertEquals(Severity.ERROR, t2.severity());
        assertNull(t2.sourceFile());
    }

    @Test
    void isAvailable_returnsTrueAlways(@TempDir Path tempDir) {
        ProjectContext ctx = new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(), List.of(), tempDir, tempDir.resolve("reports"));

        assertTrue(source.isAvailable(ctx));
    }

    @Test
    void collectFindings_messageIncludesCoveragePercentages(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/jacoco/jacocoTestReport.xml",
                "jacoco/test/jacocoTestReport.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding t1 = findings.stream()
                .filter(f -> f.code() == HeuristicCode.T1)
                .findFirst().orElseThrow();

        assertTrue(t1.message().contains("30%") || t1.message().contains("30.0%"),
                "Expected coverage percentage in message: " + t1.message());
    }

    private ProjectContext contextWithFixture(Path tempDir, String resourcePath, String targetPath)
            throws IOException {
        Path reportsDir = tempDir.resolve("reports");
        Path targetFile = reportsDir.resolve(targetPath);
        Files.createDirectories(targetFile.getParent());

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Test fixture not found: " + resourcePath);
            Files.copy(is, targetFile);
        }

        return new ProjectContext(
                tempDir, "test-project", "1.0", "21",
                List.of(), List.of(Path.of("src/test/java")),
                tempDir.resolve("build"), reportsDir);
    }
}
