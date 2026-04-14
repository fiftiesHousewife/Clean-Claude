package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

public final class SkillPathRegistry {

    private SkillPathRegistry() {}

    private static final Map<HeuristicCode, String> SKILL_PATHS = Map.ofEntries(
            Map.entry(HeuristicCode.Ch7_1, ".claude/skills/ch7-exception-handling.md"),
            Map.entry(HeuristicCode.Ch7_2, ".claude/skills/ch7-null-handling.md"),
            Map.entry(HeuristicCode.Ch10_1, ".claude/skills/ch10-classes.md"),
            Map.entry(HeuristicCode.Ch10_2, ".claude/skills/ch10-classes.md"),
            Map.entry(HeuristicCode.Ch3_1, ".claude/skills/ch3-functions.md"),
            Map.entry(HeuristicCode.Ch3_2, ".claude/skills/ch3-functions.md"),
            Map.entry(HeuristicCode.Ch3_3, ".claude/skills/ch3-functions.md"),
            Map.entry(HeuristicCode.G5, ".claude/skills/ch3-functions.md"),
            Map.entry(HeuristicCode.Ch6_1, ".claude/skills/ch6-objects-and-data.md"),
            Map.entry(HeuristicCode.G36, ".claude/skills/ch3-functions.md")
    );

    public static String skillPathFor(HeuristicCode code) {
        return SKILL_PATHS.get(code);
    }

    public static boolean hasSkill(HeuristicCode code) {
        return SKILL_PATHS.containsKey(code);
    }
}
