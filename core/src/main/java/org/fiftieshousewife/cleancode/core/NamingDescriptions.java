package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class NamingDescriptions {

    private NamingDescriptions() {}

    static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.N1, "Choose Descriptive Names"),
            Map.entry(HeuristicCode.N2, "Choose Names at the Appropriate Level of Abstraction"),
            Map.entry(HeuristicCode.N3, "Use Standard Nomenclature Where Possible"),
            Map.entry(HeuristicCode.N4, "Unambiguous Names"),
            Map.entry(HeuristicCode.N5, "Use Long Names for Long Scopes"),
            Map.entry(HeuristicCode.N6, "Avoid Encodings"),
            Map.entry(HeuristicCode.N7, "Names Should Describe Side-Effects")
    );

    static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.N1,
                    "Choose names that reveal intention. The name of a variable, function, or " +
                    "class should answer the big questions: why it exists, what it does, and " +
                    "how it is used. If a name requires a comment, then the name is not revealing " +
                    "its intent."),
            Map.entry(HeuristicCode.N5,
                    "The length of a name should correspond to the size of its scope. A variable " +
                    "named 'i' is fine in a three-line for-loop. But a variable named 's' in a " +
                    "fifty-line method is a riddle. If the scope is long, the name should be long " +
                    "enough to be found, remembered, and understood without scrolling."),
            Map.entry(HeuristicCode.N6,
                    "In the days of early C and Fortran, encoding type information in names was " +
                    "necessary. Today, with modern languages and IDEs, Hungarian notation and " +
                    "type prefixes (strName, iCount, m_field) are nothing but noise. They make " +
                    "names harder to read and harder to change. Let the type system do its job."),
            Map.entry(HeuristicCode.N7,
                    "If a method has side effects, the name should describe them. A method named " +
                    "getPassword that also initialises the session is lying. It should be called " +
                    "getPasswordAndInitialiseSession — or better, split into two methods. If a " +
                    "reader must look at the implementation to discover a side effect, the name " +
                    "has broken its promise.")
    );

    static final Map<HeuristicCode, String> REFERENCES = Map.ofEntries(
            Map.entry(HeuristicCode.N1, "Clean Code Ch.17 'Smells and Heuristics — Names' p.309"),
            Map.entry(HeuristicCode.N2, "Clean Code Ch.17 'Smells and Heuristics — Names' p.311"),
            Map.entry(HeuristicCode.N3, "Clean Code Ch.17 'Smells and Heuristics — Names' p.311"),
            Map.entry(HeuristicCode.N4, "Clean Code Ch.17 'Smells and Heuristics — Names' p.312"),
            Map.entry(HeuristicCode.N5, "Clean Code Ch.17 'Smells and Heuristics — Names' p.312"),
            Map.entry(HeuristicCode.N6, "Clean Code Ch.17 'Smells and Heuristics — Names' p.312"),
            Map.entry(HeuristicCode.N7, "Clean Code Ch.17 'Smells and Heuristics — Names' p.313")
    );

    static final Map<HeuristicCode, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeuristicCode.N1, "Choose names that reveal intent."),
            Map.entry(HeuristicCode.N5, "Name length should match scope size. Single-char only in loops."),
            Map.entry(HeuristicCode.N6, "Drop type prefixes. Modern IDEs make them obsolete."),
            Map.entry(HeuristicCode.N7, "If a getter has side effects, the name is lying. Rename or split.")
    );
}
