package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CheckstyleFindingSourceTest {

    private CheckstyleFindingSource source;

    @BeforeEach
    void setUp() {
        source = new CheckstyleFindingSource();
    }

    @Test
    void id_returnsCheckstyle() {
        assertEquals("checkstyle", source.id());
    }

    @Test
    void displayName_returnsHumanReadable() {
        assertEquals("Checkstyle", source.displayName());
    }

    @Test
    void coveredCodes_containsAllMappedCodes() {
        Set<HeuristicCode> covered = source.coveredCodes();
        assertTrue(covered.containsAll(Set.of(
                HeuristicCode.F1, HeuristicCode.G25, HeuristicCode.N1,
                HeuristicCode.G30, HeuristicCode.J1, HeuristicCode.G12,
                HeuristicCode.J2, HeuristicCode.G8, HeuristicCode.G18,
                HeuristicCode.G28, HeuristicCode.G24, HeuristicCode.G10)));
    }

    @Test
    void collectFindings_parsesErrors(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/main.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);
        assertEquals(4, findings.size());
    }

    @Test
    void collectFindings_mapsParameterNumberToF1(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/main.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding pn = findings.stream()
                .filter(f -> f.ruleRef().equals("ParameterNumber"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.F1, pn.code());
        assertEquals(Confidence.HIGH, pn.confidence());
    }

    @Test
    void collectFindings_mapsMagicNumberToG25(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/main.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding mn = findings.stream()
                .filter(f -> f.ruleRef().equals("MagicNumber"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.G25, mn.code());
        assertEquals(Confidence.HIGH, mn.confidence());
    }

    @Test
    void collectFindings_mapsAvoidStarImportToJ1(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/main.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding asi = findings.stream()
                .filter(f -> f.ruleRef().equals("AvoidStarImport"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.J1, asi.code());
        assertEquals(Confidence.HIGH, asi.confidence());
    }

    @Test
    void collectFindings_mapsLeftCurlyToG24(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/main.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding lc = findings.stream()
                .filter(f -> f.ruleRef().equals("LeftCurly"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.G24, lc.code());
        assertEquals(Confidence.HIGH, lc.confidence());
    }

    @Test
    void collectFindings_setsToolToCheckstyle(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/main.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        assertTrue(findings.stream().allMatch(f -> "checkstyle".equals(f.tool())));
    }

    @Test
    void collectFindings_setsStartAndEndLineFromLineAttribute(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/main.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding pn = findings.stream()
                .filter(f -> f.ruleRef().equals("ParameterNumber"))
                .findFirst().orElseThrow();

        assertEquals(10, pn.startLine());
        assertEquals(10, pn.endLine());
    }

    @Test
    void collectFindings_extractsCheckNameFromSourceFQN(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/main.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        assertTrue(findings.stream().anyMatch(f -> "ParameterNumber".equals(f.ruleRef())));
        assertTrue(findings.stream().anyMatch(f -> "AvoidStarImport".equals(f.ruleRef())));
    }

    @Test
    void collectFindings_emptyReport_returnsNoFindings(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/empty.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);
        assertTrue(findings.isEmpty());
    }

    @Test
    void isAvailable_returnsFalseWhenReportMissing(@TempDir Path tempDir) {
        ProjectContext ctx = new ProjectContext(
                tempDir, "test", "1.0", "21",
                List.of(), List.of(), tempDir, tempDir.resolve("reports"), List.of());

        assertFalse(source.isAvailable(ctx));
    }

    @Test
    void collectFindings_unmappedCheckIsSkipped(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/unknown-check.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);
        assertTrue(findings.isEmpty());
    }

    @Test
    void collectFindings_makesSourceFileRelative(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/checkstyle/main.xml", "checkstyle/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding pn = findings.stream()
                .filter(f -> f.ruleRef().equals("ParameterNumber"))
                .findFirst().orElseThrow();

        assertEquals("src/main/java/com/example/Foo.java", pn.sourceFile());
    }

    private ProjectContext contextWithFixture(Path tempDir, String resourcePath, String targetPath)
            throws IOException {
        return TestContexts.contextWithFixture(getClass(), tempDir, resourcePath, targetPath);
    }
}
