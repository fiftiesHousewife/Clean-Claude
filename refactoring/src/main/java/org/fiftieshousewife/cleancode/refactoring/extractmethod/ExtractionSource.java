package org.fiftieshousewife.cleancode.refactoring.extractmethod;

/**
 * Renders the Java source text for the two fragments the recipe synthesises:
 * the new private method that will carry the extracted behaviour, and the
 * call-site statement that replaces the original range.
 *
 * <p>The generated text is re-parsed by {@link AstFragments} before being
 * spliced back into the tree, so indentation decisions here only need to
 * produce text that compiles — OpenRewrite re-normalises spacing on print.
 */
final class ExtractionSource {

    private ExtractionSource() {}

    static String renderExtractedMethod(final String newMethodName,
                                        final ExtractionTarget target,
                                        final ExtractionAnalysis analysis) {
        final String staticModifier = analysis.isStatic() ? "static " : "";
        final String params = String.join(", ", analysis.parameterDeclarations());
        final String throwsClause = analysis.throwsList().isEmpty()
                ? ""
                : " throws " + String.join(", ", analysis.throwsList());
        final String body = target.extractedText().strip();
        final String returnTail = analysis.outputVariable()
                .map(v -> "\n        return " + v.name() + ";")
                .orElse("");
        return """
                private %s%s %s(%s)%s {
                    %s%s
                }
                """.formatted(
                        staticModifier,
                        analysis.returnTypeSource(),
                        newMethodName,
                        params,
                        throwsClause,
                        body,
                        returnTail);
    }

    static String renderCallSite(final String newMethodName,
                                 final ExtractionAnalysis analysis) {
        final String args = String.join(", ", analysis.argumentNames());
        return analysis.outputVariable()
                .map(v -> "%s %s = %s(%s);".formatted(v.typeSource(), v.name(), newMethodName, args))
                .orElse("%s(%s);".formatted(newMethodName, args));
    }
}
