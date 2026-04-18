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
        // All lines at column 0. The caller (ExtractMethodRecipe.spliceExtraction)
        // adds the correct indent per line based on its position in the enclosing
        // class — signature/close at method-level, body lines at body-level.
        return staticModifier + analysis.returnTypeSource() + " "
                + newMethodName + "(" + params + ")" + throwsClause + " {\n"
                + bodyFor(target, analysis) + "\n"
                + "}\n";
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
        final String raw = dedent(target.extractedText().stripTrailing());
        if (analysis.exitMode() == ExitMode.VOID_CONDITIONAL_EXIT) {
            return BARE_RETURN.matcher(raw).replaceAll("return true;") + "\nreturn false;";
        }
        return analysis.outputVariable()
                .map(v -> raw + "\nreturn " + v.name() + ";")
                .orElse(raw);
    }

    /**
     * Strip the minimum leading whitespace shared by every non-blank line. The
     * input is line-aligned (see {@link ExtractionTargetFinder}) so all body
     * lines carry the same source indent, and this reduces to a straight
     * minimum-indent dedent.
     */
    private static String dedent(final String text) {
        final String[] lines = text.split("\n", -1);
        int common = Integer.MAX_VALUE;
        for (final String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            int i = 0;
            while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
                i++;
            }
            common = Math.min(common, i);
        }
        if (common == Integer.MAX_VALUE || common == 0) {
            return text;
        }
        final StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            final String line = lines[i];
            out.append(line.length() >= common ? line.substring(common) : line);
        }
        return out.toString();
    }
}
