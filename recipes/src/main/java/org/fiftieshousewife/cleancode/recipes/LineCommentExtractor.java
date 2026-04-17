package org.fiftieshousewife.cleancode.recipes;

final class LineCommentExtractor {

    private static final String LINE_COMMENT_MARKER = "//";
    private static final int LINE_COMMENT_MARKER_LENGTH = LINE_COMMENT_MARKER.length();

    private LineCommentExtractor() {}

    static String extract(final String line) {
        final String stripped = stripStringLiterals(line);
        final int idx = stripped.indexOf(LINE_COMMENT_MARKER);
        if (idx < 0) {
            return null;
        }
        final String text = stripped.substring(idx + LINE_COMMENT_MARKER_LENGTH).trim();
        return text.length() >= MumblingCommentRecipe.MIN_COMMENT_LENGTH ? text : null;
    }

    static String stripStringLiterals(final String line) {
        final StringBuilder result = new StringBuilder(line.length());
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            final char ch = line.charAt(i);
            if (ch == '\\' && inString && i + 1 < line.length()) {
                i++;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
