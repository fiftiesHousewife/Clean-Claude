package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class FunctionDescriptions {

    private FunctionDescriptions() {}

    static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.F1, "Too Many Arguments"),
            Map.entry(HeuristicCode.F2, "Output Arguments"),
            Map.entry(HeuristicCode.F3, "Flag Arguments"),
            Map.entry(HeuristicCode.F4, "Dead Function")
    );

    static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.F1,
                    "Functions should have a small number of arguments. The ideal number is zero. " +
                    "Next comes one, followed closely by two. Three arguments should be avoided " +
                    "where possible. More than three requires very special justification — and " +
                    "then shouldn't be used anyway."),
            Map.entry(HeuristicCode.F2,
                    "Output arguments are counterintuitive. Readers expect arguments to be inputs " +
                    "to a function, not things the function writes to. When you see " +
                    "appendFooter(report), do you expect it to append something to report, or " +
                    "to append report to something else? If a function must transform something, " +
                    "let it transform the state of the owning object, or return the result."),
            Map.entry(HeuristicCode.F3,
                    "Boolean arguments loudly declare that the function does more than one thing. " +
                    "It does one thing if the flag is true and another if the flag is false. The " +
                    "function should be split into two: one for each path. render(true) tells the " +
                    "reader nothing — renderForSuite() and renderForSingleTest() tell them " +
                    "everything."),
            Map.entry(HeuristicCode.F4,
                    "Methods that are never called are dead code. They clog the class, confuse " +
                    "the reader, and add maintenance burden. Delete them without hesitation — " +
                    "your source control system remembers them if you ever need them back.")
    );

    static final Map<HeuristicCode, String> REFERENCES = Map.ofEntries(
            Map.entry(HeuristicCode.F1, "Clean Code Ch.17 'Smells and Heuristics — Functions' p.288"),
            Map.entry(HeuristicCode.F2, "Clean Code Ch.17 'Smells and Heuristics — Functions' p.288"),
            Map.entry(HeuristicCode.F3, "Clean Code Ch.17 'Smells and Heuristics — Functions' p.288"),
            Map.entry(HeuristicCode.F4, "Clean Code Ch.17 'Smells and Heuristics — Functions' p.288")
    );

    static final Map<HeuristicCode, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeuristicCode.F1, "Reduce arguments — three is the practical maximum."),
            Map.entry(HeuristicCode.F2, "Don't mutate arguments. Return the result."),
            Map.entry(HeuristicCode.F3, "Split boolean-parameterised methods into two."),
            Map.entry(HeuristicCode.F4, "Delete uncalled methods.")
    );
}
