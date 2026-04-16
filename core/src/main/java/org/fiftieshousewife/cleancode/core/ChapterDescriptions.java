package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class ChapterDescriptions {

    private ChapterDescriptions() {}

    static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.Ch3_1, "Small Functions"),
            Map.entry(HeuristicCode.Ch3_2, "Do One Thing"),
            Map.entry(HeuristicCode.Ch3_3, "One Level of Abstraction per Function"),
            Map.entry(HeuristicCode.Ch6_1, "Data/Object Anti-Symmetry"),
            Map.entry(HeuristicCode.Ch7_1, "Use Exceptions Rather Than Return Codes"),
            Map.entry(HeuristicCode.Ch7_2, "Don't Return Null"),
            Map.entry(HeuristicCode.Ch10_1, "Classes Should Be Small"),
            Map.entry(HeuristicCode.Ch10_2, "The Single Responsibility Principle")
    );

    static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.Ch3_1,
                    "The first rule of functions is that they should be small. The second rule " +
                    "is that they should be smaller than that. Functions should hardly ever be " +
                    "20 lines long. Each function should tell a story, and each line should lead " +
                    "you naturally to the next in a compelling order."),
            Map.entry(HeuristicCode.Ch7_1,
                    "Exceptions are for exceptional circumstances. When you catch an exception " +
                    "and merely log it — or worse, leave the catch block empty — you've told the " +
                    "calling code that everything is fine when it isn't. The caller makes " +
                    "decisions based on a lie. Either handle the exception meaningfully, or let " +
                    "it propagate to someone who can."),
            Map.entry(HeuristicCode.Ch7_2,
                    "When you return null, you are creating work for your callers. Every caller " +
                    "must check for null, and if even one forgets, the application blows up with " +
                    "a NullPointerException at some unexpected point. Code peppered with null " +
                    "checks is noisy, hard to read, and fragile. Use Optional, throw an exception, " +
                    "or return a Special Case object instead."),
            Map.entry(HeuristicCode.Ch10_1,
                    "The first rule of classes is that they should be small. The second rule is " +
                    "that they should be smaller than that. With functions, we measured size by " +
                    "counting physical lines. With classes, we use a different measure: " +
                    "responsibilities. A class should have one, and only one, reason to change. " +
                    "If you need the word 'and' to describe what a class does, it's too big."),
            Map.entry(HeuristicCode.Ch10_2,
                    "A record or data structure with many components is a signal that it's carrying " +
                    "too many responsibilities, or that it's a data clump — a group of fields that " +
                    "always travel together and should be extracted into their own meaningful type. " +
                    "Five is a reasonable upper bound; beyond that, ask what smaller structures " +
                    "are hiding inside.")
    );

    static final Map<HeuristicCode, String> REFERENCES = Map.ofEntries(
            Map.entry(HeuristicCode.Ch3_1, "Clean Code Ch.3 'Functions' p.34"),
            Map.entry(HeuristicCode.Ch3_2, "Clean Code Ch.3 'Functions' p.35"),
            Map.entry(HeuristicCode.Ch3_3, "Clean Code Ch.3 'Functions' p.36"),
            Map.entry(HeuristicCode.Ch6_1, "Clean Code Ch.6 'Objects and Data Structures' p.95"),
            Map.entry(HeuristicCode.Ch7_1, "Clean Code Ch.7 'Error Handling' p.103"),
            Map.entry(HeuristicCode.Ch7_2, "Clean Code Ch.7 'Error Handling' p.110"),
            Map.entry(HeuristicCode.Ch10_1, "Clean Code Ch.10 'Classes' p.136"),
            Map.entry(HeuristicCode.Ch10_2, "Clean Code Ch.10 'Classes' p.138")
    );

    static final Map<HeuristicCode, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeuristicCode.Ch3_1, "Non-trivial private methods should be package-private and tested."),
            Map.entry(HeuristicCode.Ch7_1, "Don't catch and log. Propagate with context."),
            Map.entry(HeuristicCode.Ch7_2, "Don't return or check for null. Use Optional or fail fast."),
            Map.entry(HeuristicCode.Ch10_1, "Classes with more than one responsibility are too big. Split."),
            Map.entry(HeuristicCode.Ch10_2, "Large records are data clumps. Group fields into smaller types.")
    );
}
