package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSource;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.fiftieshousewife.cleancode.core.RecipeThresholds;
import org.fiftieshousewife.cleancode.core.Severity;

import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenRewriteFindingSource implements FindingSource {

    private static final String TOOL = "openrewrite";

    private static final Map<HeuristicCode, Severity> DEFAULT_SEVERITY = Map.of(
            HeuristicCode.G4, Severity.ERROR,
            HeuristicCode.Ch7_1, Severity.ERROR,
            HeuristicCode.F2, Severity.ERROR,
            HeuristicCode.G8, Severity.ERROR);

    private static final Set<HeuristicCode> COVERED = Set.of(
            HeuristicCode.F1, HeuristicCode.F2, HeuristicCode.F3,
            HeuristicCode.C3, HeuristicCode.C5,
            HeuristicCode.Ch3_1,
            HeuristicCode.Ch7_1, HeuristicCode.Ch7_2,
            HeuristicCode.Ch10_1, HeuristicCode.Ch10_2,
            HeuristicCode.G4, HeuristicCode.G8,
            HeuristicCode.G10, HeuristicCode.G11, HeuristicCode.G14,
            HeuristicCode.G16, HeuristicCode.G19,
            HeuristicCode.G23, HeuristicCode.G25, HeuristicCode.G26,
            HeuristicCode.G28, HeuristicCode.G29,
            HeuristicCode.G30, HeuristicCode.G33, HeuristicCode.G34, HeuristicCode.G36,
            HeuristicCode.J2, HeuristicCode.J3,
            HeuristicCode.N1, HeuristicCode.N5, HeuristicCode.N6, HeuristicCode.N7,
            HeuristicCode.T1, HeuristicCode.T3, HeuristicCode.T4);

    private final OpenRewriteRecipeEngine engine;

    public OpenRewriteFindingSource() {
        this(RecipeThresholds.defaults());
    }

    public OpenRewriteFindingSource(final RecipeThresholds thresholds) {
        this.engine = new OpenRewriteRecipeEngine(thresholds);
    }

    static Severity severityFor(final HeuristicCode code) {
        return DEFAULT_SEVERITY.getOrDefault(code, Severity.WARNING);
    }

    @Override
    public String id() {
        return TOOL;
    }

    @Override
    public String displayName() {
        return "OpenRewrite";
    }

    @Override
    public Set<HeuristicCode> coveredCodes() {
        return COVERED;
    }

    @Override
    public List<Finding> collectFindings(final ProjectContext context) throws FindingSourceException {
        final List<Path> javaFiles = engine.collectSourceFiles(context);
        if (javaFiles.isEmpty()) {
            return List.of();
        }

        final List<SourceFile> parsed = engine.parseSourceFiles(javaFiles);
        final List<ScanningRecipe<?>> recipes = engine.createRecipes();
        engine.runAll(parsed, recipes);

        return OpenRewriteFindingMapper.forSourceFiles(parsed).extractFindings(recipes);
    }
}
