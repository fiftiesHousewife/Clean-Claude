package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SplitFlagArgumentRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Split private method with a boolean flag into two methods";
    }

    @Override
    public String getDescription() {
        return "Emits <name>WhenTrue() and <name>WhenFalse() alongside a private method whose body is "
                + "a single if/else on its sole boolean parameter. The original method and its call "
                + "sites are left intact; delete the original and rewrite call sites manually. Fixes F3.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl,
                                                            ExecutionContext ctx) {
                final J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                final List<Statement> original = c.getBody().getStatements();
                final List<Statement> rewritten = new ArrayList<>(original.size());
                boolean changed = false;

                for (final Statement stmt : original) {
                    rewritten.add(stmt);
                    if (!(stmt instanceof J.MethodDeclaration method)) {
                        continue;
                    }
                    if (!isSplitCandidate(method)) {
                        continue;
                    }
                    final String flagName = flagParameterName(method);
                    final J.If ifStmt = (J.If) method.getBody().getStatements().getFirst();
                    final List<Statement> truePart = asStatements(ifStmt.getThenPart());
                    final List<Statement> falsePart = asStatements(ifStmt.getElsePart().getBody());
                    final J.MethodDeclaration whenTrue = emitHelper(method, flagName, true, truePart);
                    final J.MethodDeclaration whenFalse = emitHelper(method, flagName, false, falsePart);
                    if (whenTrue == null || whenFalse == null) {
                        continue;
                    }
                    rewritten.add(whenTrue);
                    rewritten.add(whenFalse);
                    changed = true;
                }

                return changed ? c.withBody(c.getBody().withStatements(rewritten)) : c;
            }

            private boolean isSplitCandidate(J.MethodDeclaration method) {
                if (!method.getModifiers().stream()
                        .anyMatch(m -> m.getType() == J.Modifier.Type.Private)) {
                    return false;
                }
                final List<Statement> params = method.getParameters();
                if (params.size() != 1 || !(params.getFirst() instanceof J.VariableDeclarations vd)) {
                    return false;
                }
                if (vd.getType() != JavaType.Primitive.Boolean) {
                    return false;
                }
                if (method.getBody() == null || method.getBody().getStatements().size() != 1) {
                    return false;
                }
                if (!(method.getBody().getStatements().getFirst() instanceof J.If ifStmt)) {
                    return false;
                }
                if (ifStmt.getElsePart() == null
                        || ifStmt.getElsePart().getBody() instanceof J.If) {
                    return false;
                }
                final Expression condition = ifStmt.getIfCondition().getTree();
                if (!(condition instanceof J.Identifier id)) {
                    return false;
                }
                return id.getSimpleName().equals(flagParameterName(method));
            }

            private String flagParameterName(J.MethodDeclaration method) {
                final J.VariableDeclarations vd = (J.VariableDeclarations) method.getParameters().getFirst();
                return vd.getVariables().getFirst().getSimpleName();
            }

            private List<Statement> asStatements(Statement body) {
                if (body instanceof J.Block block) {
                    return block.getStatements();
                }
                return List.of(body);
            }

            private J.MethodDeclaration emitHelper(J.MethodDeclaration original,
                                                    String flagName,
                                                    boolean whenTrue,
                                                    List<Statement> body) {
                final String helperName = original.getSimpleName() + suffixFor(flagName, whenTrue);
                final J.Identifier renamed = original.getName().withSimpleName(helperName);
                final J.Block newBody = original.getBody() == null
                        ? null
                        : original.getBody().withStatements(body);
                return original
                        .withName(renamed)
                        .withBody(newBody)
                        .withParameters(List.of());
            }
        };
    }

    static String suffixFor(String flagName, boolean whenTrue) {
        final String capitalised = Character.toUpperCase(flagName.charAt(0))
                + flagName.substring(1).toLowerCase(Locale.ROOT);
        return "When" + capitalised + (whenTrue ? "" : "IsFalse");
    }
}
