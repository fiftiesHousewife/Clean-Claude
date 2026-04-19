package io.github.fiftieshousewife.cleancode.refactoring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Set;

public class ExtractClassConstantRecipe extends AbstractConstantExtractionRecipe<Number> {

    private static final Set<Number> TRIVIAL_VALUES = Set.of(0, 1, -1, 0L, 1L, -1L, 0.0, 1.0);

    @JsonCreator
    public ExtractClassConstantRecipe(@JsonProperty("minOccurrences") final int minOccurrences) {
        super(minOccurrences);
    }

    @Override
    public String getDisplayName() {
        return "Extract repeated numeric literals to class constants";
    }

    @Override
    public String getDescription() {
        return "Promotes numeric literals used %d+ times inside a class to private static final fields. "
                .formatted(minOccurrences)
                + "Skips 0, 1, -1 and their long/double variants. Fixes G35.";
    }

    @Override
    protected Number valueIfExtractable(final J.Literal literal) {
        final Object value = literal.getValue();
        if (!(value instanceof Number number)) {
            return null;
        }
        if (TRIVIAL_VALUES.contains(number)) {
            return null;
        }
        if (number instanceof Integer || number instanceof Long || number instanceof Double) {
            return number;
        }
        return null;
    }

    @Override
    protected Statement buildConstantField(final String name, final Number value) {
        final String javaType;
        final String literal;
        if (value instanceof Long) {
            javaType = "long";
            literal = value + "L";
        } else if (value instanceof Double) {
            javaType = "double";
            literal = value.toString();
        } else {
            javaType = "int";
            literal = value.toString();
        }
        return parseFieldFromHolder("class _Tmp { private static final %s %s = %s; }"
                .formatted(javaType, name, literal));
    }

    @Override
    protected String toConstantName(final Number value) {
        final String asText = value.toString()
                .replace(".", "_")
                .replace("-", "NEG_")
                .replace("L", "");
        return "CONSTANT_" + asText;
    }
}
