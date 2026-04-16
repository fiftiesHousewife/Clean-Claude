package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class TestDescriptions {

    private TestDescriptions() {}

    static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.T1, "Insufficient Tests"),
            Map.entry(HeuristicCode.T2, "Use a Coverage Tool"),
            Map.entry(HeuristicCode.T3, "Don't Skip Trivial Tests"),
            Map.entry(HeuristicCode.T4, "An Ignored Test Is a Question about an Ambiguity"),
            Map.entry(HeuristicCode.T5, "Test Boundary Conditions"),
            Map.entry(HeuristicCode.T6, "Exhaustively Test Near Bugs"),
            Map.entry(HeuristicCode.T7, "Patterns of Failure Are Revealing"),
            Map.entry(HeuristicCode.T8, "Test Coverage Patterns Can Be Revealing"),
            Map.entry(HeuristicCode.T9, "Tests Should Be Fast")
    );

    static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.T1,
                    "A test suite is insufficient so long as there are conditions that have not " +
                    "been explored by tests, or calculations that have not been validated. " +
                    "Coverage tools report gaps in your testing strategy — they tell you where " +
                    "untested code lurks, waiting to surprise you in production."),
            Map.entry(HeuristicCode.T2,
                    "A coverage tool makes it easy to find modules, classes, and functions that " +
                    "are insufficiently tested. Most IDEs give you visual coverage, making it " +
                    "quick to find if and catch statements whose bodies haven't been tested. " +
                    "Use this tool. It's not optional."),
            Map.entry(HeuristicCode.T3,
                    "A disabled test is a question about an ambiguity. The code is there, the " +
                    "test is there, but someone decided it shouldn't run. Why? Is the test wrong, " +
                    "or is the code wrong? Find out and fix it. Don't leave disabled tests " +
                    "lingering — they are a form of lying about the state of your system."),
            Map.entry(HeuristicCode.T4,
                    "Sometimes we know that a test is failing because we also know that the " +
                    "requirements are ambiguous. It's tempting to disable it 'until we figure it " +
                    "out.' But that disabled test is a reminder that something is unresolved — " +
                    "and unresolved ambiguities are bugs waiting to surface."),
            Map.entry(HeuristicCode.T9,
                    "A slow test is a test that won't get run. When things get tight, the slow " +
                    "tests are the ones that get dropped from the suite. Keep your tests fast. " +
                    "Ruthlessly refactor tests that take too long to run.")
    );

    static final Map<HeuristicCode, String> REFERENCES = Map.ofEntries(
            Map.entry(HeuristicCode.T1, "Clean Code Ch.17 'Smells and Heuristics — Tests' p.313"),
            Map.entry(HeuristicCode.T2, "Clean Code Ch.17 'Smells and Heuristics — Tests' p.313"),
            Map.entry(HeuristicCode.T3, "Clean Code Ch.17 'Smells and Heuristics — Tests' p.313"),
            Map.entry(HeuristicCode.T4, "Clean Code Ch.17 'Smells and Heuristics — Tests' p.313"),
            Map.entry(HeuristicCode.T5, "Clean Code Ch.17 'Smells and Heuristics — Tests' p.314"),
            Map.entry(HeuristicCode.T6, "Clean Code Ch.17 'Smells and Heuristics — Tests' p.314"),
            Map.entry(HeuristicCode.T7, "Clean Code Ch.17 'Smells and Heuristics — Tests' p.314"),
            Map.entry(HeuristicCode.T8, "Clean Code Ch.17 'Smells and Heuristics — Tests' p.314"),
            Map.entry(HeuristicCode.T9, "Clean Code Ch.17 'Smells and Heuristics — Tests' p.314")
    );

    static final Map<HeuristicCode, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeuristicCode.T1, "Untested code is untrustworthy code. Increase coverage."),
            Map.entry(HeuristicCode.T2, "Configure and run a coverage tool."),
            Map.entry(HeuristicCode.T3, "Don't disable tests. Fix them or delete them."),
            Map.entry(HeuristicCode.T4, "A disabled test is an unresolved question. Answer it."),
            Map.entry(HeuristicCode.T9, "Slow tests don't get run. Keep them fast.")
    );
}
