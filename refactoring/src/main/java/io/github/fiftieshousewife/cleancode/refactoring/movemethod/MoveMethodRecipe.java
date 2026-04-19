package io.github.fiftieshousewife.cleancode.refactoring.movemethod;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * First-cut port of IntelliJ IDEA Community's {@code MoveInstanceMethodProcessor}
 * (and the static branch of {@code MoveStaticMemberHandler}), narrowed hard.
 *
 * <p>Given {@code (file, methodName, targetFqn)}, moves a single method
 * from one class to another and rewrites every call site. The v1 surface
 * only accepts {@code static} methods whose body does not reference any
 * other member of the originating class — the "pure utility" case. Once
 * there, the body compiles unchanged in its new home; call sites
 * previously qualified with the source class's simple name get
 * retargeted at the destination's simple name.
 *
 * <p>The recipe uses {@link ScanningRecipe} so that the scanning pass
 * can capture the method AST and validate preconditions before any
 * compilation unit is edited. See {@link MoveMethodScanner} for the
 * precondition checks and {@link MoveMethodApplier} for the edits.
 *
 * <p>Rejected (no-op) cases, each tracked as a backlog phase:
 * <ul>
 *   <li>Instance methods — needs receiver rewriting.</li>
 *   <li>Methods that reference sibling members of the source class —
 *       needs qualifier rewriting in the moved body.</li>
 *   <li>Name clashes on the target — needs conflict resolution.</li>
 *   <li>Visibility widening — we preserve the method's declared modifier.</li>
 * </ul>
 */
public class MoveMethodRecipe extends ScanningRecipe<MoveMethodState> {

    private final String file;
    private final String methodName;
    private final String targetFqn;

    @JsonCreator
    public MoveMethodRecipe(
            @JsonProperty("file") final String file,
            @JsonProperty("methodName") final String methodName,
            @JsonProperty("targetFqn") final String targetFqn) {
        this.file = file;
        this.methodName = methodName;
        this.targetFqn = targetFqn;
    }

    @Override
    public String getDisplayName() {
        return "Move a static method to another class";
    }

    @Override
    public String getDescription() {
        return "Moves a static method from its current class to the class named by targetFqn "
                + "and rewrites every `SourceClass.methodName(…)` call site to the new home. "
                + "Rejects when the method is non-static, references sibling members of the "
                + "source class, or the target class cannot be found. Fixes G14 / G17 / G18 "
                + "when paired with a heuristic-chosen target.";
    }

    @Override
    public MoveMethodState getInitialValue(final ExecutionContext ctx) {
        return new MoveMethodState();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final MoveMethodState acc) {
        return new MoveMethodScanner(acc, file, methodName);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final MoveMethodState acc) {
        if (acc.rejected || acc.captured == null) {
            return new JavaIsoVisitor<>() { };
        }
        return new MoveMethodApplier(acc, file, methodName, targetFqn);
    }
}
