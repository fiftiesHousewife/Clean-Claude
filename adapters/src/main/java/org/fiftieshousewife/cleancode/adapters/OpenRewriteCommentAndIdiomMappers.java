package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.core.Confidence;
import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.Severity;
import org.fiftieshousewife.cleancode.recipes.BroadCatchRecipe;
import org.fiftieshousewife.cleancode.recipes.CatchLogContinueRecipe;
import org.fiftieshousewife.cleancode.recipes.CommentedCodeRecipe;
import org.fiftieshousewife.cleancode.recipes.EmbeddedLanguageRecipe;
import org.fiftieshousewife.cleancode.recipes.FullyQualifiedReferenceRecipe;
import org.fiftieshousewife.cleancode.recipes.LegacyFileApiRecipe;
import org.fiftieshousewife.cleancode.recipes.MumblingCommentRecipe;
import org.fiftieshousewife.cleancode.recipes.ObsoleteCommentRecipe;
import org.fiftieshousewife.cleancode.recipes.RawGenericRecipe;
import org.fiftieshousewife.cleancode.recipes.SuppressedWarningRecipe;
import org.fiftieshousewife.cleancode.recipes.SwallowedExceptionRecipe;
import org.fiftieshousewife.cleancode.recipes.SystemOutRecipe;
import org.fiftieshousewife.cleancode.recipes.UncheckedCastRecipe;
import org.fiftieshousewife.cleancode.recipes.VerticalSeparationRecipe;
import org.openrewrite.ScanningRecipe;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class OpenRewriteCommentAndIdiomMappers implements RecipeCategoryMapper {

    private final OpenRewriteFindingBuilder builder;

    OpenRewriteCommentAndIdiomMappers(final OpenRewriteFindingBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Optional<List<Finding>> tryMap(final ScanningRecipe<?> recipe) {
        return switch (recipe) {
            case CommentedCodeRecipe r -> Optional.of(commentedCode(r.collectedRows()));
            case MumblingCommentRecipe r -> Optional.of(mumblingComment(r.collectedRows()));
            case ObsoleteCommentRecipe r -> Optional.of(obsoleteComment(r.collectedRows()));
            case VerticalSeparationRecipe r -> Optional.of(verticalSeparation(r.collectedRows()));
            case FullyQualifiedReferenceRecipe r -> Optional.of(fullyQualifiedReferences(r.collectedRows()));
            case LegacyFileApiRecipe r -> Optional.of(legacyFileApi(r.collectedRows()));
            case RawGenericRecipe r -> Optional.of(rawGeneric(r.collectedRows()));
            case EmbeddedLanguageRecipe r -> Optional.of(embeddedLanguage(r.collectedRows()));
            case CatchLogContinueRecipe r -> Optional.of(catchLog(r.collectedRows()));
            case BroadCatchRecipe r -> Optional.of(broadCatch(r.collectedRows()));
            case SwallowedExceptionRecipe r -> Optional.of(swallowedException(r.collectedRows()));
            case SystemOutRecipe r -> Optional.of(systemOut(r.collectedRows()));
            case UncheckedCastRecipe r -> Optional.of(uncheckedCast(r.collectedRows()));
            case SuppressedWarningRecipe r -> Optional.of(suppressedWarning(r.collectedRows()));
            default -> Optional.empty();
        };
    }

    List<Finding> commentedCode(final List<CommentedCodeRecipe.Row> rows) {
        return builder.mapRows(rows, r -> Finding.at(HeuristicCode.C5, r.sourceFile(), r.lineNumber(),
                r.lineNumber(), "Commented-out code: %s".formatted(r.commentPreview()),
                Severity.WARNING, Confidence.MEDIUM,
                OpenRewriteFindingBuilder.TOOL, "CommentedCodeRecipe"));
    }

    List<Finding> mumblingComment(final List<MumblingCommentRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.C3, r.className(), r.lineNumber(),
                "Mumbling comment in '%s': %s".formatted(r.methodName(), r.commentPreview())));
    }

    List<Finding> obsoleteComment(final List<ObsoleteCommentRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.C2, r.className(),
                "Comment references '%s' which is not in scope — update or remove"
                        .formatted(r.missingIdentifier())));
    }

    List<Finding> verticalSeparation(final List<VerticalSeparationRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G10, r.className(),
                r.declarationLine(),
                ("'%s' is declared in %s() but not used until %d lines later"
                        + " — move the declaration closer to line %d")
                        .formatted(r.variableName(), r.methodName(), r.distance(), r.firstUseLine())));
    }

    List<Finding> fullyQualifiedReferences(final List<FullyQualifiedReferenceRecipe.Row> rows) {
        return builder.mapRows(rows, r -> Finding.at(HeuristicCode.G12, r.sourceFile(), 0, 0,
                ("%d inline fully-qualified type reference(s); first: %s"
                        + " — run ShortenFullyQualifiedReferencesRecipe")
                        .formatted(r.count(), r.samplePreview()),
                Severity.WARNING, Confidence.HIGH,
                OpenRewriteFindingBuilder.TOOL, "FullyQualifiedReferenceRecipe"));
    }

    List<Finding> legacyFileApi(final List<LegacyFileApiRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G26, r.className(),
                "'%s' is a legacy API — use java.nio.file.Path and Files instead"
                        .formatted(r.legacyType())));
    }

    List<Finding> rawGeneric(final List<RawGenericRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G26, r.className(),
                "'%s' in '%s' uses Object type parameter — use a typed record or specific generic"
                        .formatted(r.typeName(), r.methodName())));
    }

    List<Finding> embeddedLanguage(final List<EmbeddedLanguageRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G1, r.className(),
                "Embedded %s in method '%s' — extract to a template or resource file"
                        .formatted(r.language().toUpperCase(Locale.ROOT), r.methodName())));
    }

    List<Finding> catchLog(final List<CatchLogContinueRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.Ch7_1, r.className(),
                "Catch block in '%s' only logs or is empty".formatted(r.methodName())));
    }

    List<Finding> broadCatch(final List<BroadCatchRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.Ch7_1, r.className(),
                "Method '%s' catches %s — catch specific exception types instead"
                        .formatted(r.methodName(), r.caughtType())));
    }

    List<Finding> swallowedException(final List<SwallowedExceptionRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G4, r.className(),
                "Method '%s' catches %s and silently swallows it — handle or propagate"
                        .formatted(r.methodName(), r.exceptionType())));
    }

    List<Finding> systemOut(final List<SystemOutRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G4, r.className(),
                "'%s' bypasses structured logging — use @Slf4j instead".formatted(r.call())));
    }

    List<Finding> uncheckedCast(final List<UncheckedCastRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G4, r.className(), r.lineNumber(),
                "@SuppressWarnings(\"unchecked\") on '%s' — redesign to avoid unsafe casts"
                        .formatted(r.memberName())));
    }

    List<Finding> suppressedWarning(final List<SuppressedWarningRecipe.Row> rows) {
        return builder.mapRows(rows, r -> builder.finding(HeuristicCode.G4, r.className(),
                "@SuppressWarnings(\"%s\") on '%s' — redesign to avoid unsafe operations"
                        .formatted(r.warningType(), r.methodName())));
    }
}
