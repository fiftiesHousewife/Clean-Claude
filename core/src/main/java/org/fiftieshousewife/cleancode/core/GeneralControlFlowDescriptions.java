package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class GeneralControlFlowDescriptions {

    private GeneralControlFlowDescriptions() {}

    static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.G23, "Prefer Polymorphism to If/Else or Switch/Case"),
            Map.entry(HeuristicCode.G28, "Encapsulate Conditionals"),
            Map.entry(HeuristicCode.G29, "Avoid Negative Conditionals"),
            Map.entry(HeuristicCode.G30, "Functions Should Do One Thing"),
            Map.entry(HeuristicCode.G31, "Hidden Temporal Couplings"),
            Map.entry(HeuristicCode.G32, "Don't Be Arbitrary"),
            Map.entry(HeuristicCode.G33, "Encapsulate Boundary Conditions"),
            Map.entry(HeuristicCode.G34, "Functions Should Descend Only One Level of Abstraction"),
            Map.entry(HeuristicCode.G35, "Keep Configurable Data at High Levels"),
            Map.entry(HeuristicCode.G36, "Avoid Transitive Navigation")
    );

    static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.G23,
                    "When you see code that tests for a type to decide what behaviour to invoke, " +
                    "consider replacing it with polymorphism. 'One switch' is a reasonable rule of " +
                    "thumb — if you find yourself writing the same switch in multiple places, the " +
                    "switch is telling you that there's a class hierarchy hiding in your code, " +
                    "waiting to be discovered."),
            Map.entry(HeuristicCode.G28,
                    "Boolean logic is hard enough to understand without reading it in the context " +
                    "of an if or while statement. Extract functions that explain the intent of the " +
                    "conditional. if (shouldBeDeleted(timer)) is vastly preferable to " +
                    "if (timer.hasExpired() && !timer.isRecurrent())."),
            Map.entry(HeuristicCode.G29,
                    "Negatives are slightly harder to understand than positives. So, when " +
                    "possible, conditionals should be expressed as positives. " +
                    "if (buffer.shouldCompact()) is preferable to if (!buffer.shouldNotCompact()). " +
                    "Double negation forces the reader to do mental gymnastics that add nothing."),
            Map.entry(HeuristicCode.G30,
                    "Functions should do one thing. They should do it well. They should do it only. " +
                    "A function that is too long is probably doing too many things. If you can " +
                    "extract another function from it with a name that is not merely a restatement " +
                    "of its implementation, then the original is doing more than one thing."),
            Map.entry(HeuristicCode.G33,
                    "Boundary conditions are hard to keep track of. Put the processing for them " +
                    "in one place. Don't let them leak all over the code. 'array.length - 1' " +
                    "scattered through your code is a bug waiting to happen. Extract it: " +
                    "'final int lastIndex = array.length - 1;' — the name documents the intent " +
                    "and the adjustment happens in exactly one place."),
            Map.entry(HeuristicCode.G34,
                    "Mixing levels of abstraction within a function is always confusing. Section " +
                    "comments — '// initialisation', '// processing', '// cleanup' — are a dead " +
                    "giveaway. Each section is at a different level of abstraction. Extract each " +
                    "section into its own well-named function. The sections become the function " +
                    "calls, and the comment headers become function names."),
            Map.entry(HeuristicCode.G35,
                    "If you have a constant such as a default or configuration value, it is " +
                    "better to make it a named constant at the top of the class than to bury " +
                    "it in some low-level function. The high-level constant is easy to find " +
                    "and to change. A magic number deep in a private method is invisible."),
            Map.entry(HeuristicCode.G36,
                    "Write shy code — modules that don't reveal anything unnecessary and that " +
                    "don't rely on others' implementations. The Law of Demeter says a method f " +
                    "of class C should only call methods on C itself, objects created by f, " +
                    "objects passed as arguments to f, and objects held in instance variables. " +
                    "a.getB().getC().doSomething() is a train wreck — it couples you to the " +
                    "entire chain.")
    );

    static final Map<HeuristicCode, String> REFERENCES = Map.ofEntries(
            Map.entry(HeuristicCode.G23, "Clean Code Ch.17 'Smells and Heuristics — General' p.299"),
            Map.entry(HeuristicCode.G28, "Clean Code Ch.17 'Smells and Heuristics — General' p.301"),
            Map.entry(HeuristicCode.G29, "Clean Code Ch.17 'Smells and Heuristics — General' p.302"),
            Map.entry(HeuristicCode.G30, "Clean Code Ch.17 'Smells and Heuristics — General' p.302"),
            Map.entry(HeuristicCode.G31, "Clean Code Ch.17 'Smells and Heuristics — General' p.302"),
            Map.entry(HeuristicCode.G32, "Clean Code Ch.17 'Smells and Heuristics — General' p.303"),
            Map.entry(HeuristicCode.G33, "Clean Code Ch.17 'Smells and Heuristics — General' p.304"),
            Map.entry(HeuristicCode.G34, "Clean Code Ch.17 'Smells and Heuristics — General' p.304"),
            Map.entry(HeuristicCode.G35, "Clean Code Ch.17 'Smells and Heuristics — General' p.306"),
            Map.entry(HeuristicCode.G36, "Clean Code Ch.17 'Smells and Heuristics — General' p.306")
    );

    static final Map<HeuristicCode, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeuristicCode.G23, "Replace type-switching with polymorphism or an enum."),
            Map.entry(HeuristicCode.G28, "Extract complex boolean logic to a well-named method."),
            Map.entry(HeuristicCode.G29, "Rewrite double negatives as positive conditions."),
            Map.entry(HeuristicCode.G30, "Each function should do exactly one thing."),
            Map.entry(HeuristicCode.G33, "Extract boundary arithmetic to a named variable."),
            Map.entry(HeuristicCode.G34, "Section comments mean the method mixes abstraction levels. Extract."),
            Map.entry(HeuristicCode.G35, "Extract magic numbers from private methods to named class constants."),
            Map.entry(HeuristicCode.G36, "Don't reach through objects. Talk to immediate collaborators only.")
    );
}
