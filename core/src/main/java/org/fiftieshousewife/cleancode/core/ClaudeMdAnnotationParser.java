package org.fiftieshousewife.cleancode.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ClaudeMdAnnotationParser {

    private ClaudeMdAnnotationParser() {}

    static Map<String, String> parseFromFile(final Path claudeMdFile) throws IOException {
        if (!Files.exists(claudeMdFile)) {
            return Map.of();
        }
        return parse(Files.readString(claudeMdFile));
    }

    static Map<String, String> parse(final String content) {
        final List<String> lines = content.lines().toList();
        final Map<String, String> sections = new LinkedHashMap<>();
        int i = 0;
        while (i < lines.size()) {
            final String line = lines.get(i);
            if (line.startsWith(AnnotateMarker.OPEN_PREFIX.token())) {
                final String code = line.substring(AnnotateMarker.OPEN_PREFIX.length())
                        .replace(AnnotateMarker.OPEN_SUFFIX.token(), "").trim();
                final StringBuilder body = new StringBuilder();
                i++;
                while (i < lines.size() && !lines.get(i).startsWith(AnnotateMarker.CLOSE_PREFIX.token())) {
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
