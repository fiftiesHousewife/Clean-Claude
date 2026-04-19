package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CpdFindingSourceTest {

    private CpdFindingSource source;

    @BeforeEach
    void setUp() {
        source = new CpdFindingSource();
    }

    @Test
    void id_returnsCpd() {
        assertEquals("cpd", source.id());
    }

    @Test
    void displayName_returnsHumanReadable() {
        assertEquals("CPD", source.displayName());
    }

    @Test
    void coveredCodes_containsOnlyG5() {
        assertEquals(Set.of(HeuristicCode.G5), source.coveredCodes());
    }

    @Test
    void collectFindings_producesOneFindingPerFile(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/cpd/cpd.xml", "cpd/cpd.xml");
        List<Finding> findings = source.collectFindings(ctx);
        // 3 duplications × 2 files each = 6 findings
        assertEquals(6, findings.size());
    }

    @Test
    void collectFindings_allFindingsAreG5(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/cpd/cpd.xml", "cpd/cpd.xml");
        List<Finding> findings = source.collectFindings(ctx);

        assertTrue(findings.stream().allMatch(f -> HeuristicCode.G5 == f.code()));
    }

    @Test
    void collectFindings_severityWarningFor150Tokens(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/cpd/cpd.xml", "cpd/cpd.xml");
        List<Finding> findings = source.collectFindings(ctx);

        List<Finding> fooFindings = findings.stream()
                .filter(f -> f.sourceFile().endsWith("Foo.java"))
                .toList();

        assertFalse(fooFindings.isEmpty());
        assertEquals(Severity.WARNING, fooFindings.getFirst().severity());
    }

    @Test
    void collectFindings_severityWarningForUnder100Tokens(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/cpd/cpd.xml", "cpd/cpd.xml");
        List<Finding> findings = source.collectFindings(ctx);

        List<Finding> bazFindings = findings.stream()
                .filter(f -> f.sourceFile().endsWith("Baz.java"))
                .toList();

        assertFalse(bazFindings.isEmpty());
        assertEquals(Severity.WARNING, bazFindings.getFirst().severity());
    }

    @Test
    void collectFindings_severityErrorFor200PlusTokens(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/cpd/cpd.xml", "cpd/cpd.xml");
        List<Finding> findings = source.collectFindings(ctx);

        List<Finding> bigFindings = findings.stream()
                .filter(f -> f.sourceFile().endsWith("Big.java"))
                .toList();

        assertFalse(bigFindings.isEmpty());
        assertEquals(Severity.ERROR, bigFindings.getFirst().severity());
    }

    @Test
    void collectFindings_metadataContainsOtherFile(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/cpd/cpd.xml", "cpd/cpd.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding fooFinding = findings.stream()
                .filter(f -> f.sourceFile().endsWith("Foo.java"))
                .findFirst().orElseThrow();

        assertTrue(fooFinding.metadata().containsKey("otherFile"));
        assertTrue(fooFinding.metadata().get("otherFile").endsWith("Bar.java"));
    }

    @Test
    void collectFindings_metadataContainsTokenCount(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/cpd/cpd.xml", "cpd/cpd.xml");
        List<Finding> findings = source.collectFindings(ctx);

        Finding fooFinding = findings.stream()
                .filter(f -> f.sourceFile().endsWith("Foo.java"))
                .findFirst().orElseThrow();

        assertEquals("150", fooFinding.metadata().get("tokens"));
    }

    @Test
    void collectFindings_setsToolToCpd(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/cpd/cpd.xml", "cpd/cpd.xml");
        List<Finding> findings = source.collectFindings(ctx);

        assertTrue(findings.stream().allMatch(f -> "cpd".equals(f.tool())));
    }

    @Test
    void collectFindings_emptyReport_returnsNoFindings(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir, "/cpd/empty.xml", "cpd/cpd.xml");
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

    private ProjectContext contextWithFixture(Path tempDir, String resourcePath, String targetPath)
            throws IOException {
        return TestContexts.contextWithFixture(getClass(), tempDir, resourcePath, targetPath);
    }
}
