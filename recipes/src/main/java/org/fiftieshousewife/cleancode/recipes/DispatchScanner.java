package org.fiftieshousewife.cleancode.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.Set;
import java.util.stream.Collectors;

final class DispatchScanner extends JavaIsoVisitor<ExecutionContext> {

    private static final Set<String> SKIP_METHODS = Set.of("equals", "hashCode", "toString", "main");
    private static final int MIN_BRANCHES = 2;
    private static final String UNKNOWN_CLASS_NAME = "<unknown>";

    private final StringlyTypedDispatchRecipe.Accumulator acc;

    DispatchScanner(StringlyTypedDispatchRecipe.Accumulator acc) {
        this.acc = acc;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
        if (shouldSkip(m)) {
            return m;
        }
        final Set<String> stringParamNames = collectStringParamNames(m);
        if (stringParamNames.isEmpty()) {
            return m;
        }
        final String className = enclosingClassName();
        stringParamNames.forEach(paramName -> recordDispatch(m, className, paramName));
        return m;
    }

    private static boolean shouldSkip(J.MethodDeclaration m) {
        return SKIP_METHODS.contains(m.getSimpleName()) || m.getBody() == null;
    }

    private static Set<String> collectStringParamNames(J.MethodDeclaration m) {
        return m.getParameters().stream()
                .filter(J.VariableDeclarations.class::isInstance)
                .map(J.VariableDeclarations.class::cast)
                .filter(StringlyTypedDispatchRecipe::isStringType)
                .flatMap(v -> v.getVariables().stream())
                .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                .collect(Collectors.toSet());
    }

    private String enclosingClassName() {
        final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
        return enclosingClass != null ? enclosingClass.getSimpleName() : UNKNOWN_CLASS_NAME;
    }

    private void recordDispatch(J.MethodDeclaration m, String className, String paramName) {
        final int switchBranches = DispatchBranchCounter.countSwitchBranches(m.getBody(), paramName);
        if (switchBranches >= MIN_BRANCHES) {
            acc.rows.add(new StringlyTypedDispatchRecipe.Row(className, m.getSimpleName(), paramName, switchBranches));
            return;
        }
        final int ifBranches = DispatchBranchCounter.countIfElseBranches(m.getBody(), paramName);
        if (ifBranches >= MIN_BRANCHES) {
            acc.rows.add(new StringlyTypedDispatchRecipe.Row(className, m.getSimpleName(), paramName, ifBranches));
        }
    }
}
