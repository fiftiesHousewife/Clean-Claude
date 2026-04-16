package org.fiftieshousewife.cleancode.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AnnotateSectionParser {

    private static final String OPEN_MARKER = "<!-- ANNOTATE:";
    private static final String CLOSE_MARKER = "<!-- /ANNOTATE";
    private static final String CLOSE_BRACKET = "-->";

    private AnnotateSectionParser() {}

    static Map<String, String> parse(final String content) {
        final Map<String, String> sections = new LinkedHashMap<>();
        final List<String> lines = content.lines().toList();

        int i = 0;
        while (i < lines.size()) {
            final String line = lines.get(i);
            if (line.startsWith(OPEN_MARKER)) {
                final String code = line.substring(OPEN_MARKER.length()).replace(CLOSE_BRACKET, "").trim();
                final StringBuilder body = new StringBuilder();
                i++;
                while (i < lines.size() && !lines.get(i).startsWith(CLOSE_MARKER)) {
                    body.append(lines.get(i)).append('\n');
                    i++;
                }
                sections.put(code, body.toString());
            }
            i++;
        }
        return sections;
    }
}
