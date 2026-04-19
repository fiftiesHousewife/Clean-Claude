package io.github.fiftieshousewife.cleancode.refactoring.extractmethod;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.fiftieshousewife.cleancode.refactoring.support.AstFragments;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
    private final AtomicReference<String> lastRejectionReason = new AtomicReference<>();

    /**
     * Why the last {@link #run} invocation declined to modify the tree, or
     * empty if the recipe extracted successfully or was never called. Used
     * by callers (MCP, CLI) to show the user a specific reason rather than
     * a generic "recipe rejected the range".
     */
    public Optional<String> lastRejectionReason() {
        return Optional.ofNullable(lastRejectionReason.get());
    }

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
                    lastRejectionReason.compareAndSet(null,
                            "source path mismatch: expected " + file + ", got " + cu.getSourcePath());
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

    /**
     * Compute the extracted source by textual splicing — the byte-accurate
     * path used by the MCP tool. Avoids the OpenRewrite AST print-back,
     * which has been observed to mangle multi-line Javadoc under certain
     * shapes (see {@code extractsLines64to67AgainstRealPipelineFixtureOnDisk}).
     * The AST is used only to validate and compute the new call site and
     * helper method text; the original source bytes are preserved verbatim
     * outside the extraction region.
     */
    public Optional<String> extractTextually(final J.CompilationUnit cu, final String cuText,
                                             final Cursor cuCursor) {
        if (!sourcePathMatches(cu)) {
            lastRejectionReason.compareAndSet(null,
                    "source path mismatch: expected " + file + ", got " + cu.getSourcePath());
            return Optional.empty();
        }
        final LineIndex lines = LineIndex.forText(cuText);
        final Optional<ExtractionTarget> maybeTarget =
                ExtractionTarget.findIn(cu, cuText, lines, startLine, endLine);
        if (maybeTarget.isEmpty()) {
            lastRejectionReason.compareAndSet(null, "lines " + startLine + "-" + endLine
                    + " do not align with a contiguous run of top-level statements in a single method body");
            return Optional.empty();
        }
        final ExtractionTarget target = maybeTarget.get();
        final ExtractionAnalysis analysis = ExtractionAnalysis.analyse(target, cuCursor);
        if (!analysis.extractable()) {
            lastRejectionReason.compareAndSet(null, analysis.rejectionReason());
            return Optional.empty();
        }
        return Optional.of(spliceExtraction(cuText, lines, target, analysis));
    }

    private String spliceExtraction(final String source, final LineIndex lines,
                                    final ExtractionTarget target, final ExtractionAnalysis analysis) {
        final String callSite = ExtractionSource.renderCallSite(newMethodName, analysis);
        final String methodAtColumnZero = ExtractionSource.renderExtractedMethod(
                newMethodName, target, analysis).stripTrailing();
        final int rangeLineStart = lines.startOfLine(startLine);
        final int rangeLineEndExclusive = lines.endOfLine(endLine);
        final int bodyCloseLine = lines.lineOf(target.bodyCloseBraceOffset());
        final int afterEnclosingMethod = lines.endOfLine(bodyCloseLine);
        // Body-level indent is the column of the first extracted line; sibling
        // methods sit one level out.
        final String bodyIndent = indentOfLine(source, rangeLineStart);
        final String methodIndent = outdentOneLevel(bodyIndent);
        return new StringBuilder(source.length() + methodAtColumnZero.length() + 32)
                .append(source, 0, rangeLineStart)
                .append(bodyIndent).append(callSite).append('\n')
                .append(source, rangeLineEndExclusive, afterEnclosingMethod)
                .append('\n')
                .append(indentEachLine(methodAtColumnZero, methodIndent, bodyIndent))
                .append('\n')
                .append(source, afterEnclosingMethod, source.length())
                .toString();
    }

    /**
     * Indent the rendered method. {@link ExtractionSource#renderExtractedMethod}
     * returns the signature on the first line and the closing brace on the
     * last line; every other line is body. That's enough to assign indents
     * without inspecting line contents.
     */
    private static String indentEachLine(final String renderedMethod,
                                         final String methodIndent, final String bodyIndent) {
        final String[] methodLines = renderedMethod.split("\n", -1);
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < methodLines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            final String line = methodLines[i];
            if (line.isBlank()) {
                out.append(line);
                continue;
            }
            final boolean isSignatureOrClose = i == 0 || i == methodLines.length - 1;
            out.append(isSignatureOrClose ? methodIndent : bodyIndent).append(line);
        }
        return out.toString();
    }

    private static String indentOfLine(final String source, final int lineStart) {
        int i = lineStart;
        while (i < source.length() && (source.charAt(i) == ' ' || source.charAt(i) == '\t')) {
            i++;
        }
        return source.substring(lineStart, i);
    }

    private static String outdentOneLevel(final String indent) {
        if (indent.length() >= 4 && "    ".equals(indent.substring(indent.length() - 4))) {
            return indent.substring(0, indent.length() - 4);
        }
        return indent;
    }

    private Optional<J.CompilationUnit> attemptExtraction(final J.CompilationUnit cu,
                                                          final org.openrewrite.Cursor cuCursor) {
        final String cuText = cu.printAll();
        final LineIndex lines = LineIndex.forText(cuText);
        final Optional<ExtractionTarget> maybeTarget =
                ExtractionTarget.findIn(cu, cuText, lines, startLine, endLine);
        if (maybeTarget.isEmpty()) {
            lastRejectionReason.compareAndSet(null, "lines " + startLine + "-" + endLine
                    + " do not align with a contiguous run of top-level statements in a single method body");
            return Optional.empty();
        }
        final ExtractionTarget target = maybeTarget.get();
        final ExtractionAnalysis analysis = ExtractionAnalysis.analyse(target, cuCursor);
        if (!analysis.extractable()) {
            lastRejectionReason.compareAndSet(null, analysis.rejectionReason());
            return Optional.empty();
        }
        final Optional<J.MethodDeclaration> newMethod = AstFragments.parseMethod(
                ExtractionSource.renderExtractedMethod(newMethodName, target, analysis));
        final Optional<Statement> callStatement = AstFragments.parseStatement(
                ExtractionSource.renderCallSite(newMethodName, analysis));
        if (newMethod.isEmpty() || callStatement.isEmpty()) {
            lastRejectionReason.compareAndSet(null,
                    "could not reparse the synthesised method or call site — AstFragments.parseMethod/parseStatement returned empty");
            return Optional.empty();
        }
        return Optional.of(ExtractionRewrite.apply(cu, target, newMethod.get(), callStatement.get()));
    }
}
