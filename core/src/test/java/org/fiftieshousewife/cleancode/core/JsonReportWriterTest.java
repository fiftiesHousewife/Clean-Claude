package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JsonReportWriterTest {

    @Test
    void writesReportToJsonFile(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("findings.json");
        AggregatedReport report = sampleReport();

        JsonReportWriter.write(report, output);

        assertTrue(Files.exists(output));
        String json = Files.readString(output);
        assertTrue(json.contains("G5"));
        assertTrue(json.contains("Foo.java"));
    }

    @Test
    void jsonContainsAllFindingFields(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("findings.json");
        AggregatedReport report = sampleReport();

        JsonReportWriter.write(report, output);

        String json = Files.readString(output);
        assertTrue(json.contains("\"code\""));
        assertTrue(json.contains("\"sourceFile\""));
        assertTrue(json.contains("\"startLine\""));
        assertTrue(json.contains("\"endLine\""));
        assertTrue(json.contains("\"message\""));
        assertTrue(json.contains("\"severity\""));
        assertTrue(json.contains("\"confidence\""));
        assertTrue(json.contains("\"tool\""));
        assertTrue(json.contains("\"ruleRef\""));
    }

    @Test
    void jsonContainsReportMetadata(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("findings.json");
        AggregatedReport report = sampleReport();

        JsonReportWriter.write(report, output);

        String json = Files.readString(output);
        assertTrue(json.contains("\"projectName\""));
        assertTrue(json.contains("\"projectVersion\""));
        assertTrue(json.contains("\"generatedAt\""));
    }

    @Test
    void roundTrip_producesEquivalentData(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("findings.json");
        AggregatedReport original = sampleReport();

        JsonReportWriter.write(original, output);
        AggregatedReport restored = JsonReportReader.read(output);

        assertEquals(original.findings().size(), restored.findings().size());
        assertEquals(original.projectName(), restored.projectName());
        assertEquals(original.projectVersion(), restored.projectVersion());

        Finding origFirst = original.findings().getFirst();
        Finding restoredFirst = restored.findings().getFirst();
        assertEquals(origFirst.code(), restoredFirst.code());
        assertEquals(origFirst.sourceFile(), restoredFirst.sourceFile());
        assertEquals(origFirst.startLine(), restoredFirst.startLine());
        assertEquals(origFirst.severity(), restoredFirst.severity());
        assertEquals(origFirst.tool(), restoredFirst.tool());
    }

    @Test
    void createsParentDirectories(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("deep/nested/dir/findings.json");
        AggregatedReport report = sampleReport();

        JsonReportWriter.write(report, output);

        assertTrue(Files.exists(output));
    }

    private AggregatedReport sampleReport() {
        Finding f1 = new Finding(HeuristicCode.G5, "Foo.java", 10, 20,
                "duplicated block", Severity.WARNING, Confidence.HIGH, "cpd", "cpd-dup",
                Map.of("otherFile", "Bar.java", "tokens", "150"));
        Finding f2 = Finding.projectLevel(HeuristicCode.T1, "Low coverage",
                Severity.ERROR, Confidence.HIGH, "jacoco", "coverage");

        return new AggregatedReport(
                List.of(f1, f2),
                Set.of(HeuristicCode.G5, HeuristicCode.T1),
                Instant.parse("2026-01-15T10:30:00Z"),
                "test-project", "1.0");
    }
}
