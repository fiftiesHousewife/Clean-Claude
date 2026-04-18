package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Optional;

/**
 * First-cut port of IntelliJ IDEA Community's {@code ExtractMethodProcessor}
 * (java/java-impl-refactorings/src/com/intellij/refactoring/extractMethod).
 *
 * <p>The upstream processor builds a {@code ControlFlowWrapper} over the
 * selected elements, classifies each free variable as input or output,
 * collects exit statements that escape the selection, and synthesises a
 * method with the inferred signature. One conditional exit is supported;
 * anything more complex raises {@code PrepareFailedException}.
 *
 * <p>This port keeps the algorithmic core but accepts a
 * {@code (file, startLine, endLine, newMethodName)} tuple and extracts only
 * when all of the following hold:
 * <ul>
 *   <li>The line range covers a contiguous run of top-level statements in
 *       a single method body.</li>
 *   <li>The range contains no {@code return}/{@code break}/{@code continue}/
 *       {@code throw} keyword that would escape.</li>
 *   <li>No local declared before the range is reassigned inside it (Java has
 *       no pass-by-reference for locals).</li>
 *   <li>At most one local declared in the range is used after it; that local
 *       becomes the return value.</li>
 * </ul>
 * When any precondition fails the recipe leaves the tree unchanged and the
 * caller falls back to the skill-guided manual extraction path.
 */
public class ExtractMethodRecipe extends Recipe {

    private final String file;
    private final int startLine;
    private final int endLine;
    private final String newMethodName;

    @JsonCreator
    public ExtractMethodRecipe(
            @JsonProperty("file") final String file,
            @JsonProperty("startLine") final int startLine,
            @JsonProperty("endLine") final int endLine,
            @JsonProperty("newMethodName") final String newMethodName) {
        this.file = file;
        this.startLine = startLine;
        this.endLine = endLine;
        this.newMethodName = newMethodName;
    }

    @Override
    public String getDisplayName() {
        return "Extract a method from a line range";
    }

    @Override
    public String getDescription() {
        return "Extracts the contiguous top-level statements spanned by the given line range "
                + "into a new private method (or static method when the enclosing one is static). "
                + "Ported from IntelliJ IDEA Community's ExtractMethodProcessor: supports zero or "
                + "one output variable, rejects selections whose control flow escapes the range, "
                + "and leaves the tree unchanged when the selection is not cleanly extractable. "
                + "Fixes G30 / Ch10.1 when paired with a heuristic-chosen range.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(final J.CompilationUnit cu,
                                                          final ExecutionContext ctx) {
                if (!sourcePathMatches(cu)) {
                    return cu;
                }
                return attemptExtraction(cu, getCursor()).orElse(cu);
            }
        };
    }

    private boolean sourcePathMatches(final J.CompilationUnit cu) {
        final String path = cu.getSourcePath() == null ? "" : cu.getSourcePath().toString();
        return path.equals(file) || path.endsWith("/" + file) || path.endsWith(file);
    }

    private Optional<J.CompilationUnit> attemptExtraction(final J.CompilationUnit cu,
                                                          final org.openrewrite.Cursor cuCursor) {
        final String cuText = cu.printAll();
        final LineIndex lines = LineIndex.forText(cuText);
        final Optional<ExtractionTarget> maybeTarget =
                ExtractionTarget.findIn(cu, cuText, lines, startLine, endLine);
        if (maybeTarget.isEmpty()) {
            return Optional.empty();
        }
        final ExtractionTarget target = maybeTarget.get();
        final ExtractionAnalysis analysis = ExtractionAnalysis.analyse(target, cuCursor);
        if (!analysis.extractable()) {
            return Optional.empty();
        }
        final Optional<J.MethodDeclaration> newMethod = AstFragments.parseMethod(
                ExtractionSource.renderExtractedMethod(newMethodName, target, analysis));
        final Optional<Statement> callStatement = AstFragments.parseStatement(
                ExtractionSource.renderCallSite(newMethodName, analysis));
        if (newMethod.isEmpty() || callStatement.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ExtractionRewrite.apply(cu, target, newMethod.get(), callStatement.get()));
    }
}
