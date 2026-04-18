package org.fiftieshousewife.cleancode.refactoring.extractmethod;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * Splices the two synthesised fragments (call statement and new method
 * declaration) back into the compilation unit. Kept separate from the
 * recipe so the "how do we rewrite" decisions live next to each other:
 * where the call goes, where the new method goes, whose prefix to copy.
 */
final class ExtractionRewrite {

    private ExtractionRewrite() {}

    static J.CompilationUnit apply(final J.CompilationUnit cu,
                                   final ExtractionTarget target,
                                   final J.MethodDeclaration newMethod,
                                   final Statement callStatement) {
        final Statement placedCall = copyPrefixFromFirstExtracted(target, callStatement);
        final J.Block updatedBody = target.body().withStatements(replaceRangeWith(target, placedCall));
        final J.MethodDeclaration updatedMethod = target.enclosingMethod().withBody(updatedBody);
        final J.Block updatedClassBody = target.enclosingClass().getBody().withStatements(
                insertAfter(target.enclosingClass().getBody().getStatements(),
                        target.enclosingMethod(), updatedMethod, newMethod));
        final J.ClassDeclaration updatedClass = target.enclosingClass().withBody(updatedClassBody);
        final List<J.ClassDeclaration> newClasses = new ArrayList<>(cu.getClasses());
        newClasses.replaceAll(c -> c == target.enclosingClass() ? updatedClass : c);
        return cu.withClasses(newClasses);
    }

    private static Statement copyPrefixFromFirstExtracted(final ExtractionTarget target,
                                                          final Statement call) {
        final List<Statement> extracted = target.extractedStatements();
        return extracted.isEmpty() ? call : call.withPrefix(extracted.getFirst().getPrefix());
    }

    private static List<Statement> replaceRangeWith(final ExtractionTarget target,
                                                    final Statement callStatement) {
        final List<Statement> rebuilt = new ArrayList<>(target.statementsBeforeRange());
        rebuilt.add(callStatement);
        rebuilt.addAll(target.statementsAfterRange());
        return rebuilt;
    }

    private static List<Statement> insertAfter(final List<Statement> original,
                                               final J.MethodDeclaration target,
                                               final J.MethodDeclaration replacement,
                                               final J.MethodDeclaration addition) {
        final List<Statement> updated = new ArrayList<>(original.size() + 1);
        original.forEach(s -> {
            updated.add(s == target ? replacement : s);
            if (s == target) {
                updated.add(addition);
            }
        });
        return updated;
    }
}
