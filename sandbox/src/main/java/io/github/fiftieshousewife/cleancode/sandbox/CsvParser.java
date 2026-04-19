package io.github.fiftieshousewife.cleancode.sandbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV row parser with quoted-field + escape handling. The
 * parseRow method carries a five-phase state machine — initialise,
 * advance through characters, handle quote toggles, handle field
 * separators, finalise — in one loop. Extract-method friendly: each
 * phase is a coherent block and none reassign the field list.
 */
public final class CsvParser {

    private final char separator;
    private final char quote;
    private int rowsParsed = 0;

    public CsvParser() {
        this(',', '"');
    }

    public CsvParser(final char separator, final char quote) {
        this.separator = separator;
        this.quote = quote;
    }

    public List<String> parseRow(final String line) {
        final List<String> fields = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;
        int index = 0;

        while (index < line.length()) {
            final char ch = line.charAt(index);

            if (ch == quote) {
                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == quote) {
                    current.append(quote);
                    index = index + 2;
                    continue;
                }
                insideQuotes = !insideQuotes;
                index = index + 1;
                continue;
            }

            if (ch == separator && !insideQuotes) {
                fields.add(current.toString());
                current.setLength(0);
                index = index + 1;
                continue;
            }

            if (ch == '\\' && index + 1 < line.length()) {
                current.append(line.charAt(index + 1));
                index = index + 2;
                continue;
            }

            current.append(ch);
            index = index + 1;
        }

        fields.add(current.toString());
        rowsParsed = rowsParsed + 1;
        return fields;
    }

    public int rowsParsed() {
        return rowsParsed;
    }
}
