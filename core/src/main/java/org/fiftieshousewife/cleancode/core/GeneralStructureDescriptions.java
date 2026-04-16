package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class GeneralStructureDescriptions {

    private GeneralStructureDescriptions() {}

    static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.G1, "Multiple Languages in One Source File"),
            Map.entry(HeuristicCode.G2, "Obvious Behaviour Is Unimplemented"),
            Map.entry(HeuristicCode.G3, "Incorrect Behaviour at the Boundaries"),
            Map.entry(HeuristicCode.G4, "Overridden Safeties"),
            Map.entry(HeuristicCode.G5, "Duplication"),
            Map.entry(HeuristicCode.G6, "Code at Wrong Level of Abstraction"),
            Map.entry(HeuristicCode.G7, "Base Classes Depending on Their Derivatives"),
            Map.entry(HeuristicCode.G8, "Too Much Information"),
            Map.entry(HeuristicCode.G9, "Dead Code"),
            Map.entry(HeuristicCode.G10, "Vertical Separation"),
            Map.entry(HeuristicCode.G11, "Inconsistency"),
            Map.entry(HeuristicCode.G12, "Clutter"),
            Map.entry(HeuristicCode.G13, "Artificial Coupling"),
            Map.entry(HeuristicCode.G14, "Feature Envy"),
            Map.entry(HeuristicCode.G15, "Selector Arguments"),
            Map.entry(HeuristicCode.G17, "Misplaced Responsibility"),
            Map.entry(HeuristicCode.G18, "Inappropriate Static")
    );

    static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.G1,
                    "A source file should contain one, and only one, language. A Java file " +
                    "with embedded HTML, CSS, or SQL is harder to read, harder to search, and " +
                    "impossible to validate with language-specific tools. Extract the other " +
                    "language into a template file, a resource, or a separate class that " +
                    "encapsulates the generation."),
            Map.entry(HeuristicCode.G4,
                    "Don't override safeties. Turning off warnings, ignoring failing tests, " +
                    "catching and discarding exceptions — these are all the same mistake. " +
                    "Safeties exist because someone, at some point, decided they were important " +
                    "enough to create. Don't silence them; fix the underlying problem."),
            Map.entry(HeuristicCode.G5,
                    "Duplication is the root of all evil in software. Every duplication represents " +
                    "a missed opportunity for abstraction. When you see duplicated code, it almost " +
                    "always means there is a concept crying out to be a subroutine or a class. " +
                    "Find it and eliminate the duplication."),
            Map.entry(HeuristicCode.G8,
                    "Well-defined modules have very small interfaces that allow you to do a lot " +
                    "with a little. A class with too many public methods, too many fields, or too " +
                    "many dependencies has exposed too much of itself. Keep interfaces tight. " +
                    "Fewer things to know means fewer things to go wrong."),
            Map.entry(HeuristicCode.G9,
                    "Dead code is code that isn't executed. It's the body of an if statement that " +
                    "checks for a condition that can't happen. It's the catch block for an " +
                    "exception that is never thrown. It's the utility method that is never called. " +
                    "Dead code is not completely updated when designs change. It rots. Delete it."),
            Map.entry(HeuristicCode.G10,
                    "Local variables should be declared just before their first use. Vertical " +
                    "separation between declaration and use makes the reader hold a mental " +
                    "placeholder — 'what was that variable for again?' — while reading unrelated " +
                    "code. Declare variables at the point of need, not at the top of the method."),
            Map.entry(HeuristicCode.G11,
                    "If you do something a certain way, do all similar things the same way. If " +
                    "you name one method fetchUsers, don't name a similar method getOrders and " +
                    "another retrieveProducts. Inconsistency breeds confusion — the reader must " +
                    "wonder whether the different names signify different semantics when they don't."),
            Map.entry(HeuristicCode.G12,
                    "Clutter is anything that adds noise without adding value: unused variables, " +
                    "never-called functions, redundant imports, purposeless comments. Keep your " +
                    "source files clean. A lean source file is easier to read, easier to " +
                    "understand, and easier to change."),
            Map.entry(HeuristicCode.G14,
                    "A method that calls six methods on another object but only one on its own " +
                    "class has Feature Envy — it clearly wants to be over there, not here. Move " +
                    "the method to the class whose data it is actually manipulating. Methods should " +
                    "operate on the data of their own class, not reach across into another."),
            Map.entry(HeuristicCode.G17,
                    "One of the most important decisions a developer makes is where to put code. " +
                    "A data class is a class with public fields and no real behaviour — it " +
                    "exposes its internals and delegates all responsibility elsewhere. Ask: " +
                    "does this behaviour belong here, or is it misplaced?"),
            Map.entry(HeuristicCode.G18,
                    "In general you should prefer non-static methods to static methods. If you " +
                    "really want a function to be static, make sure there is no chance that you'll " +
                    "want it to behave polymorphically. Static methods cannot be overridden and " +
                    "cannot participate in dependency injection.")
    );

    static final Map<HeuristicCode, String> REFERENCES = Map.ofEntries(
            Map.entry(HeuristicCode.G1, "Clean Code Ch.17 'Smells and Heuristics — General' p.288"),
            Map.entry(HeuristicCode.G2, "Clean Code Ch.17 'Smells and Heuristics — General' p.288"),
            Map.entry(HeuristicCode.G3, "Clean Code Ch.17 'Smells and Heuristics — General' p.289"),
            Map.entry(HeuristicCode.G4, "Clean Code Ch.17 'Smells and Heuristics — General' p.289"),
            Map.entry(HeuristicCode.G5, "Clean Code Ch.17 'Smells and Heuristics — General' p.289"),
            Map.entry(HeuristicCode.G6, "Clean Code Ch.17 'Smells and Heuristics — General' p.290"),
            Map.entry(HeuristicCode.G7, "Clean Code Ch.17 'Smells and Heuristics — General' p.291"),
            Map.entry(HeuristicCode.G8, "Clean Code Ch.17 'Smells and Heuristics — General' p.291"),
            Map.entry(HeuristicCode.G9, "Clean Code Ch.17 'Smells and Heuristics — General' p.292"),
            Map.entry(HeuristicCode.G10, "Clean Code Ch.17 'Smells and Heuristics — General' p.292"),
            Map.entry(HeuristicCode.G11, "Clean Code Ch.17 'Smells and Heuristics — General' p.292"),
            Map.entry(HeuristicCode.G12, "Clean Code Ch.17 'Smells and Heuristics — General' p.293"),
            Map.entry(HeuristicCode.G13, "Clean Code Ch.17 'Smells and Heuristics — General' p.293"),
            Map.entry(HeuristicCode.G14, "Clean Code Ch.17 'Smells and Heuristics — General' p.293"),
            Map.entry(HeuristicCode.G15, "Clean Code Ch.17 'Smells and Heuristics — General' p.294"),
            Map.entry(HeuristicCode.G17, "Clean Code Ch.17 'Smells and Heuristics — General' p.295"),
            Map.entry(HeuristicCode.G18, "Clean Code Ch.17 'Smells and Heuristics — General' p.296")
    );

    static final Map<HeuristicCode, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeuristicCode.G1, "Extract embedded HTML, SQL, or CSS into templates or resource files."),
            Map.entry(HeuristicCode.G4, "Don't suppress warnings. Fix the underlying issue."),
            Map.entry(HeuristicCode.G5, "Eliminate duplication — extract the shared logic."),
            Map.entry(HeuristicCode.G8, "Reduce public surface. Keep interfaces tight."),
            Map.entry(HeuristicCode.G9, "Delete dead code."),
            Map.entry(HeuristicCode.G10, "Declare variables just before their first use."),
            Map.entry(HeuristicCode.G11, "Pick one verb for each concept and use it consistently."),
            Map.entry(HeuristicCode.G12, "Delete unused imports, empty statements, and noise."),
            Map.entry(HeuristicCode.G14, "Move the method to the class whose data it uses.")
    );
}
