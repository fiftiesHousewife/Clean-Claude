package io.github.fiftieshousewife.cleancode.recipes.support;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LineNumbers {

    private static final Map<UUID, Map<UUID, Integer>> CACHE = new ConcurrentHashMap<>();

    private LineNumbers() {}

    public static int lineOf(final Cursor cursor) {
        final Object value = cursor.getValue();
        if (!(value instanceof J node)) {
            return -1;
        }
        final J.CompilationUnit cu = cursor.firstEnclosing(J.CompilationUnit.class);
        if (cu == null) {
            return -1;
        }
        return CACHE.computeIfAbsent(cu.getId(), id -> buildIndex(cu))
                .getOrDefault(node.getId(), -1);
    }

    private static Map<UUID, Integer> buildIndex(final J.CompilationUnit cu) {
        final Map<UUID, Integer> index = new HashMap<>();
        final int[] line = {1};
        new JavaIsoVisitor<Object>() {
            @Override
            public @org.jspecify.annotations.Nullable J visit(final @org.jspecify.annotations.Nullable Tree tree, final Object ignored) {
                if (tree instanceof J j) {
                    line[0] += countNewlines(j.getPrefix().getWhitespace());
                    for (final Comment c : j.getPrefix().getComments()) {
                        line[0] += countNewlines(c.printComment(getCursor()));
                    }
                    index.put(j.getId(), line[0]);
                }
                return super.visit(tree, ignored);
            }
        }.visit(cu, new Object());
        return index;
    }

    private static int countNewlines(final String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }
}
