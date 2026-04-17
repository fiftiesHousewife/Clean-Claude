package org.fiftieshousewife.cleancode.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ClaudeMdAnnotationParser {

    private static final String ANNOTATE_OPEN_PREFIX = "<!-- ANNOTATE:";
    private static final String ANNOTATE_CLOSE_PREFIX = "<!-- /ANNOTATE";
    private static final String ANNOTATE_OPEN_SUFFIX = "-->";

    private ClaudeMdAnnotationParser() {}

    static Map<String, String> parseFromFile(final Path claudeMdFile) throws IOException {
        if (!Files.exists(claudeMdFile)) {
            return Map.of();
        }
        return parse(Files.readString(claudeMdFile));
    }

    static Map<String, String> parse(final String content) {
        final Map<String, String> sections = new LinkedHashMap<>();
        final List<String> lines = content.lines().toList();

        int i = 0;
        while (i < lines.size()) {
            final String line = lines.get(i);
            if (line.startsWith(ANNOTATE_OPEN_PREFIX)) {
                final String code = line.substring(ANNOTATE_OPEN_PREFIX.length())
                        .replace(ANNOTATE_OPEN_SUFFIX, "").trim();
                final StringBuilder body = new StringBuilder();
                i++;
                while (i < lines.size() && !lines.get(i).startsWith(ANNOTATE_CLOSE_PREFIX)) {
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
