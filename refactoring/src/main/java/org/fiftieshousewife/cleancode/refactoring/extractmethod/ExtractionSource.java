package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import java.util.regex.Pattern;

/**
 * Renders the Java source text for the two fragments the recipe
 * synthesises: the new private method that will carry the extracted
 * behaviour, and the call-site statement that replaces the original
 * range. The generated text is re-parsed by {@link AstFragments} before
 * being spliced back into the tree, so indentation decisions here only
 * need to produce text that compiles — OpenRewrite re-normalises spacing
 * on print.
 *
 * <p>Two axes drive the shape of the output:
 * <ul>
 *   <li>{@link ExitMode} — whether the range's control flow escapes.
 *       Void conditional-exit rewrites each {@code return;} inside the
 *       body to {@code return true;}, appends {@code return false;},
 *       and wraps the call site in {@code if (…) return;}.</li>
 *   <li>Output kind — no output, an internal-declared output (call site
 *       declares the variable), or an outer-local write (call site
 *       re-assigns the existing declaration).</li>
 * </ul>
 */
final class ExtractionSource {

    private static final Pattern BARE_RETURN = Pattern.compile("\\breturn\\s*;");

    private ExtractionSource() {}

    static String renderExtractedMethod(final String newMethodName,
                                        final ExtractionTarget target,
                                        final ExtractionAnalysis analysis) {
        final String staticModifier = analysis.isStatic() ? "static " : "";
        final String params = String.join(", ", analysis.parameterDeclarations());
        final String throwsClause = analysis.throwsList().isEmpty()
                ? ""
                : " throws " + String.join(", ", analysis.throwsList());
        final String body = bodyFor(target, analysis);
        return """
                private %s%s %s(%s)%s {
                    %s
                }
                """.formatted(
                        staticModifier,
                        analysis.returnTypeSource(),
                        newMethodName,
                        params,
                        throwsClause,
                        body);
    }

    static String renderCallSite(final String newMethodName,
                                 final ExtractionAnalysis analysis) {
        final String args = String.join(", ", analysis.argumentNames());
        final String call = "%s(%s)".formatted(newMethodName, args);
        if (analysis.exitMode() == ExitMode.VOID_CONDITIONAL_EXIT) {
            return "if (" + call + ") return;";
        }
        return analysis.outputVariable()
                .map(v -> v.outerWrite()
                        ? "%s = %s;".formatted(v.name(), call)
                        : "%s %s = %s;".formatted(v.typeSource(), v.name(), call))
                .orElse(call + ";");
    }

    private static String bodyFor(final ExtractionTarget target, final ExtractionAnalysis analysis) {
        final String raw = target.extractedText().strip();
        if (analysis.exitMode() == ExitMode.VOID_CONDITIONAL_EXIT) {
            return BARE_RETURN.matcher(raw).replaceAll("return true;") + "\n        return false;";
        }
        return analysis.outputVariable()
                .map(v -> raw + "\n        return " + v.name() + ";")
                .orElse(raw);
    }
}
