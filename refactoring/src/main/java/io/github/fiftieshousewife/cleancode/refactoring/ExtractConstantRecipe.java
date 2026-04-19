package io.github.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Locale;

public class ExtractConstantRecipe extends AbstractConstantExtractionRecipe<String> {

    private static final int MIN_STRING_LENGTH = 3;

    @JsonCreator
    public ExtractConstantRecipe(@JsonProperty("minOccurrences") final int minOccurrences) {
        super(minOccurrences);
    }

    @Override
    public String getDisplayName() {
        return "Extract duplicated strings to named constants";
    }

    @Override
    public String getDescription() {
        return "Extracts string literals that appear %d+ times into private static final constants. Fixes G25."
                .formatted(minOccurrences);
    }

    @Override
    protected String valueIfExtractable(final J.Literal literal) {
        if (literal.getValue() instanceof String s && s.length() >= MIN_STRING_LENGTH) {
            return s;
        }
        return null;
    }

    @Override
    protected Statement buildConstantField(final String name, final String value) {
        final String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return parseFieldFromHolder("class _Tmp { private static final String %s = \"%s\"; }"
                .formatted(name, escaped));
    }

    @Override
    protected String toConstantName(final String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(Locale.ROOT);
    }
}
