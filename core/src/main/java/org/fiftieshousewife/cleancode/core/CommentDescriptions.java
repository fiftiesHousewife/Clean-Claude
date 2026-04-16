package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

final class CommentDescriptions {

    private CommentDescriptions() {}

    static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.C1, "Inappropriate Information"),
            Map.entry(HeuristicCode.C2, "Obsolete Comment"),
            Map.entry(HeuristicCode.C3, "Redundant Comment"),
            Map.entry(HeuristicCode.C4, "Poorly Written Comment"),
            Map.entry(HeuristicCode.C5, "Commented-Out Code")
    );

    static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            Map.entry(HeuristicCode.C1,
                    "Comments should be reserved for technical notes about the code and its " +
                    "design. Anything that belongs in another system — changelogs, author " +
                    "attributions, issue tracker references — is inappropriate information " +
                    "that clutters the source and drifts out of date."),
            Map.entry(HeuristicCode.C2,
                    "A comment that has become inaccurate is worse than no comment at all. " +
                    "It actively misleads. If the code has changed and the comment hasn't, " +
                    "delete it or rewrite it. Floating, forgotten comments are a form of lying."),
            Map.entry(HeuristicCode.C3,
                    "A comment that merely restates the code is clutter. 'i++ // increment i' " +
                    "teaches us nothing. If the code is so unclear that it needs a comment to " +
                    "explain what it does, the real solution is to make the code clearer — not " +
                    "to add a redundant comment on top of unclear code."),
            Map.entry(HeuristicCode.C4,
                    "If you are going to write a comment, take the time to make sure it is the " +
                    "best comment you can write. Choose your words carefully. Use correct grammar " +
                    "and punctuation. A sloppy comment is a sign that the author doesn't care — " +
                    "and that carelessness infects the code around it."),
            Map.entry(HeuristicCode.C5,
                    "Commented-out code rots. Others who see it won't have the courage to delete " +
                    "it — they'll assume it's there for a reason. Over time, commented-out code " +
                    "accumulates like sediment, obscuring the code that actually matters. Delete " +
                    "it. Source control remembers everything; you don't need to.")
    );

    static final Map<HeuristicCode, String> REFERENCES = Map.ofEntries(
            Map.entry(HeuristicCode.C1, "Clean Code Ch.17 'Smells and Heuristics — Comments' p.286"),
            Map.entry(HeuristicCode.C2, "Clean Code Ch.17 'Smells and Heuristics — Comments' p.286"),
            Map.entry(HeuristicCode.C3, "Clean Code Ch.17 'Smells and Heuristics — Comments' p.286"),
            Map.entry(HeuristicCode.C4, "Clean Code Ch.17 'Smells and Heuristics — Comments' p.287"),
            Map.entry(HeuristicCode.C5, "Clean Code Ch.17 'Smells and Heuristics — Comments' p.287")
    );

    static final Map<HeuristicCode, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeuristicCode.C3, "Delete comments that restate the code."),
            Map.entry(HeuristicCode.C5, "Delete commented-out code. Source control remembers.")
    );
}
