package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts {@code void foo(List<T> out, ...) { ... out.add(x); ... }} into
 * {@code List<T> foo(...) { List<T> out = new ArrayList<>(); ... out.add(x); ... return out; }}
 * and rewrites same-class callers so the returned list is assigned back to
 * the variable the caller had already declared. Addresses F2 for the
 * narrow-but-common "pure accumulator" shape the 4-way rework runs keep
 * re-fixing by hand (e.g. {@code UserAccountService.validate}).
 *
 * <p>Preconditions, all mandatory:
 * <ul>
 *   <li>Method is {@code private} or package-private (same-CU call sites only).
 *   <li>Method returns {@code void}.
 *   <li>Exactly one parameter whose declared type renders as {@code List}
 *       or {@code List<...>}.
 *   <li>Every reference to that parameter in the body is a
 *       {@code param.add(...)} invocation — no reads, no removes, no
 *       passing it elsewhere.
 *   <li>Same-class callers follow the pattern
 *       {@code List<T> local = new ArrayList<>(); foo(local, ...);} — the
 *       recipe rewrites the pair to {@code List<T> local = foo(...);}.
 *       Other call shapes (pre-existing list reused, call in an expression,
 *       cross-class) are left alone.
 * </ul>
 *
 * <p>If any precondition fails for a method, the recipe leaves it alone.
 * Multi-parameter accumulators ({@code Map}/{@code Set}) and non-private
 * visibility are deliberately deferred.
 */
public final class ReturnInsteadOfMutateArgRecipe extends Recipe {

    private static final String LIST_TYPE_PREFIX = "List";

    @Override
    public String getDisplayName() {
        return "Return a List instead of mutating a List argument";
    }

    @Override
    public String getDescription() {
        return "Rewrites private/package-private void methods whose sole mutation is "
                + "appending to a List argument — replaces the argument with a local, "
                + "returns it, and updates same-class call sites. Fixes F2 for the "
                + "pure accumulator pattern.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AccumulatorVisitor();
    }

    private record MethodCandidate(String methodName, String paramName,
                                   String paramTypeDeclaration, int paramIndex) {}

    private static final class AccumulatorVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final Map<String, MethodCandidate> candidatesByName = new HashMap<>();

        @Override
        public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl,
                                                        final ExecutionContext ctx) {
            candidatesByName.clear();
            if (classDecl.getBody() != null) {
                for (final Statement statement : classDecl.getBody().getStatements()) {
                    if (!(statement instanceof J.MethodDeclaration method)) {
                        continue;
                    }
                    final MethodCandidate candidate = classify(method);
                    if (candidate != null) {
                        candidatesByName.put(candidate.methodName(), candidate);
                    }
                }
            }
            if (candidatesByName.isEmpty()) {
                return classDecl;
            }
            // Drop any candidate whose call sites include shapes we can't
            // safely rewrite. If a caller passes the list inline, to another
            // method, or into a non-adjacent statement, rewriting the
            // signature without updating that caller produces broken code.
            disqualifyUnrewritableCallSites(classDecl);
            if (candidatesByName.isEmpty()) {
                return classDecl;
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }

        private void disqualifyUnrewritableCallSites(final J.ClassDeclaration classDecl) {
            if (classDecl.getBody() == null) {
                return;
            }
            // Collect the (method-name, identity) of every call site inside the
            // adjacent-declare-then-call shape we know how to rewrite.
            final Set<J.MethodInvocation> rewritable = new HashSet<>();
            for (final Statement stmt : classDecl.getBody().getStatements()) {
                if (!(stmt instanceof J.MethodDeclaration method) || method.getBody() == null) {
                    continue;
                }
                final List<Statement> stmts = method.getBody().getStatements();
                for (int i = 0; i < stmts.size() - 1; i++) {
                    if (!(stmts.get(i) instanceof J.VariableDeclarations vd)
                            || !isListConstructorInit(vd)) {
                        continue;
                    }
                    final String listVar = vd.getVariables().getFirst().getSimpleName();
                    final J.MethodInvocation call = extractInvocation(stmts.get(i + 1));
                    if (call == null || call.getSelect() != null) {
                        continue;
                    }
                    final MethodCandidate candidate = candidatesByName.get(call.getSimpleName());
                    if (candidate != null && argAtIndexIs(call, candidate.paramIndex(), listVar)) {
                        rewritable.add(call);
                    }
                }
            }

            // Walk the class again; any unqualified invocation of a candidate
            // that isn't in the rewritable set disqualifies that candidate.
            final Set<String> disqualified = new HashSet<>();
            new JavaIsoVisitor<Object>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation mi,
                                                                final Object unused) {
                    if (candidatesByName.containsKey(mi.getSimpleName())
                            && mi.getSelect() == null
                            && !rewritable.contains(mi)) {
                        disqualified.add(mi.getSimpleName());
                    }
                    return super.visitMethodInvocation(mi, unused);
                }
            }.visit(classDecl.getBody(), null);
            disqualified.forEach(candidatesByName::remove);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method,
                                                          final ExecutionContext ctx) {
            final J.MethodDeclaration visited = super.visitMethodDeclaration(method, ctx);
            final MethodCandidate candidate = candidatesByName.get(visited.getSimpleName());
            if (candidate == null) {
                return visited;
            }
            return rewriteMethod(visited, candidate, ctx);
        }

        @Override
        public J.Block visitBlock(final J.Block block, final ExecutionContext ctx) {
            final J.Block visited = super.visitBlock(block, ctx);
            if (candidatesByName.isEmpty()) {
                return visited;
            }
            final List<Statement> original = visited.getStatements();
            final List<Statement> rewritten = new ArrayList<>(original.size());
            int i = 0;
            while (i < original.size()) {
                final Statement stmt = original.get(i);
                final Statement next = i + 1 < original.size() ? original.get(i + 1) : null;
                final CallSiteRewrite rewrite = tryRewriteCallSite(stmt, next);
                if (rewrite != null) {
                    rewritten.add(rewrite.replacement());
                    i += 2;
                    continue;
                }
                rewritten.add(stmt);
                i++;
            }
            return visited.withStatements(rewritten);
        }

        private CallSiteRewrite tryRewriteCallSite(final Statement stmt, final Statement next) {
            if (next == null) {
                return null;
            }
            if (!(stmt instanceof J.VariableDeclarations vd)) {
                return null;
            }
            if (!isListConstructorInit(vd)) {
                return null;
            }
            final String listVar = vd.getVariables().getFirst().getSimpleName();
            final J.MethodInvocation call = extractInvocation(next);
            if (call == null) {
                return null;
            }
            final MethodCandidate candidate = candidatesByName.get(call.getSimpleName());
            if (candidate == null || !argAtIndexIs(call, candidate.paramIndex(), listVar)) {
                return null;
            }
            if (call.getArguments().size() <= candidate.paramIndex()) {
                return null;
            }
            final List<Expression> remainingArgs = new ArrayList<>(call.getArguments());
            remainingArgs.remove(candidate.paramIndex());
            final J.MethodInvocation newInvocation = call.withArguments(remainingArgs)
                    .withPrefix(org.openrewrite.java.tree.Space.SINGLE_SPACE);
            final J.VariableDeclarations.NamedVariable originalVar = vd.getVariables().getFirst();
            final J.VariableDeclarations rewrittenVd = vd.withVariables(
                    List.of(originalVar.withInitializer(newInvocation)));
            return new CallSiteRewrite(rewrittenVd);
        }

        private J.MethodDeclaration rewriteMethod(final J.MethodDeclaration method,
                                                  final MethodCandidate candidate,
                                                  final ExecutionContext ctx) {
            if (!stillMatchesCandidate(method, candidate)) {
                return method;
            }
            final String remainingParams = renderParamsExcluding(method, candidate.paramName());
            final String leadingModifiers = renderModifiers(method);
            final String templateSource = ("%s%s %s(%s) {\n"
                    + "    final %s %s = new ArrayList<>();\n"
                    + "    return %s;\n"
                    + "}").formatted(
                            leadingModifiers,
                            candidate.paramTypeDeclaration(), candidate.methodName(),
                            remainingParams,
                            candidate.paramTypeDeclaration(), candidate.paramName(),
                            candidate.paramName());
            final J.MethodDeclaration scaffold = JavaTemplate.builder(templateSource)
                    .contextSensitive()
                    .imports("java.util.ArrayList")
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace());
            maybeAddImport("java.util.ArrayList");
            final List<Statement> scaffoldStmts = scaffold.getBody().getStatements();
            final List<Statement> newBody = new ArrayList<>(scaffoldStmts.size() + method.getBody().getStatements().size());
            newBody.add(scaffoldStmts.getFirst());
            newBody.addAll(method.getBody().getStatements());
            newBody.add(scaffoldStmts.getLast());
            return scaffold
                    .withPrefix(method.getPrefix())
                    .withBody(scaffold.getBody().withStatements(newBody));
        }

        private static String renderModifiers(final J.MethodDeclaration method) {
            final StringBuilder sb = new StringBuilder();
            for (final J.Modifier mod : method.getModifiers()) {
                switch (mod.getType()) {
                    case Static -> sb.append("static ");
                    case Final -> sb.append("final ");
                    case Synchronized -> sb.append("synchronized ");
                    default -> { /* public/protected are filtered by classify(); skip others */ }
                }
            }
            return sb.toString();
        }

        private static boolean stillMatchesCandidate(final J.MethodDeclaration method,
                                                     final MethodCandidate candidate) {
            for (final Statement p : method.getParameters()) {
                if (p instanceof J.VariableDeclarations vd
                        && !vd.getVariables().isEmpty()
                        && vd.getVariables().getFirst().getSimpleName().equals(candidate.paramName())) {
                    return true;
                }
            }
            return false;
        }

        private static String renderParamsExcluding(final J.MethodDeclaration method, final String excluding) {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (final Statement p : method.getParameters()) {
                if (!(p instanceof J.VariableDeclarations vd) || vd.getVariables().isEmpty()) {
                    continue;
                }
                final String name = vd.getVariables().getFirst().getSimpleName();
                if (name.equals(excluding) || vd.getTypeExpression() == null) {
                    continue;
                }
                if (!first) {
                    sb.append(", ");
                }
                for (final J.Modifier mod : vd.getModifiers()) {
                    if (mod.getType() == J.Modifier.Type.Final) {
                        sb.append("final ");
                    }
                }
                sb.append(vd.getTypeExpression().toString().trim())
                        .append(' ').append(name);
                first = false;
            }
            return sb.toString();
        }
    }

    private record CallSiteRewrite(Statement replacement) {}

    private static MethodCandidate classify(final J.MethodDeclaration method) {
        if (method.getBody() == null || method.isConstructor()) {
            return null;
        }
        if (!isPackagePrivateOrPrivate(method)) {
            return null;
        }
        if (!returnsVoid(method)) {
            return null;
        }
        final int listParamIndex = findSoleListParameterIndex(method);
        if (listParamIndex < 0) {
            return null;
        }
        final J.VariableDeclarations listParam = (J.VariableDeclarations) method.getParameters().get(listParamIndex);
        final String paramName = listParam.getVariables().getFirst().getSimpleName();
        if (!bodyOnlyAppendsToList(method, paramName)) {
            return null;
        }
        return new MethodCandidate(method.getSimpleName(), paramName,
                listParam.getTypeExpression() == null ? "List<Object>"
                        : listParam.getTypeExpression().toString().trim(),
                listParamIndex);
    }

    private static boolean isPackagePrivateOrPrivate(final J.MethodDeclaration method) {
        for (final J.Modifier mod : method.getModifiers()) {
            if (mod.getType() == J.Modifier.Type.Public || mod.getType() == J.Modifier.Type.Protected) {
                return false;
            }
        }
        return true;
    }

    private static boolean returnsVoid(final J.MethodDeclaration method) {
        return method.getReturnTypeExpression() != null
                && "void".equals(method.getReturnTypeExpression().toString().trim());
    }

    private static int findSoleListParameterIndex(final J.MethodDeclaration method) {
        int foundIndex = -1;
        final List<Statement> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            final Statement param = params.get(i);
            if (!(param instanceof J.VariableDeclarations vd) || vd.getTypeExpression() == null) {
                continue;
            }
            final String rendered = vd.getTypeExpression().toString().trim();
            if (rendered.equals(LIST_TYPE_PREFIX) || rendered.startsWith(LIST_TYPE_PREFIX + "<")) {
                if (foundIndex >= 0) {
                    return -1;
                }
                foundIndex = i;
            }
        }
        return foundIndex;
    }

    private static boolean bodyOnlyAppendsToList(final J.MethodDeclaration method, final String paramName) {
        final boolean[] disqualified = {false};
        final boolean[] anyAppend = {false};
        new JavaIsoVisitor<Object>() {
            @Override
            public J.Identifier visitIdentifier(final J.Identifier ident, final Object unused) {
                if (ident.getSimpleName().equals(paramName)) {
                    final Object parent = getCursor().getParentTreeCursor().getValue();
                    if (!(parent instanceof J.MethodInvocation mi
                            && mi.getSelect() instanceof J.Identifier sel
                            && sel.getSimpleName().equals(paramName)
                            && "add".equals(mi.getSimpleName()))) {
                        disqualified[0] = true;
                    }
                }
                return ident;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation invocation, final Object unused) {
                if (invocation.getSelect() instanceof J.Identifier sel
                        && sel.getSimpleName().equals(paramName)) {
                    if (!"add".equals(invocation.getSimpleName())) {
                        disqualified[0] = true;
                    } else {
                        anyAppend[0] = true;
                    }
                    for (final Expression arg : invocation.getArguments()) {
                        visit(arg, unused);
                    }
                    return invocation;
                }
                return super.visitMethodInvocation(invocation, unused);
            }
        }.visit(method.getBody(), null);
        return anyAppend[0] && !disqualified[0];
    }

    private static boolean isListConstructorInit(final J.VariableDeclarations vd) {
        if (vd.getTypeExpression() == null || vd.getVariables().size() != 1) {
            return false;
        }
        final String rendered = vd.getTypeExpression().toString().trim();
        if (!rendered.equals(LIST_TYPE_PREFIX) && !rendered.startsWith(LIST_TYPE_PREFIX + "<")
                && !rendered.startsWith("ArrayList") && !rendered.startsWith("java.util.ArrayList")
                && !rendered.startsWith("java.util.List")) {
            return false;
        }
        return vd.getVariables().getFirst().getInitializer() instanceof J.NewClass;
    }

    private static J.MethodInvocation extractInvocation(final Statement stmt) {
        if (stmt instanceof J.MethodInvocation mi) {
            return mi;
        }
        return null;
    }

    private static boolean argAtIndexIs(final J.MethodInvocation call, final int index, final String varName) {
        if (call.getArguments().size() <= index || index < 0) {
            return false;
        }
        return call.getArguments().get(index) instanceof J.Identifier id
                && id.getSimpleName().equals(varName);
    }
}
