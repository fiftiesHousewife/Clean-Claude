package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

public final class SkillPathRegistry {

    private SkillPathRegistry() {}

    private static final Map<HeuristicCode, String> SKILL_PATHS = Map.ofEntries(
            // exception-handling.md
            Map.entry(HeuristicCode.Ch7_1, ".claude/skills/exception-handling.md"),

            // null-handling.md
            Map.entry(HeuristicCode.Ch7_2, ".claude/skills/null-handling.md"),

            // functions.md
            Map.entry(HeuristicCode.Ch3_1, ".claude/skills/functions.md"),
            Map.entry(HeuristicCode.Ch3_2, ".claude/skills/functions.md"),
            Map.entry(HeuristicCode.Ch3_3, ".claude/skills/functions.md"),
            Map.entry(HeuristicCode.F1, ".claude/skills/functions.md"),
            Map.entry(HeuristicCode.F2, ".claude/skills/functions.md"),
            Map.entry(HeuristicCode.F3, ".claude/skills/functions.md"),
            Map.entry(HeuristicCode.G5, ".claude/skills/functions.md"),
            Map.entry(HeuristicCode.G30, ".claude/skills/functions.md"),
            Map.entry(HeuristicCode.G34, ".claude/skills/functions.md"),
            Map.entry(HeuristicCode.G35, ".claude/skills/functions.md"),

            // classes.md
            Map.entry(HeuristicCode.Ch10_1, ".claude/skills/classes.md"),
            Map.entry(HeuristicCode.Ch10_2, ".claude/skills/classes.md"),
            Map.entry(HeuristicCode.G8, ".claude/skills/classes.md"),
            Map.entry(HeuristicCode.G14, ".claude/skills/classes.md"),
            Map.entry(HeuristicCode.G17, ".claude/skills/classes.md"),
            Map.entry(HeuristicCode.G18, ".claude/skills/classes.md"),

            // naming.md
            Map.entry(HeuristicCode.N1, ".claude/skills/naming.md"),
            Map.entry(HeuristicCode.N5, ".claude/skills/naming.md"),
            Map.entry(HeuristicCode.N6, ".claude/skills/naming.md"),
            Map.entry(HeuristicCode.N7, ".claude/skills/naming.md"),
            Map.entry(HeuristicCode.G11, ".claude/skills/naming.md"),
            Map.entry(HeuristicCode.G16, ".claude/skills/naming.md"),

            // conditionals-and-expressions.md
            Map.entry(HeuristicCode.G23, ".claude/skills/conditionals-and-expressions.md"),
            Map.entry(HeuristicCode.G28, ".claude/skills/conditionals-and-expressions.md"),
            Map.entry(HeuristicCode.G29, ".claude/skills/conditionals-and-expressions.md"),
            Map.entry(HeuristicCode.G33, ".claude/skills/conditionals-and-expressions.md"),
            Map.entry(HeuristicCode.G19, ".claude/skills/conditionals-and-expressions.md"),

            // comments-and-clutter.md
            Map.entry(HeuristicCode.C3, ".claude/skills/comments-and-clutter.md"),
            Map.entry(HeuristicCode.C5, ".claude/skills/comments-and-clutter.md"),
            Map.entry(HeuristicCode.G9, ".claude/skills/comments-and-clutter.md"),
            Map.entry(HeuristicCode.G10, ".claude/skills/comments-and-clutter.md"),
            Map.entry(HeuristicCode.G12, ".claude/skills/comments-and-clutter.md"),
            Map.entry(HeuristicCode.G24, ".claude/skills/comments-and-clutter.md"),

            // java-idioms.md
            Map.entry(HeuristicCode.J1, ".claude/skills/java-idioms.md"),
            Map.entry(HeuristicCode.J2, ".claude/skills/java-idioms.md"),
            Map.entry(HeuristicCode.J3, ".claude/skills/java-idioms.md"),
            Map.entry(HeuristicCode.G4, ".claude/skills/java-idioms.md"),
            Map.entry(HeuristicCode.G25, ".claude/skills/java-idioms.md"),
            Map.entry(HeuristicCode.G26, ".claude/skills/java-idioms.md"),

            // test-quality.md
            Map.entry(HeuristicCode.T1, ".claude/skills/test-quality.md"),
            Map.entry(HeuristicCode.T3, ".claude/skills/test-quality.md"),
            Map.entry(HeuristicCode.T4, ".claude/skills/test-quality.md")
    );

    public static String skillPathFor(HeuristicCode code) {
        return SKILL_PATHS.get(code);
    }

    public static boolean hasSkill(HeuristicCode code) {
        return SKILL_PATHS.containsKey(code);
    }
}
