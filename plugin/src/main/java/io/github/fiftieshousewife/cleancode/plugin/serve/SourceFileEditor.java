package io.github.fiftieshousewife.cleancode.plugin.serve;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Inserts {@code @SuppressWarnings("CleanCode:CODE")} annotations into
 * a Java source file at a specified line.
 *
 * <p>Strategy: walk the parsed AST to find the smallest declaration
 * (method / constructor / record / class) whose source range contains
 * the target line, then operate on its source range as text. This
 * preserves the user's exact formatting everywhere outside the
 * insertion point — pretty-printing the AST would not.
 *
 * <p>If the declaration already has {@code @SuppressWarnings},
 * the existing annotation is rewritten to include the new code
 * (idempotent). Otherwise a new annotation is inserted on its own line.
 *
 * <p>A {@code // CleanCode-suppress CODE: <reason>} comment is
 * always inserted on the line above the annotation so the rationale
 * is human-readable next to the suppression.
 */
public final class SourceFileEditor {

    private final Path javaFile;
    private List<String> lines;
    private boolean dirty;

    public SourceFileEditor(final Path javaFile) throws IOException {
        this.javaFile = javaFile;
        this.lines = new ArrayList<>(Files.readAllLines(javaFile));
    }

    public Result suppressFinding(final int line, final String code, final String reason) throws IOException {
        final ParseResult<CompilationUnit> parse = new JavaParser().parse(String.join("\n", lines));
        if (!parse.isSuccessful() || parse.getResult().isEmpty()) {
            return Result.failed("could not parse " + javaFile.getFileName());
        }
        final CompilationUnit cu = parse.getResult().get();
        final Optional<BodyDeclaration<?>> target = findEnclosingDeclaration(cu, line);
        if (target.isEmpty()) {
            return Result.failed("no enclosing declaration found at line " + line + " in " + javaFile.getFileName());
        }
        final BodyDeclaration<?> decl = target.get();

        final int declStartLine = decl.getRange().orElseThrow().begin.line;
        final int annotationStartLine = annotationsBeginLine(decl, declStartLine);
        final int insertionLine = annotationStartLine - 1;
        final String existingIndent = leadingWhitespace(lines.get(annotationStartLine - 1));

        if (mergeIntoExistingAnnotation(decl, code, annotationStartLine - 1)) {
            lines.add(insertionLine, existingIndent + "// CleanCode-suppress " + code + ": " + reason);
            dirty = true;
            return Result.ok();
        }

        lines.add(insertionLine, existingIndent + "@SuppressWarnings(\"CleanCode:" + code + "\")");
        lines.add(insertionLine, existingIndent + "// CleanCode-suppress " + code + ": " + reason);
        dirty = true;
        return Result.ok();
    }

    public boolean dirty() {
        return dirty;
    }

    public void save() throws IOException {
        if (dirty) {
            Files.write(javaFile, lines);
        }
    }

    public List<String> currentLines() {
        return List.copyOf(lines);
    }

    private Optional<BodyDeclaration<?>> findEnclosingDeclaration(final CompilationUnit cu, final int line) {
        BodyDeclaration<?> bestMatch = null;
        int bestSize = Integer.MAX_VALUE;

        for (final Node node : cu.findAll(MethodDeclaration.class)) {
            bestMatch = chooseSmallest(line, (BodyDeclaration<?>) node, bestMatch);
            bestSize = sizeOrMax(bestMatch);
        }
        for (final Node node : cu.findAll(ConstructorDeclaration.class)) {
            bestMatch = chooseSmallest(line, (BodyDeclaration<?>) node, bestMatch);
        }
        for (final Node node : cu.findAll(RecordDeclaration.class)) {
            bestMatch = chooseSmallest(line, (BodyDeclaration<?>) node, bestMatch);
        }
        for (final Node node : cu.findAll(EnumDeclaration.class)) {
            bestMatch = chooseSmallest(line, (BodyDeclaration<?>) node, bestMatch);
        }
        for (final Node node : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            bestMatch = chooseSmallest(line, (BodyDeclaration<?>) node, bestMatch);
        }
        for (final Node node : cu.findAll(AnnotationDeclaration.class)) {
            bestMatch = chooseSmallest(line, (BodyDeclaration<?>) node, bestMatch);
        }
        return Optional.ofNullable(bestMatch);
    }

    private static int sizeOrMax(final BodyDeclaration<?> decl) {
        return decl == null ? Integer.MAX_VALUE
                : decl.getRange().map(r -> r.end.line - r.begin.line).orElse(Integer.MAX_VALUE);
    }

    private static BodyDeclaration<?> chooseSmallest(final int targetLine, final BodyDeclaration<?> candidate,
                                                      final BodyDeclaration<?> best) {
        if (candidate.getRange().isEmpty()) {
            return best;
        }
        final var range = candidate.getRange().get();
        if (targetLine < range.begin.line || targetLine > range.end.line) {
            return best;
        }
        final int size = range.end.line - range.begin.line;
        return best == null || size < sizeOrMax(best) ? candidate : best;
    }

    private static int annotationsBeginLine(final BodyDeclaration<?> decl, final int declStartLine) {
        if (!(decl instanceof NodeWithAnnotations<?> annotated)) {
            return declStartLine;
        }
        int earliest = declStartLine;
        for (final AnnotationExpr ann : annotated.getAnnotations()) {
            if (ann.getRange().isPresent()) {
                earliest = Math.min(earliest, ann.getRange().get().begin.line);
            }
        }
        return earliest;
    }

    private boolean mergeIntoExistingAnnotation(final BodyDeclaration<?> decl, final String code,
                                                  final int firstAnnotationLineIdx) {
        if (!(decl instanceof NodeWithAnnotations<?> annotated)) {
            return false;
        }
        final NodeList<AnnotationExpr> annotations = annotated.getAnnotations();
        for (final AnnotationExpr ann : annotations) {
            final String name = ann.getNameAsString();
            if (!"SuppressWarnings".equals(name) && !"java.lang.SuppressWarnings".equals(name)) {
                continue;
            }
            final int lineIdx = ann.getRange().orElseThrow().begin.line - 1;
            final String original = lines.get(lineIdx);
            final String newCode = "\"CleanCode:" + code + "\"";
            if (original.contains(newCode)) {
                return true;
            }
            final String rewritten = rewriteSuppressWarnings(original, newCode);
            if (rewritten == null) {
                return false;
            }
            lines.set(lineIdx, rewritten);
            return true;
        }
        return false;
    }

    private static String rewriteSuppressWarnings(final String line, final String newCode) {
        final int singleStart = line.indexOf("@SuppressWarnings(\"");
        if (singleStart >= 0) {
            final int valueStart = singleStart + "@SuppressWarnings(".length();
            final int closeParen = line.indexOf(')', valueStart);
            if (closeParen < 0) {
                return null;
            }
            final String existing = line.substring(valueStart, closeParen).trim();
            return line.substring(0, valueStart) + "{" + existing + ", " + newCode + "}"
                    + line.substring(closeParen);
        }
        final int braceStart = line.indexOf("@SuppressWarnings({");
        if (braceStart >= 0) {
            final int closeBrace = line.indexOf('}', braceStart);
            if (closeBrace < 0) {
                return null;
            }
            return line.substring(0, closeBrace) + ", " + newCode + line.substring(closeBrace);
        }
        return null;
    }

    private static String leadingWhitespace(final String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return line.substring(0, i);
    }

    public record Result(boolean success, String error) {
        public static Result ok() {
            return new Result(true, null);
        }

        public static Result failed(final String error) {
            return new Result(false, error);
        }
    }
}
