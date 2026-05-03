package io.github.fiftieshousewife.cleancode.recipes.support;

import org.openrewrite.Cursor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Statement;

import java.util.List;

public final class BoilerplateMethodSkip {

    private static final MethodMatcher EQUALS =
            new MethodMatcher("java.lang.Object equals(java.lang.Object)", true);
    private static final MethodMatcher HASH_CODE =
            new MethodMatcher("java.lang.Object hashCode()", true);
    private static final MethodMatcher COMPARE_TO =
            new MethodMatcher("java.lang.Comparable compareTo(..)", true);

    private BoilerplateMethodSkip() {}

    public static boolean isContractMethod(final J.MethodDeclaration method) {
        return isEqualsOrHashCode(method)
                || isCompareTo(method)
                || isOptionAnnotated(method);
    }

    public static boolean isContractMethod(final J.MethodDeclaration method, final Cursor cursor) {
        if (isContractMethod(method)) {
            return true;
        }
        return method.isConstructor() && enclosingClassHasOptionField(cursor);
    }

    private static boolean enclosingClassHasOptionField(final Cursor cursor) {
        final J.ClassDeclaration cls = cursor.firstEnclosing(J.ClassDeclaration.class);
        if (cls == null || cls.getBody() == null) {
            return false;
        }
        for (final Statement s : cls.getBody().getStatements()) {
            if (s instanceof J.VariableDeclarations vd
                    && hasOptionAnnotation(vd.getLeadingAnnotations())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEqualsOrHashCode(final J.MethodDeclaration method) {
        final JavaType.Method type = method.getMethodType();
        if (type != null && (EQUALS.matches(type) || HASH_CODE.matches(type))) {
            return true;
        }
        return matchesEqualsOrHashCodeShape(method);
    }

    public static boolean isCompareTo(final J.MethodDeclaration method) {
        final JavaType.Method type = method.getMethodType();
        if (type != null && COMPARE_TO.matches(type)) {
            return true;
        }
        return matchesCompareToShape(method);
    }

    public static boolean isOptionAnnotated(final J.MethodDeclaration method) {
        if (hasOptionAnnotation(method.getLeadingAnnotations())) {
            return true;
        }
        for (final Statement p : method.getParameters()) {
            if (p instanceof J.VariableDeclarations vd
                    && hasOptionAnnotation(vd.getLeadingAnnotations())) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the method carries {@code @Override}, i.e. its signature
     * is dictated by a supertype contract. Use this to skip findings
     * whose remediation would require changing the signature: the
     * caller can't do that without breaking the override.
     */
    public static boolean isOverride(final J.MethodDeclaration method) {
        final List<J.Annotation> annotations = method.getLeadingAnnotations();
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }
        for (final J.Annotation a : annotations) {
            if ("Override".equals(simpleNameOf(a))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesEqualsOrHashCodeShape(final J.MethodDeclaration method) {
        final String name = method.getSimpleName();
        if ("hashCode".equals(name)) {
            return hasNoParams(method);
        }
        if ("equals".equals(name)) {
            return hasSingleObjectParam(method);
        }
        return false;
    }

    private static boolean matchesCompareToShape(final J.MethodDeclaration method) {
        if (!"compareTo".equals(method.getSimpleName())) {
            return false;
        }
        final List<Statement> params = method.getParameters();
        return params.size() == 1 && params.get(0) instanceof J.VariableDeclarations;
    }

    private static boolean hasNoParams(final J.MethodDeclaration method) {
        final List<Statement> params = method.getParameters();
        return params.isEmpty() || (params.size() == 1 && params.get(0) instanceof J.Empty);
    }

    private static boolean hasSingleObjectParam(final J.MethodDeclaration method) {
        final List<Statement> params = method.getParameters();
        if (params.size() != 1 || !(params.get(0) instanceof J.VariableDeclarations vd)) {
            return false;
        }
        if (vd.getType() instanceof JavaType.FullyQualified fq) {
            return "java.lang.Object".equals(fq.getFullyQualifiedName());
        }
        if (vd.getTypeExpression() != null) {
            final String typeName = vd.getTypeExpression().toString().trim();
            return "Object".equals(typeName) || "java.lang.Object".equals(typeName);
        }
        return false;
    }

    private static boolean hasOptionAnnotation(final List<J.Annotation> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }
        for (final J.Annotation a : annotations) {
            if ("Option".equals(simpleNameOf(a))) {
                return true;
            }
        }
        return false;
    }

    private static String simpleNameOf(final J.Annotation annotation) {
        final NameTree type = annotation.getAnnotationType();
        if (type instanceof J.Identifier id) {
            return id.getSimpleName();
        }
        if (type instanceof J.FieldAccess fa) {
            return fa.getSimpleName();
        }
        return null;
    }
}
