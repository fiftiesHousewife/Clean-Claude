package io.github.fiftieshousewife.cleancode.plugin.serve;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Line-based mutator for {@code build.gradle.kts}. Handles the common
 * Kotlin-DSL forms produced by the README example; deliberately fails
 * fast on anything fancier so we never silently mangle a script.
 *
 * <p>Supported forms:
 * <pre>
 * cleanCode {
 *     disabledRecipes = listOf("G36", "G10")
 *     thresholds {
 *         classLineCount = 200
 *     }
 * }
 * </pre>
 *
 * <p>Bails on: variable references, conditional blocks, or any
 * assignment whose RHS isn't a literal {@code listOf(...)} or integer.
 */
public final class BuildScriptEditor {

    private static final Pattern CLEAN_CODE_BLOCK_START = Pattern.compile("^\\s*cleanCode\\s*\\{\\s*$");
    private static final Pattern THRESHOLDS_BLOCK_START = Pattern.compile("^\\s*thresholds\\s*\\{\\s*$");
    private static final Pattern DISABLED_RECIPES = Pattern.compile(
            "^(\\s*)disabledRecipes\\s*=\\s*listOf\\((.*)\\)\\s*$");
    private static final Pattern THRESHOLD_ASSIGN = Pattern.compile(
            "^(\\s*)([A-Za-z]+)\\s*=\\s*(\\d+)\\s*$");

    private final Path buildScript;
    private List<String> lines;

    public BuildScriptEditor(final Path buildScript) throws IOException {
        this.buildScript = buildScript;
        this.lines = new ArrayList<>(Files.readAllLines(buildScript));
    }

    public void disableRecipe(final String code) {
        final int blockStart = findCleanCodeBlockStart();
        final int blockEnd = findMatchingClose(blockStart);

        for (int i = blockStart + 1; i < blockEnd; i++) {
            final Matcher m = DISABLED_RECIPES.matcher(lines.get(i));
            if (m.matches()) {
                final String existing = m.group(2);
                if (existing.contains("\"" + code + "\"")) {
                    return;
                }
                final String separator = existing.isBlank() ? "" : ", ";
                lines.set(i, m.group(1) + "disabledRecipes = listOf("
                        + existing + separator + "\"" + code + "\")");
                return;
            }
        }

        final String indent = guessIndent(blockStart + 1, blockEnd);
        lines.add(blockEnd, indent + "disabledRecipes = listOf(\"" + code + "\")");
    }

    public void tuneThreshold(final String key, final int newValue) {
        final int cleanCodeStart = findCleanCodeBlockStart();
        final int cleanCodeEnd = findMatchingClose(cleanCodeStart);

        int thresholdsStart = -1;
        int thresholdsEnd = -1;
        for (int i = cleanCodeStart + 1; i < cleanCodeEnd; i++) {
            if (THRESHOLDS_BLOCK_START.matcher(lines.get(i)).matches()) {
                thresholdsStart = i;
                thresholdsEnd = findMatchingClose(i);
                break;
            }
        }

        if (thresholdsStart < 0) {
            final String indent = guessIndent(cleanCodeStart + 1, cleanCodeEnd);
            final String inner = indent + "    ";
            lines.add(cleanCodeEnd, indent + "}");
            lines.add(cleanCodeEnd, inner + key + " = " + newValue);
            lines.add(cleanCodeEnd, indent + "thresholds {");
            return;
        }

        for (int i = thresholdsStart + 1; i < thresholdsEnd; i++) {
            final Matcher m = THRESHOLD_ASSIGN.matcher(lines.get(i));
            if (m.matches() && key.equals(m.group(2))) {
                lines.set(i, m.group(1) + key + " = " + newValue);
                return;
            }
        }

        final String indent = guessIndent(thresholdsStart + 1, thresholdsEnd);
        lines.add(thresholdsEnd, indent + key + " = " + newValue);
    }

    public List<String> currentLines() {
        return List.copyOf(lines);
    }

    public void save() throws IOException {
        Files.write(buildScript, lines);
    }

    private int findCleanCodeBlockStart() {
        for (int i = 0; i < lines.size(); i++) {
            if (CLEAN_CODE_BLOCK_START.matcher(lines.get(i)).matches()) {
                return i;
            }
        }
        lines.add("");
        lines.add("cleanCode {");
        lines.add("}");
        return lines.size() - 2;
    }

    private int findMatchingClose(final int blockStart) {
        int depth = 1;
        for (int i = blockStart + 1; i < lines.size(); i++) {
            for (final char c : lines.get(i).toCharArray()) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        throw new IllegalStateException("could not find matching '}' for block at line " + (blockStart + 1));
    }

    private String guessIndent(final int from, final int to) {
        for (int i = from; i < to; i++) {
            final String line = lines.get(i);
            if (!line.isBlank()) {
                int spaces = 0;
                while (spaces < line.length() && line.charAt(spaces) == ' ') {
                    spaces++;
                }
                return " ".repeat(spaces);
            }
        }
        return "    ";
    }
}
