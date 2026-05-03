package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FullyQualifiedReferenceRecipeTest {

    @Test
    void flagsInlineFullyQualifiedParameterType() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void register(java.util.List<String> items) {}
                }
                """);

        final var rows = recipe.collectedRows();
        assertAll(
                () -> assertEquals(1, rows.size()),
                () -> assertTrue(rows.getFirst().fqText().contains("java.util.List"),
                        "fqText should include the offending reference, got: "
                                + rows.getFirst().fqText()));
    }

    @Test
    void doesNotFlagImportedTypes() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    void register(List<String> items) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "an imported type used by short name is not a FQN reference");
    }

    @Test
    void doesNotFlagImportStatementsThemselves() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                import java.util.Map;
                public class Foo {
                    List<String> items;
                    Map<String, String> lookup;
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "the FQN inside an import statement is required and should not be flagged");
    }

    @Test
    void doesNotFlagNestedTypeReferences() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Map;
                public class Foo {
                    void register(Map.Entry<String, String> entry) {}
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "Map.Entry is a nested type already in its readable form, not a package-qualified FQN");
    }

    @Test
    void doesNotFlagClassLiteral() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void register() {
                        Class<?> cls = String.class;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "X.class is a class literal — the only way to express it in source. "
                        + "It is not a fully-qualified package reference");
    }

    @Test
    void doesNotFlagClassLiteralOfImportedType() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.List;
                public class Foo {
                    Class<?> cls = List.class;
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void doesNotFlagStaticFieldAccessOnAnImportedClass() {
        // CLAUDE.instructionsFile is a static field access on an
        // already-imported class. It's NOT a fully-qualified type
        // reference even though the FIELD'S type happens to be a
        // FullyQualified type. The recipe must distinguish between
        // "the receiver chain is a package" and "the receiver is a
        // class on which a typed field happens to live".
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    enum CLAUDE { INSTANCE;
                        public java.nio.file.Path instructionsFile() { return null; }
                    }
                    public java.nio.file.Path read() {
                        return CLAUDE.INSTANCE.instructionsFile();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().stream()
                        .noneMatch(r -> r.fqText().startsWith("CLAUDE.")),
                "CLAUDE.instructionsFile is a static field access, not a package-qualified type. "
                        + "Got: " + recipe.collectedRows());
    }

    @Test
    void doesNotFlagThisFieldAccess() {
        // this.metaFindings is a field access on the current instance,
        // not a fully-qualified package reference. The field's TYPE
        // may happen to be a FullyQualified type, but the access text
        // is still `this.x` — never a real FQN.
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    private java.util.Map<String, String> metaFindings;
                    public void touch() {
                        this.metaFindings = new java.util.HashMap<>();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().stream()
                        .noneMatch(r -> r.fqText().startsWith("this.")),
                "this.x is a field access, not a package-qualified type. Got: "
                        + recipe.collectedRows());
    }

    @Test
    void doesNotFlagEnumValueAccess() {
        // Severity.WARNING — accessing an enum constant through its
        // type. The class / enum name is valuable in code; flagging it
        // would push users toward static imports that erase context.
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    enum Severity { INFO, WARNING, ERROR }
                    public Severity highest() {
                        return Severity.WARNING;
                    }
                }
                """);

        assertTrue(recipe.collectedRows().stream()
                        .noneMatch(r -> r.fqText().startsWith("Severity.")),
                "Severity.WARNING is enum value access, not a FQN. Got: "
                        + recipe.collectedRows());
    }

    @Test
    void doesNotFlagStaticMethodCallOnImportedUtilityClass() {
        // Files.exists(...) — the leftmost identifier `Files` is a
        // class (uppercase), not a package segment. This stays
        // un-flagged even when something resolves to a FullyQualified
        // type along the way.
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.nio.file.Files;
                import java.nio.file.Path;
                public class Foo {
                    public boolean check(Path p) {
                        return Files.exists(p);
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "static method call on imported utility class should not be a G12. "
                        + "Got: " + recipe.collectedRows());
    }

    @Test
    void emitsOneRowPerOccurrenceSoTheAdapterCanAnchorEachAtItsLine() {
        final var recipe = new FullyQualifiedReferenceRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    void a(java.util.List<String> xs) {}
                    void b(java.util.Map<String, String> ys) {}
                }
                """);

        final var rows = recipe.collectedRows();
        assertAll(
                () -> assertEquals(2, rows.size(),
                        "one Row per occurrence — the adapter resolves the line by source-text"),
                () -> assertTrue(rows.stream().anyMatch(r -> r.fqText().contains("java.util.List"))),
                () -> assertTrue(rows.stream().anyMatch(r -> r.fqText().contains("java.util.Map"))));
    }
}
