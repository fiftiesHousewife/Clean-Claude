package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class JavaDescriptions {

    private JavaDescriptions() {}

    static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.J1, "Avoid Long Import Lists by Using Wildcards"),
            Map.entry(HeuristicCode.J2, "Don't Inherit Constants"),
            Map.entry(HeuristicCode.J3, "Constants versus Enums")
    );

    static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.J1,
                    "Long lists of imports are daunting to the reader. If you're importing from " +
                    "many different packages, it's worth asking whether this class has too many " +
                    "responsibilities. Wildcard imports can reduce clutter, but the real fix is " +
                    "often to split the class."),
            Map.entry(HeuristicCode.J2,
                    "Implementing an interface just to gain convenient access to its constants " +
                    "is a terrible practice. The constants become part of your class's public API, " +
                    "polluting the namespace. Use static imports to access constants, or place " +
                    "constants in a class or enum where they naturally belong."),
            Map.entry(HeuristicCode.J3,
                    "Now that Java has enums, use them. The old pattern of public static final " +
                    "int or String constants as an enumeration is obsolete. Enums provide type " +
                    "safety, can have methods and fields, and make switch statements exhaustive. " +
                    "Don't cling to the pre-Java 5 idiom.")
    );

    static final Map<HeuristicCode, String> REFERENCES = Map.ofEntries(
            Map.entry(HeuristicCode.J1, "Clean Code Ch.17 'Smells and Heuristics — Java' p.307"),
            Map.entry(HeuristicCode.J2, "Clean Code Ch.17 'Smells and Heuristics — Java' p.307"),
            Map.entry(HeuristicCode.J3, "Clean Code Ch.17 'Smells and Heuristics — Java' p.308")
    );

    static final Map<HeuristicCode, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeuristicCode.J1, "Replace wildcard imports with explicit ones."),
            Map.entry(HeuristicCode.J2, "Use static imports instead of inheriting from a constants interface."),
            Map.entry(HeuristicCode.J3, "Use enums instead of static final constant groups.")
    );
}
