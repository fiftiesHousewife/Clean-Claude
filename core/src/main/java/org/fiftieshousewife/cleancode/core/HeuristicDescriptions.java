package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

public final class HeuristicDescriptions {

    private HeuristicDescriptions() {}

    private static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.C1, "Inappropriate Information"),
            Map.entry(HeuristicCode.C2, "Obsolete Comment"),
            Map.entry(HeuristicCode.C3, "Redundant Comment"),
            Map.entry(HeuristicCode.C4, "Poorly Written Comment"),
            Map.entry(HeuristicCode.C5, "Commented-Out Code"),
            Map.entry(HeuristicCode.E1, "Build Requires More Than One Step"),
            Map.entry(HeuristicCode.E2, "Tests Require More Than One Step"),
            Map.entry(HeuristicCode.F1, "Too Many Arguments"),
            Map.entry(HeuristicCode.F2, "Output Arguments"),
            Map.entry(HeuristicCode.F3, "Flag Arguments"),
            Map.entry(HeuristicCode.F4, "Dead Function"),
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
            Map.entry(HeuristicCode.G16, "Obscured Intent"),
            Map.entry(HeuristicCode.G17, "Misplaced Responsibility"),
            Map.entry(HeuristicCode.G18, "Inappropriate Static"),
            Map.entry(HeuristicCode.G19, "Use Explanatory Variables"),
            Map.entry(HeuristicCode.G20, "Function Names Should Say What They Do"),
            Map.entry(HeuristicCode.G21, "Understand the Algorithm"),
            Map.entry(HeuristicCode.G22, "Make Logical Dependencies Physical"),
            Map.entry(HeuristicCode.G23, "Prefer Polymorphism to If/Else or Switch/Case"),
            Map.entry(HeuristicCode.G24, "Follow Standard Conventions"),
            Map.entry(HeuristicCode.G25, "Replace Magic Numbers with Named Constants"),
            Map.entry(HeuristicCode.G26, "Be Precise"),
            Map.entry(HeuristicCode.G27, "Structure over Convention"),
            Map.entry(HeuristicCode.G28, "Encapsulate Conditionals"),
            Map.entry(HeuristicCode.G29, "Avoid Negative Conditionals"),
            Map.entry(HeuristicCode.G30, "Functions Should Do One Thing"),
            Map.entry(HeuristicCode.G31, "Hidden Temporal Couplings"),
            Map.entry(HeuristicCode.G32, "Don't Be Arbitrary"),
            Map.entry(HeuristicCode.G33, "Encapsulate Boundary Conditions"),
            Map.entry(HeuristicCode.G34, "Functions Should Descend Only One Level of Abstraction"),
            Map.entry(HeuristicCode.G35, "Keep Configurable Data at High Levels"),
            Map.entry(HeuristicCode.G36, "Avoid Transitive Navigation"),
            Map.entry(HeuristicCode.J1, "Avoid Long Import Lists by Using Wildcards"),
            Map.entry(HeuristicCode.J2, "Don't Inherit Constants"),
            Map.entry(HeuristicCode.J3, "Constants versus Enums"),
            Map.entry(HeuristicCode.N1, "Choose Descriptive Names"),
            Map.entry(HeuristicCode.N2, "Choose Names at the Appropriate Level of Abstraction"),
            Map.entry(HeuristicCode.N3, "Use Standard Nomenclature Where Possible"),
            Map.entry(HeuristicCode.N4, "Unambiguous Names"),
            Map.entry(HeuristicCode.N5, "Use Long Names for Long Scopes"),
            Map.entry(HeuristicCode.N6, "Avoid Encodings"),
            Map.entry(HeuristicCode.N7, "Names Should Describe Side-Effects"),
            Map.entry(HeuristicCode.T1, "Insufficient Tests"),
            Map.entry(HeuristicCode.T2, "Use a Coverage Tool"),
            Map.entry(HeuristicCode.T3, "Don't Skip Trivial Tests"),
            Map.entry(HeuristicCode.T4, "An Ignored Test Is a Question about an Ambiguity"),
            Map.entry(HeuristicCode.T5, "Test Boundary Conditions"),
            Map.entry(HeuristicCode.T6, "Exhaustively Test Near Bugs"),
            Map.entry(HeuristicCode.T7, "Patterns of Failure Are Revealing"),
            Map.entry(HeuristicCode.T8, "Test Coverage Patterns Can Be Revealing"),
            Map.entry(HeuristicCode.T9, "Tests Should Be Fast"),
            Map.entry(HeuristicCode.Ch3_1, "Small Functions"),
            Map.entry(HeuristicCode.Ch3_2, "Do One Thing"),
            Map.entry(HeuristicCode.Ch3_3, "One Level of Abstraction per Function"),
            Map.entry(HeuristicCode.Ch6_1, "Data/Object Anti-Symmetry"),
            Map.entry(HeuristicCode.Ch7_1, "Use Exceptions Rather Than Return Codes"),
            Map.entry(HeuristicCode.Ch7_2, "Don't Return Null"),
            Map.entry(HeuristicCode.Ch10_1, "Classes Should Be Small"),
            Map.entry(HeuristicCode.Ch10_2, "The Single Responsibility Principle")
    );

    private static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.C3, "A comment is redundant if it describes something that adequately describes itself. Remove it."),
            Map.entry(HeuristicCode.C5, "Commented-out code rots. It confuses readers who wonder why it's there and whether it's important. Delete it — source control remembers."),
            Map.entry(HeuristicCode.E1, "Every dependency should be up to date. Outdated libraries accumulate risk silently."),
            Map.entry(HeuristicCode.F2, "Output arguments are counterintuitive. Readers expect arguments to be inputs, not outputs. Return the result instead."),
            Map.entry(HeuristicCode.F3, "Boolean arguments loudly declare that a function does more than one thing. Split it into two functions."),
            Map.entry(HeuristicCode.G9, "Dead code is baggage. If it's not called, delete it. If you need it later, source control has it."),
            Map.entry(HeuristicCode.G10, "A variable should be declared as close to its usage as possible. Vertical separation forces readers to scroll."),
            Map.entry(HeuristicCode.G20, "If you have to look at the implementation to know what a function does, the name is wrong."),
            Map.entry(HeuristicCode.G22, "Variables that never change should be declared final. This communicates intent and prevents accidental reassignment."),
            Map.entry(HeuristicCode.G23, "When you see a switch or if/else chain that selects behaviour based on type, think polymorphism. One switch is one too many."),
            Map.entry(HeuristicCode.G28, "Boolean logic is hard to understand in the context of an if statement. Extract the logic into well-named methods."),
            Map.entry(HeuristicCode.G36, "Write shy code. A module should know only its immediate collaborators, never the internals of its collaborators' collaborators."),
            Map.entry(HeuristicCode.J1, "Long import lists signal a class that depends on too much. Consider whether the class has too many responsibilities."),
            Map.entry(HeuristicCode.J2, "Inheriting constants from an interface is a deception. Use a static import or a constants class instead."),
            Map.entry(HeuristicCode.J3, "Now that Java has enums, use them. Don't use public static final ints or Strings as enumerated types."),
            Map.entry(HeuristicCode.N6, "Encoding type or scope information in names adds noise. Modern IDEs make Hungarian notation obsolete."),
            Map.entry(HeuristicCode.Ch7_1, "Catch blocks that only log — or worse, are empty — silently swallow errors. The caller never knows the operation failed."),
            Map.entry(HeuristicCode.Ch7_2, "Code peppered with null checks is a sign of poor error handling. Use Optional, fail fast, or fix the source of nulls."),
            Map.entry(HeuristicCode.Ch10_1, "A class that is too long is probably doing too many things. Extract responsibilities into well-named collaborators."),
            Map.entry(HeuristicCode.Ch10_2, "A record with many components is a data clump. Group related fields into smaller, meaningful records."),
            Map.entry(HeuristicCode.G34, "Section comments are a sign that the function mixes levels of abstraction. Extract sections into their own methods."),
            Map.entry(HeuristicCode.T1, "A test suite is insufficient if there are conditions or calculations that have not been explored. Coverage tells you where to look."),
            Map.entry(HeuristicCode.T3, "A disabled test is a question about an ambiguity. Either the code or the test is wrong — find out which and fix it.")
    );

    public static String name(HeuristicCode code) {
        return NAMES.getOrDefault(code, code.name());
    }

    public static String guidance(HeuristicCode code) {
        return GUIDANCE.get(code);
    }
}
