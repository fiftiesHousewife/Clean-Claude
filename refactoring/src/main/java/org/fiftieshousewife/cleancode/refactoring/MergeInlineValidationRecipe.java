package org.fiftieshousewife.cleancode.refactoring;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Moves an inline validation block out of a caller method and into the
 * class's existing {@code validate(..., List<String> errors)} method.
 *
 * <p>Triggers when both shapes are present in the same class:
 *
 * <ol>
 *   <li>A method <em>M</em> whose body contains, in order:
 *     <ul>
 *       <li>{@code List<String> errors = new ArrayList<>();}</li>
 *       <li>≥ 2 sibling {@code if (COND) errors.add(MSG);} statements</li>
 *       <li>optionally {@code if (!errors.isEmpty()) throw ...;}</li>
 *     </ul>
 *   </li>
 *   <li>A method {@code void validate(..., List<String> errors)} whose last
 *       parameter is a {@code List<String>}.</li>
 * </ol>
 *
 * <p>Each inline if-block that isn't already present in {@code validate}'s
 * body is appended to {@code validate}; any referenced caller parameter
 * that isn't already a parameter of {@code validate} is added (keeping the
 * errors list last). The inline if-blocks are replaced with
 * {@code validate(args..., errors);}.
 *
 * <p>Scope: validated values must be parameters of <em>M</em>; locals are
 * out of scope for the first cut.
 */
public final class MergeInlineValidationRecipe extends Recipe {

    private static final String VALIDATE_METHOD_NAME = "validate";
    private static final String LIST_STRING_RENDERED = "List<String>";
    private static final int MIN_INLINE_ADDS = 2;

    @Override
    public String getDisplayName() {
        return "Merge inline validation into an existing validate() method";
    }

    @Override
    public String getDescription() {
        return "When a method contains an inline `errors.add(...)` validation "
                + "block and the class already has `void validate(..., "
                + "List<String> errors)`, fold the inline blocks into "
                + "validate() and replace the inline block with a call.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MergeVisitor();
    }

    private static final class MergeVisitor extends JavaIsoVisitor<ExecutionContext> {

        private ValidateTarget validateTarget;
        private final Map<String, CallerPlan> plans = new LinkedHashMap<>();

        @Override
        public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl,
                                                        final ExecutionContext ctx) {
            validateTarget = null;
            plans.clear();
            if (classDecl.getBody() == null) {
                return classDecl;
            }
            validateTarget = findValidate(classDecl);
            if (validateTarget == null) {
                return classDecl;
            }
            for (final Statement stmt : classDecl.getBody().getStatements()) {
                if (!(stmt instanceof J.MethodDeclaration method)
                        || method.getSimpleName().equals(VALIDATE_METHOD_NAME)) {
                    continue;
                }
                final CallerPlan plan = tryBuildCallerPlan(method);
                if (plan != null) {
                    plans.put(method.getSimpleName(), plan);
                }
            }
            if (plans.isEmpty()) {
                return classDecl;
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method,
                                                          final ExecutionContext ctx) {
            final J.MethodDeclaration visited = super.visitMethodDeclaration(method, ctx);
            if (validateTarget != null
                    && method.getSimpleName().equals(VALIDATE_METHOD_NAME)) {
                return rewriteValidate(visited);
            }
            final CallerPlan plan = plans.get(method.getSimpleName());
            if (plan != null) {
                return rewriteCaller(visited, plan);
            }
            return visited;
        }

        private J.MethodDeclaration rewriteValidate(final J.MethodDeclaration validateMethod) {
            final Set<String> allParamsToAdd = new LinkedHashSet<>();
            final List<String> appendedBlockSources = new ArrayList<>();
            for (final CallerPlan plan : plans.values()) {
                plan.paramsToAdd().forEach(p -> allParamsToAdd.add(p.render()));
                appendedBlockSources.addAll(plan.ifBlockSourcesToAppend());
            }
            if (allParamsToAdd.isEmpty() && appendedBlockSources.isEmpty()) {
                return validateMethod;
            }
            final String templateSource = buildValidateTemplate(validateMethod,
                    allParamsToAdd, appendedBlockSources);
            final J.MethodDeclaration replaced = JavaTemplate.builder(templateSource)
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), validateMethod.getCoordinates().replace());
            return replaced.withPrefix(validateMethod.getPrefix());
        }

        private String buildValidateTemplate(final J.MethodDeclaration validateMethod,
                                             final Set<String> extraParams,
                                             final List<String> appendedBlocks) {
            final List<String> paramSources = new ArrayList<>();
            final List<Statement> originalParams = validateMethod.getParameters();
            for (int i = 0; i < originalParams.size(); i++) {
                if (!(originalParams.get(i) instanceof J.VariableDeclarations vd)) {
                    continue;
                }
                if (i == originalParams.size() - 1) {
                    paramSources.addAll(extraParams);
                }
                paramSources.add(renderParam(vd));
            }
            final StringBuilder body = new StringBuilder();
            validateMethod.getBody().getStatements().forEach(s ->
                    body.append("    ").append(printStatement(s)).append("\n"));
            appendedBlocks.forEach(src ->
                    body.append("    ").append(src.strip()).append("\n"));
            return "void " + VALIDATE_METHOD_NAME + "("
                    + String.join(", ", paramSources) + ") {\n"
                    + body
                    + "}";
        }

        private J.MethodDeclaration rewriteCaller(final J.MethodDeclaration method,
                                                  final CallerPlan plan) {
            if (method.getBody() == null) {
                return method;
            }
            final String templateSource = buildCallerTemplate(method, plan);
            final J.MethodDeclaration replaced = JavaTemplate.builder(templateSource)
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace());
            return replaced.withPrefix(method.getPrefix());
        }

        private String buildCallerTemplate(final J.MethodDeclaration method,
                                           final CallerPlan plan) {
            final StringBuilder header = new StringBuilder();
            for (final J.Modifier mod : method.getModifiers()) {
                switch (mod.getType()) {
                    case Public -> header.append("public ");
                    case Protected -> header.append("protected ");
                    case Private -> header.append("private ");
                    case Static -> header.append("static ");
                    case Final -> header.append("final ");
                    default -> { }
                }
            }
            header.append(method.getReturnTypeExpression() == null ? "void"
                            : method.getReturnTypeExpression().toString().trim())
                    .append(' ').append(method.getSimpleName()).append('(');
            final List<String> paramSources = new ArrayList<>();
            for (final Statement p : method.getParameters()) {
                if (p instanceof J.VariableDeclarations vd && !vd.getVariables().isEmpty()) {
                    paramSources.add(renderParam(vd));
                }
            }
            header.append(String.join(", ", paramSources)).append(") {\n");

            final StringBuilder body = new StringBuilder();
            final List<Statement> stmts = method.getBody().getStatements();
            boolean inserted = false;
            for (final Statement s : stmts) {
                if (plan.replacedStatements().contains(s)) {
                    if (!inserted) {
                        body.append("    ").append(plan.callSource()).append("\n");
                        inserted = true;
                    }
                    continue;
                }
                body.append("    ").append(printStatement(s)).append("\n");
            }
            return header + body.toString() + "}";
        }

        private CallerPlan tryBuildCallerPlan(final J.MethodDeclaration method) {
            if (method.getBody() == null) {
                return null;
            }
            final List<Statement> stmts = method.getBody().getStatements();
            final int listDeclIndex = indexOfErrorsListDecl(stmts);
            if (listDeclIndex < 0) {
                return null;
            }
            final J.VariableDeclarations listDecl = (J.VariableDeclarations) stmts.get(listDeclIndex);
            final String localErrorsName = listDecl.getVariables().getFirst().getSimpleName();

            final List<J.If> inlineAdds = new ArrayList<>();
            final List<Statement> replacedStatements = new ArrayList<>();
            for (int i = listDeclIndex + 1; i < stmts.size(); i++) {
                final Statement s = stmts.get(i);
                if (s instanceof J.If ifStmt && isInlineErrorsAdd(ifStmt, localErrorsName)) {
                    inlineAdds.add(ifStmt);
                    replacedStatements.add(s);
                    continue;
                }
                break;
            }
            if (inlineAdds.size() < MIN_INLINE_ADDS) {
                return null;
            }

            final Set<String> methodParamNames = new LinkedHashSet<>();
            method.getParameters().forEach(p -> {
                if (p instanceof J.VariableDeclarations vd && !vd.getVariables().isEmpty()) {
                    methodParamNames.add(vd.getVariables().getFirst().getSimpleName());
                }
            });

            final Set<String> validateBlockPrints = new LinkedHashSet<>();
            validateTarget.blocks().forEach(b -> validateBlockPrints.add(
                    normalise(b, validateTarget.errorsParamName(), localErrorsName)));
            final List<String> ifBlockSourcesToAppend = new ArrayList<>();
            final Map<String, MethodParam> paramsToAddByName = new LinkedHashMap<>();
            final Set<String> validateParamNames = new LinkedHashSet<>(validateTarget.paramNames());

            for (final J.If ifStmt : inlineAdds) {
                final String normalised = normalise(ifStmt, localErrorsName,
                        validateTarget.errorsParamName());
                final Set<String> referenced = referencedParams(ifStmt, methodParamNames);
                referenced.remove(localErrorsName);
                for (final String name : referenced) {
                    if (validateParamNames.contains(name)
                            || paramsToAddByName.containsKey(name)) {
                        continue;
                    }
                    final String typeName = renderParamTypeFromMethod(method, name);
                    if (typeName == null) {
                        return null;
                    }
                    paramsToAddByName.put(name, new MethodParam(name, typeName));
                }
                if (!validateBlockPrints.contains(normalised)) {
                    final String renamed = printStatement(ifStmt)
                            .replaceAll("\\b" + localErrorsName + "\\b",
                                    validateTarget.errorsParamName());
                    ifBlockSourcesToAppend.add(renamed);
                }
            }
            final List<String> callArgOrder = new ArrayList<>(validateTarget.paramNames());
            paramsToAddByName.keySet().forEach(callArgOrder::add);
            callArgOrder.add(localErrorsName);
            final String callSource = VALIDATE_METHOD_NAME
                    + "(" + String.join(", ", callArgOrder) + ");";
            return new CallerPlan(replacedStatements, callSource, ifBlockSourcesToAppend,
                    new ArrayList<>(paramsToAddByName.values()));
        }

        private String renderParamTypeFromMethod(final J.MethodDeclaration method,
                                                 final String paramName) {
            for (final Statement p : method.getParameters()) {
                if (p instanceof J.VariableDeclarations vd && !vd.getVariables().isEmpty()
                        && vd.getVariables().getFirst().getSimpleName().equals(paramName)
                        && vd.getTypeExpression() != null) {
                    return vd.getTypeExpression().toString().trim();
                }
            }
            return null;
        }

        private String printStatement(final Statement s) {
            final String raw = s.print(getCursor()).strip();
            // Statement-level print() drops the trailing semicolon for expression
            // and declaration statements (it belongs to the enclosing block).
            // When we reassemble the body as text we have to put it back.
            if (raw.endsWith(";") || raw.endsWith("}") || raw.endsWith("*/")) {
                return raw;
            }
            return raw + ";";
        }

        private String normalise(final Statement s, final String fromName, final String toName) {
            return s.print(getCursor())
                    .replaceAll("\\b" + fromName + "\\b", toName)
                    .replaceAll("\\s+", " ")
                    .strip();
        }

        private static int indexOfErrorsListDecl(final List<Statement> stmts) {
            for (int i = 0; i < stmts.size(); i++) {
                if (stmts.get(i) instanceof J.VariableDeclarations vd
                        && vd.getTypeExpression() != null
                        && vd.getTypeExpression().toString().trim().startsWith("List<")
                        && vd.getVariables().size() == 1
                        && vd.getVariables().getFirst().getInitializer() instanceof J.NewClass nc
                        && nc.getClazz() != null
                        && nc.getClazz().toString().trim().startsWith("ArrayList")) {
                    return i;
                }
            }
            return -1;
        }

        private static boolean isInlineErrorsAdd(final J.If ifStmt, final String localErrorsName) {
            if (!(ifStmt.getThenPart() instanceof J.Block thenBlock)) {
                return false;
            }
            if (thenBlock.getStatements().isEmpty()) {
                return false;
            }
            for (final Statement s : thenBlock.getStatements()) {
                if (!(s instanceof J.MethodInvocation mi)) {
                    return false;
                }
                if (!(mi.getSelect() instanceof J.Identifier sel)
                        || !sel.getSimpleName().equals(localErrorsName)
                        || !"add".equals(mi.getSimpleName())) {
                    return false;
                }
            }
            return true;
        }

        private static Set<String> referencedParams(final J.If ifStmt,
                                                    final Set<String> methodParamNames) {
            final Set<String> names = new LinkedHashSet<>();
            new JavaIsoVisitor<Set<String>>() {
                @Override
                public J.Identifier visitIdentifier(final J.Identifier ident,
                                                    final Set<String> acc) {
                    if (methodParamNames.contains(ident.getSimpleName())) {
                        acc.add(ident.getSimpleName());
                    }
                    return ident;
                }
            }.visit(ifStmt.getIfCondition(), names);
            return names;
        }

        private static String renderParam(final J.VariableDeclarations vd) {
            final StringBuilder sb = new StringBuilder();
            for (final J.Modifier mod : vd.getModifiers()) {
                if (mod.getType() == J.Modifier.Type.Final) {
                    sb.append("final ");
                }
            }
            sb.append(vd.getTypeExpression() == null ? "Object"
                            : vd.getTypeExpression().toString().trim())
                    .append(' ')
                    .append(vd.getVariables().getFirst().getSimpleName());
            return sb.toString();
        }

        private static ValidateTarget findValidate(final J.ClassDeclaration classDecl) {
            for (final Statement stmt : classDecl.getBody().getStatements()) {
                if (!(stmt instanceof J.MethodDeclaration method)
                        || !method.getSimpleName().equals(VALIDATE_METHOD_NAME)
                        || method.getReturnTypeExpression() == null
                        || !"void".equals(method.getReturnTypeExpression().toString().trim())) {
                    continue;
                }
                final List<Statement> params = method.getParameters();
                if (params.isEmpty()
                        || !(params.getLast() instanceof J.VariableDeclarations lastVd)
                        || lastVd.getTypeExpression() == null
                        || !lastVd.getTypeExpression().toString().trim().equals(LIST_STRING_RENDERED)
                        || lastVd.getVariables().isEmpty()) {
                    continue;
                }
                final String errorsParam = lastVd.getVariables().getFirst().getSimpleName();
                final List<String> paramNames = new ArrayList<>();
                for (int i = 0; i < params.size() - 1; i++) {
                    if (params.get(i) instanceof J.VariableDeclarations vd
                            && !vd.getVariables().isEmpty()) {
                        paramNames.add(vd.getVariables().getFirst().getSimpleName());
                    }
                }
                final List<Statement> blocks = method.getBody() == null
                        ? List.of() : method.getBody().getStatements();
                return new ValidateTarget(paramNames, errorsParam, blocks);
            }
            return null;
        }
    }

    private record ValidateTarget(List<String> paramNames, String errorsParamName,
                                  List<Statement> blocks) {}

    private record CallerPlan(List<Statement> replacedStatements,
                              String callSource,
                              List<String> ifBlockSourcesToAppend,
                              List<MethodParam> paramsToAdd) {}

    private record MethodParam(String name, String typeName) {
        String render() {
            return "final " + typeName + " " + name;
        }
    }
}
