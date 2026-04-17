package org.fiftieshousewife.cleancode.core;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

import java.util.List;
import java.util.Optional;

final class SuppressionAnnotationProcessor {

    static final String SUPPRESS_ANNOTATION = "SuppressCleanCode";
    static final String SUPPRESS_LIST_ANNOTATION = "SuppressCleanCode.List";

    private static final String REPEATABLE_VALUE_FIELD = "value";

    private final List<Suppression> suppressions;
    private final SuppressionMetaFindings meta;

    SuppressionAnnotationProcessor(final List<Suppression> suppressions,
                                   final SuppressionMetaFindings meta) {
        this.suppressions = suppressions;
        this.meta = meta;
    }

    void processAnnotation(final AnnotationExpr annotation,
                           final ParseContext context,
                           final LineRange range) {
        final String name = annotation.getNameAsString();
        if (SUPPRESS_ANNOTATION.equals(name)) {
            processSingle(annotation, context, range);
        } else if (SUPPRESS_LIST_ANNOTATION.equals(name)) {
            processRepeatableContainer(annotation, context, range);
        }
    }

    void processSingle(final AnnotationExpr annotation,
                       final ParseContext context,
                       final LineRange range) {
        extractFields(annotation)
                .filter(SuppressionFields::hasCodes)
                .ifPresent(fields -> record(annotation, context, range, fields));
    }

    private void record(final AnnotationExpr annotation,
                        final ParseContext context,
                        final LineRange range,
                        final SuppressionFields fields) {
        final String sourceFile = context.sourceFile();
        meta.recordIfExpired(annotation, sourceFile, fields.codes(), fields.until());
        meta.recordIfBlankReason(annotation, sourceFile, fields.reason());
        suppressions.add(buildSuppression(context, range, fields));
    }

    private void processRepeatableContainer(final AnnotationExpr annotation,
                                            final ParseContext context,
                                            final LineRange range) {
        if (!(annotation instanceof NormalAnnotationExpr normal)) {
            return;
        }
        normal.getPairs().stream()
                .filter(pair -> REPEATABLE_VALUE_FIELD.equals(pair.getNameAsString()))
                .forEach(pair -> processContainerPair(pair, context, range));
    }

    private void processContainerPair(final MemberValuePair pair,
                                      final ParseContext context,
                                      final LineRange range) {
        pair.getValue().toArrayInitializerExpr().ifPresent(arr ->
                arr.getValues().forEach(value -> {
                    if (value instanceof AnnotationExpr inner) {
                        processSingle(inner, context, range);
                    }
                }));
    }

    private static Optional<SuppressionFields> extractFields(final AnnotationExpr annotation) {
        if (annotation instanceof NormalAnnotationExpr normal) {
            return Optional.of(AnnotationValues.extractSuppressionFields(normal));
        }
        return Optional.empty();
    }

    private static Suppression buildSuppression(final ParseContext context,
                                                final LineRange range,
                                                final SuppressionFields fields) {
        return new Suppression(
                new Suppression.Scope(context.sourceFile(), range.start(), range.end(), context.packagePath()),
                new Suppression.Coverage(fields.codes(), fields.reason(), fields.until()));
    }
}
