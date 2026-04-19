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

class SpotBugsFindingSourceTest {

    private SpotBugsFindingSource source;

    @BeforeEach
    void setUp() {
        source = new SpotBugsFindingSource();
    }

    @Test
    void id_returnsSpotbugs() {
        assertEquals("spotbugs", source.id());
    }

    @Test
    void displayName_returnsHumanReadable() {
        assertEquals("SpotBugs", source.displayName());
    }

    @Test
    void coveredCodes_containsExpectedCodes() {
        Set<HeuristicCode> covered = source.coveredCodes();
        assertTrue(covered.containsAll(Set.of(
                HeuristicCode.G4, HeuristicCode.G18, HeuristicCode.G9,
                HeuristicCode.G26, HeuristicCode.G8)));
    }

    @Test
    void collectFindings_mapsNullPathToCh7_2(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        Finding np = findings.stream()
                .filter(f -> f.ruleRef().equals("NP_NULL_ON_SOME_PATH"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.Ch7_2, np.code());
        assertEquals(Severity.ERROR, np.severity());
        assertEquals(Confidence.HIGH, np.confidence());
    }

    @Test
    void collectFindings_mapsBadPracticeDeMightIgnoreToG4(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        Finding de = findings.stream()
                .filter(f -> f.ruleRef().equals("DE_MIGHT_IGNORE"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.G4, de.code());
        assertEquals(Severity.ERROR, de.severity());
    }

    @Test
    void collectFindings_mapsStaticWriteToG18(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        Finding st = findings.stream()
                .filter(f -> f.ruleRef().equals("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.G18, st.code());
        assertEquals(Severity.WARNING, st.severity());
    }

    @Test
    void collectFindings_mapsUnreadFieldToG9(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        Finding urf = findings.stream()
                .filter(f -> f.ruleRef().equals("URF_UNREAD_FIELD"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.G9, urf.code());
        assertEquals(Severity.INFO, urf.severity());
    }

    @Test
    void collectFindings_mapsBoxedPrimitiveToG26(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        Finding dm = findings.stream()
                .filter(f -> f.ruleRef().equals("DM_BOXED_PRIMITIVE_FOR_COMPARE"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.G26, dm.code());
        assertEquals(Severity.INFO, dm.severity());
    }

    @Test
    void collectFindings_mapsMutableArrayToG8(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        Finding ms = findings.stream()
                .filter(f -> f.ruleRef().equals("MS_MUTABLE_ARRAY"))
                .findFirst().orElseThrow();

        assertEquals(HeuristicCode.G8, ms.code());
        assertEquals(Severity.WARNING, ms.severity());
    }

    @Test
    void collectFindings_unknownCategoryTypeSkipped(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        assertFalse(findings.stream().anyMatch(f -> f.ruleRef().equals("UNKNOWN_TYPE")));
    }

    @Test
    void collectFindings_setsToolToSpotbugs(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        assertTrue(findings.stream().allMatch(f -> "spotbugs".equals(f.tool())));
    }

    @Test
    void collectFindings_preservesLineNumbers(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        Finding np = findings.stream()
                .filter(f -> f.ruleRef().equals("NP_NULL_ON_SOME_PATH"))
                .findFirst().orElseThrow();

        assertEquals(15, np.startLine());
        assertEquals(15, np.endLine());
    }

    @Test
    void collectFindings_setsSourceFileFromSourcePath(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = contextWithFixture(tempDir);
        List<Finding> findings = source.collectFindings(ctx);

        Finding np = findings.stream()
                .filter(f -> f.ruleRef().equals("NP_NULL_ON_SOME_PATH"))
                .findFirst().orElseThrow();

        assertEquals("com/example/Foo.java", np.sourceFile());
    }

    @Test
    void collectFindings_emptyReport_returnsNoFindings(@TempDir Path tempDir) throws Exception {
        ProjectContext ctx = TestContexts.contextWithFixture(getClass(), tempDir,
                "/spotbugs/empty.xml", "spotbugs/main.xml");
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

    private ProjectContext contextWithFixture(Path tempDir) throws IOException {
        return TestContexts.contextWithFixture(getClass(), tempDir,
                "/spotbugs/main.xml", "spotbugs/main.xml");
    }
}
