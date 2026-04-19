package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

public final class SkillPathRegistry {

    private SkillPathRegistry() {}

    private static final Map<HeuristicCode, String> SKILL_PATHS = Map.ofEntries(
            // clean-code-exception-handling
            Map.entry(HeuristicCode.Ch7_1, ".claude/skills/clean-code-exception-handling/SKILL.md"),

            // clean-code-null-handling
            Map.entry(HeuristicCode.Ch7_2, ".claude/skills/clean-code-null-handling/SKILL.md"),

            // clean-code-functions
            Map.entry(HeuristicCode.Ch3_1, ".claude/skills/clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.Ch3_2, ".claude/skills/clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.Ch3_3, ".claude/skills/clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.F1, ".claude/skills/clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.F2, ".claude/skills/clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.F3, ".claude/skills/clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.G5, ".claude/skills/clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.G30, ".claude/skills/clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.G34, ".claude/skills/clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.G35, ".claude/skills/clean-code-functions/SKILL.md"),

            // clean-code-classes
            Map.entry(HeuristicCode.Ch10_1, ".claude/skills/clean-code-classes/SKILL.md"),
            Map.entry(HeuristicCode.Ch10_2, ".claude/skills/clean-code-classes/SKILL.md"),
            Map.entry(HeuristicCode.G8, ".claude/skills/clean-code-classes/SKILL.md"),
            Map.entry(HeuristicCode.G14, ".claude/skills/clean-code-classes/SKILL.md"),
            Map.entry(HeuristicCode.G17, ".claude/skills/clean-code-classes/SKILL.md"),
            Map.entry(HeuristicCode.G18, ".claude/skills/clean-code-classes/SKILL.md"),

            // clean-code-naming
            Map.entry(HeuristicCode.N1, ".claude/skills/clean-code-naming/SKILL.md"),
            Map.entry(HeuristicCode.N5, ".claude/skills/clean-code-naming/SKILL.md"),
            Map.entry(HeuristicCode.N6, ".claude/skills/clean-code-naming/SKILL.md"),
            Map.entry(HeuristicCode.N7, ".claude/skills/clean-code-naming/SKILL.md"),
            Map.entry(HeuristicCode.G11, ".claude/skills/clean-code-naming/SKILL.md"),
            Map.entry(HeuristicCode.G16, ".claude/skills/clean-code-naming/SKILL.md"),

            // clean-code-conditionals-and-expressions
            Map.entry(HeuristicCode.G19, ".claude/skills/clean-code-conditionals-and-expressions/SKILL.md"),
            Map.entry(HeuristicCode.G23, ".claude/skills/clean-code-conditionals-and-expressions/SKILL.md"),
            Map.entry(HeuristicCode.G28, ".claude/skills/clean-code-conditionals-and-expressions/SKILL.md"),
            Map.entry(HeuristicCode.G29, ".claude/skills/clean-code-conditionals-and-expressions/SKILL.md"),
            Map.entry(HeuristicCode.G33, ".claude/skills/clean-code-conditionals-and-expressions/SKILL.md"),
            Map.entry(HeuristicCode.G36, ".claude/skills/clean-code-conditionals-and-expressions/SKILL.md"),

            // clean-code-dependency-updates
            Map.entry(HeuristicCode.E1, ".claude/skills/clean-code-dependency-updates/SKILL.md"),

            // clean-code-comments-and-clutter
            Map.entry(HeuristicCode.C2, ".claude/skills/clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.C3, ".claude/skills/clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.C5, ".claude/skills/clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.F4, ".claude/skills/clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.G9, ".claude/skills/clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.G10, ".claude/skills/clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.G12, ".claude/skills/clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.G22, ".claude/skills/clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.G24, ".claude/skills/clean-code-comments-and-clutter/SKILL.md"),

            // clean-code-java-idioms
            Map.entry(HeuristicCode.G1, ".claude/skills/clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.J1, ".claude/skills/clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.J2, ".claude/skills/clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.J3, ".claude/skills/clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.G4, ".claude/skills/clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.G25, ".claude/skills/clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.G26, ".claude/skills/clean-code-java-idioms/SKILL.md"),

            // clean-code-test-quality
            Map.entry(HeuristicCode.T1, ".claude/skills/clean-code-test-quality/SKILL.md"),
            Map.entry(HeuristicCode.T3, ".claude/skills/clean-code-test-quality/SKILL.md"),
            Map.entry(HeuristicCode.T4, ".claude/skills/clean-code-test-quality/SKILL.md")
    );

    public static String skillPathFor(HeuristicCode code) {
        return SKILL_PATHS.get(code);
    }

    public static boolean hasSkill(HeuristicCode code) {
        return SKILL_PATHS.containsKey(code);
    }
}
