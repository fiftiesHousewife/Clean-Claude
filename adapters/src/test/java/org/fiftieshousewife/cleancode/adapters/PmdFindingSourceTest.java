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

class PmdFindingSourceTest {

    private PmdFindingSource source;

    @BeforeEach
    void setUp() {
        source = new PmdFindingSource();
    }

    @Test
    void id_returnsPmd() {
        assertEquals("pmd", source.id());
    }

    @Test
    void displayName_returnsHumanReadable() {
        assertEquals("PMD", source.displayName());
    }

    @Test
    void coveredCodes_containsAllMappedCodes() {
        Set<HeuristicCode> covered = source.coveredCodes();
        assertTrue(covered.containsAll(Set.of(
                HeuristicCode.G30, HeuristicCode.F4, HeuristicCode.G9,
                HeuristicCode.G12, HeuristicCode.G8, HeuristicCode.C5,
                HeuristicCode.G4, HeuristicCode.G23, HeuristicCode.G17,
                HeuristicCode.J2)));
    }

    @Test
    void collectFindings_parsesViolations(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/pmd/main.xml", "pmd/main.xml");
        List<Finding> findings = source.collectFindings(ctx);
        assertEquals(4, findings.size());
    }

    @Test
    void collectFindings_mapsCyclomaticComplexityToG30(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/pmd/main.xml", "pmd/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding cc = findings.stream()
                .filter(f -> f.ruleRef().equals("CyclomaticComplexity"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.G30, cc.code());
        assertEquals(Severity.WARNING, cc.severity());
        assertEquals(Confidence.MEDIUM, cc.confidence());
    }

    @Test
    void collectFindings_mapsUnusedLocalVariableToG9(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/pmd/main.xml", "pmd/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding ulv = findings.stream()
                .filter(f -> f.ruleRef().equals("UnusedLocalVariable"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.G9, ulv.code());
        assertEquals(Severity.INFO, ulv.severity());
        assertEquals(Confidence.HIGH, ulv.confidence());
    }

    @Test
    void collectFindings_mapsEmptyCatchBlockToG4(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/pmd/main.xml", "pmd/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding ecb = findings.stream()
                .filter(f -> f.ruleRef().equals("EmptyCatchBlock"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.G4, ecb.code());
        assertEquals(Severity.ERROR, ecb.severity());
        assertEquals(Confidence.HIGH, ecb.confidence());
    }

    @Test
    void collectFindings_mapsCommentedOutCodeToC5(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/pmd/main.xml", "pmd/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding coc = findings.stream()
                .filter(f -> f.ruleRef().equals("CommentedOutCodeLine"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.C5, coc.code());
        assertEquals(Severity.WARNING, coc.severity());
        assertEquals(Confidence.HIGH, coc.confidence());
    }

    @Test
    void collectFindings_setsToolToPmd(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/pmd/main.xml", "pmd/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        assertTrue(findings.stream().allMatch(f -> "pmd".equals(f.tool())));
    }

    @Test
    void collectFindings_preservesLineNumbers(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/pmd/main.xml", "pmd/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding cc = findings.stream()
                .filter(f -> f.ruleRef().equals("CyclomaticComplexity"))
                .findFirst().orElseThrow();

        assertEquals(15, cc.startLine());
        assertEquals(25, cc.endLine());
    }

    @Test
    void collectFindings_makesSourceFileRelative(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/pmd/main.xml", "pmd/main.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding cc = findings.stream()
                .filter(f -> f.ruleRef().equals("CyclomaticComplexity"))
                .findFirst().orElseThrow();

        assertEquals("src/main/java/com/example/Foo.java", cc.sourceFile());
    }

    @Test
    void collectFindings_emptyReport_returnsNoFindings(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/pmd/empty.xml", "pmd/main.xml");
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
    void collectFindings_unknownRuleIsSkipped(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/pmd/unknown-rule.xml", "pmd/main.xml");
        List<Finding> findings = source.collectFindings(ctx);
        assertTrue(findings.isEmpty());
    }

    private ProjectContext contextWithFixture(Path tempDir, String resourcePath, String targetPath)
            throws IOException {
        return TestContexts.contextWithFixture(getClass(), tempDir, resourcePath, targetPath);
    }
}
