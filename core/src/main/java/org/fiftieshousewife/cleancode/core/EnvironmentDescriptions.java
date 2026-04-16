package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class EnvironmentDescriptions {

    private EnvironmentDescriptions() {}

    static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.E1, "Build Requires More Than One Step"),
            Map.entry(HeuristicCode.E2, "Tests Require More Than One Step")
    );

    static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.E1,
                    "You should be able to build the system with a single trivial command. You " +
                    "should not have to search around for dependencies, scripts, or obscure " +
                    "commands. Outdated dependencies make that one-step build fragile — every " +
                    "stale library is a silent accumulation of risk, incompatibility, and " +
                    "unfixed vulnerabilities."),
            Map.entry(HeuristicCode.E2,
                    "You should be able to run all the unit tests with a single trivial command. " +
                    "Being able to run tests quickly, easily, and without fuss is so fundamental " +
                    "that a failure here poisons the entire development experience.")
    );

    static final Map<HeuristicCode, String> REFERENCES = Map.ofEntries(
            Map.entry(HeuristicCode.E1, "Clean Code Ch.17 'Smells and Heuristics — Environment' p.287"),
            Map.entry(HeuristicCode.E2, "Clean Code Ch.17 'Smells and Heuristics — Environment' p.287")
    );

    static final Map<HeuristicCode, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeuristicCode.E1, "Keep dependencies up to date.")
    );
}
