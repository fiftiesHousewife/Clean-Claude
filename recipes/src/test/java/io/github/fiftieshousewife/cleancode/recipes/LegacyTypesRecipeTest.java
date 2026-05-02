package io.github.fiftieshousewife.cleancode.recipes;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyTypesRecipeTest {

    @Test
    void detectsJavaIoFileDeclaration() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.io.File;
                public class Foo {
                    void method() {
                        File file = new File("/tmp/data.csv");
                    }
                }
                """);

        assertTrue(recipe.collectedRows().stream()
                .anyMatch(r -> r.legacyType().equals("File")
                        && r.replacement().toLowerCase().contains("nio")),
                "java.io.File should be flagged with an nio replacement hint");
    }

    @Test
    void detectsFileInputStreamConstruction() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.io.File;
                import java.io.FileInputStream;
                import java.io.IOException;
                public class Foo {
                    void method() throws IOException {
                        FileInputStream fis = new FileInputStream(new File("/tmp/data.csv"));
                    }
                }
                """);

        assertFalse(recipe.collectedRows().isEmpty());
    }

    @Test
    void ignoresNioPath() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.nio.file.Path;
                public class Foo {
                    void method() {
                        Path path = Path.of("/tmp/data.csv");
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty());
    }

    @Test
    void detectsLegacyDateAndCalendar() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Calendar;
                import java.util.Date;
                public class Foo {
                    Date d = new Date();
                    Calendar c = Calendar.getInstance();
                }
                """);

        final List<String> legacyTypes = recipe.collectedRows().stream()
                .map(LegacyTypesRecipe.Row::legacyType).toList();

        assertAll(
                () -> assertTrue(legacyTypes.contains("Date"),
                        "java.util.Date should be flagged"),
                () -> assertTrue(legacyTypes.contains("Calendar"),
                        "java.util.Calendar should be flagged"));
    }

    @Test
    void detectsLegacyCollectionsVectorHashtableStack() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Hashtable;
                import java.util.Stack;
                import java.util.Vector;
                public class Foo {
                    Vector<String> v = new Vector<>();
                    Hashtable<String, String> h = new Hashtable<>();
                    Stack<Integer> s = new Stack<>();
                }
                """);

        final List<String> legacyTypes = recipe.collectedRows().stream()
                .map(LegacyTypesRecipe.Row::legacyType).toList();

        assertAll(
                () -> assertTrue(legacyTypes.contains("Vector"),
                        "Vector should be flagged in favour of ArrayList"),
                () -> assertTrue(legacyTypes.contains("Hashtable"),
                        "Hashtable should be flagged in favour of HashMap/ConcurrentHashMap"),
                () -> assertTrue(legacyTypes.contains("Stack"),
                        "Stack should be flagged in favour of ArrayDeque"));
    }

    @Test
    void detectsRandomAndSimpleDateFormat() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.text.SimpleDateFormat;
                import java.util.Random;
                public class Foo {
                    Random r = new Random();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                }
                """);

        final List<String> legacyTypes = recipe.collectedRows().stream()
                .map(LegacyTypesRecipe.Row::legacyType).toList();

        assertAll(
                () -> assertTrue(legacyTypes.contains("Random")),
                () -> assertTrue(legacyTypes.contains("SimpleDateFormat")));
    }

    @Test
    void detectsStringBuffer() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    String build() {
                        StringBuffer sb = new StringBuffer();
                        sb.append("x");
                        return sb.toString();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().stream()
                .anyMatch(r -> r.legacyType().equals("StringBuffer")));
    }

    @Test
    void ignoresStringBuilder() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    String build() {
                        StringBuilder sb = new StringBuilder();
                        sb.append("x");
                        return sb.toString();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().isEmpty(),
                "StringBuilder is the recommended modern API and must not fire");
    }

    @Test
    void detectsClassNewInstanceCall() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                public class Foo {
                    Object create(Class<?> cls) throws Exception {
                        return cls.newInstance();
                    }
                }
                """);

        assertTrue(recipe.collectedRows().stream()
                .anyMatch(r -> r.legacyType().equals("Class.newInstance()")),
                "Class.newInstance() is deprecated since Java 9 and should be flagged");
    }

    @Test
    void carriesSpecificReplacementHintPerLegacyType() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.util.Hashtable;
                import java.util.Vector;
                public class Foo {
                    Vector<String> v = new Vector<>();
                    Hashtable<String, String> h = new Hashtable<>();
                }
                """);

        final var byType = recipe.collectedRows().stream()
                .collect(java.util.stream.Collectors.toMap(
                        LegacyTypesRecipe.Row::legacyType,
                        LegacyTypesRecipe.Row::replacement,
                        (a, b) -> a));

        assertAll(
                () -> assertTrue(byType.get("Vector").toLowerCase().contains("arraylist"),
                        "Vector hint should mention ArrayList"),
                () -> assertTrue(byType.get("Hashtable").toLowerCase().contains("hashmap"),
                        "Hashtable hint should mention HashMap"));
    }

    @Test
    void handlesMultipleLegacyTypesInSameFile() {
        final var recipe = new LegacyTypesRecipe();
        RecipeTestHelper.runAgainst(recipe, """
                package com.example;
                import java.io.File;
                import java.util.Date;
                import java.util.Vector;
                public class Foo {
                    File f = new File("/tmp");
                    Date d = new Date();
                    Vector<String> v = new Vector<>();
                }
                """);

        assertEquals(3, recipe.collectedRows().size(),
                "every legacy type used in the file should produce its own row");
    }
}
