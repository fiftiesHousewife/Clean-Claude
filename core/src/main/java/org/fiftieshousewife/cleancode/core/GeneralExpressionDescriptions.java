package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class GeneralExpressionDescriptions {

    private GeneralExpressionDescriptions() {}

    static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.G16, "Obscured Intent"),
            Map.entry(HeuristicCode.G19, "Use Explanatory Variables"),
            Map.entry(HeuristicCode.G20, "Function Names Should Say What They Do"),
            Map.entry(HeuristicCode.G21, "Understand the Algorithm"),
            Map.entry(HeuristicCode.G22, "Make Logical Dependencies Physical"),
            Map.entry(HeuristicCode.G24, "Follow Standard Conventions"),
            Map.entry(HeuristicCode.G25, "Replace Magic Numbers with Named Constants"),
            Map.entry(HeuristicCode.G26, "Be Precise"),
            Map.entry(HeuristicCode.G27, "Structure over Convention")
    );

    static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.G16,
                    "Obscured intent is the opposite of expressiveness. Code that uses nested " +
                    "ternaries, overly terse variable names, or magic expressions is prioritising " +
                    "brevity over clarity. The reader shouldn't need a debugger to understand what " +
                    "a line of code does. If you're impressed by how cleverly terse your code is, " +
                    "that's a bad sign."),
            Map.entry(HeuristicCode.G19,
                    "Complex expressions should be broken into intermediate variables with " +
                    "explanatory names. 'wasPressed' is better than 'event.getTarget().isButton() " +
                    "&& event.getState() == CLICKED'. The intermediate variable documents the " +
                    "programmer's intent and makes the code read like a well-written paragraph."),
            Map.entry(HeuristicCode.G20,
                    "If you have to look at the implementation of a function to know what it does, " +
                    "then you should work to find a better name, or rearrange the functionality so " +
                    "that it can be placed in a function with a better name. The name of a function " +
                    "should tell you exactly what it does, unambiguously, without surprises."),
            Map.entry(HeuristicCode.G22,
                    "If something in your code can logically be constant — a variable that's never " +
                    "reassigned, an object that's never swapped — then declare it final. This isn't " +
                    "pedantry. It communicates intent to every future reader: 'this value is set " +
                    "once and never changes.' It prevents accidental reassignment and signals a " +
                    "design where fewer things are moving at once."),
            Map.entry(HeuristicCode.G24,
                    "Every team should follow a coding standard. The standard should specify things " +
                    "like where to declare instance variables, consistent naming, brace style, and " +
                    "so forth. The standard should not need a document to describe it because the " +
                    "code itself should be the exemplar."),
            Map.entry(HeuristicCode.G25,
                    "In general it is a bad idea to have raw numbers in your code. Numbers like 42 " +
                    "or 86400 are magic — they have no context, no meaning, no documentation. " +
                    "Hide them behind well-named constants. SECONDS_PER_DAY is immediately clear; " +
                    "86400 is not."),
            Map.entry(HeuristicCode.G26,
                    "Ambiguity in code is a sign of insufficient care. When you make a decision in " +
                    "code, make it precisely. Know why you've made it and how you will deal with " +
                    "any exceptions. Don't be lazy about the precision of your decisions.")
    );

    static final Map<HeuristicCode, String> REFERENCES = Map.ofEntries(
            Map.entry(HeuristicCode.G16, "Clean Code Ch.17 'Smells and Heuristics — General' p.295"),
            Map.entry(HeuristicCode.G19, "Clean Code Ch.17 'Smells and Heuristics — General' p.296"),
            Map.entry(HeuristicCode.G20, "Clean Code Ch.17 'Smells and Heuristics — General' p.297"),
            Map.entry(HeuristicCode.G21, "Clean Code Ch.17 'Smells and Heuristics — General' p.297"),
            Map.entry(HeuristicCode.G22, "Clean Code Ch.17 'Smells and Heuristics — General' p.298"),
            Map.entry(HeuristicCode.G24, "Clean Code Ch.17 'Smells and Heuristics — General' p.299"),
            Map.entry(HeuristicCode.G25, "Clean Code Ch.17 'Smells and Heuristics — General' p.300"),
            Map.entry(HeuristicCode.G26, "Clean Code Ch.17 'Smells and Heuristics — General' p.301"),
            Map.entry(HeuristicCode.G27, "Clean Code Ch.17 'Smells and Heuristics — General' p.301")
    );

    static final Map<HeuristicCode, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeuristicCode.G16, "Nested ternaries obscure intent. Use if/else or a named method."),
            Map.entry(HeuristicCode.G19, "Extract complex expressions to named intermediate variables."),
            Map.entry(HeuristicCode.G20, "If you must read the body to know what it does, rename it."),
            Map.entry(HeuristicCode.G22, "Declare variables final when they don't change."),
            Map.entry(HeuristicCode.G24, "Follow the project's formatting conventions."),
            Map.entry(HeuristicCode.G25, "Extract repeated literals to named constants."),
            Map.entry(HeuristicCode.G26, "Use precise types — BigDecimal for money, LocalDate for dates.")
    );
}
