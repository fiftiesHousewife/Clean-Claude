package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

final class MethodChain {

    private static final int FLUENT_MAJORITY_DIVISOR = 2;

    private MethodChain() {}

    static int depth(final J.MethodInvocation invocation) {
        int depth = 1;
        var current = invocation.getSelect();
        while (current instanceof J.MethodInvocation nested) {
            depth++;
            current = nested.getSelect();
        }
        return depth;
    }

    static boolean isFluent(final J.MethodInvocation invocation) {
        int total = 0;
        int fluent = 0;
        J.MethodInvocation current = invocation;
        while (current != null) {
            total++;
            if (FluentMethodNames.contains(current.getSimpleName())) {
                fluent++;
            }
            current = current.getSelect() instanceof J.MethodInvocation nested ? nested : null;
        }
        return fluent > total / FLUENT_MAJORITY_DIVISOR;
    }

    static J.Identifier findRoot(final J.MethodInvocation invocation) {
        var current = invocation.getSelect();
        while (current instanceof J.MethodInvocation nested) {
            current = nested.getSelect();
        }
        return current instanceof J.Identifier id ? id : null;
    }

    static String asString(final J.MethodInvocation invocation) {
        final List<String> parts = new ArrayList<>();
        parts.add(invocation.getSimpleName() + "()");
        var current = invocation.getSelect();
        while (current instanceof J.MethodInvocation nested) {
            parts.addFirst(nested.getSimpleName() + "()");
            current = nested.getSelect();
        }
        if (current instanceof J.Identifier identifier) {
            parts.addFirst(identifier.getSimpleName());
        }
        return String.join(".", parts);
    }
}
