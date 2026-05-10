package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

public final class SkillPathRegistry {

    private SkillPathRegistry() {}

    private static final Map<HeuristicCode, String> SKILL_FILES = Map.ofEntries(
            // clean-code-exception-handling
            Map.entry(HeuristicCode.Ch7_1, "clean-code-exception-handling/SKILL.md"),

            // clean-code-null-handling
            Map.entry(HeuristicCode.Ch7_2, "clean-code-null-handling/SKILL.md"),

            // clean-code-functions
            Map.entry(HeuristicCode.Ch3_1, "clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.Ch3_2, "clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.Ch3_3, "clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.F1, "clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.F2, "clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.F3, "clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.G5, "clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.G30, "clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.G34, "clean-code-functions/SKILL.md"),
            Map.entry(HeuristicCode.G35, "clean-code-functions/SKILL.md"),

            // clean-code-classes
            Map.entry(HeuristicCode.Ch10_1, "clean-code-classes/SKILL.md"),
            Map.entry(HeuristicCode.Ch10_2, "clean-code-classes/SKILL.md"),
            Map.entry(HeuristicCode.G8, "clean-code-classes/SKILL.md"),
            Map.entry(HeuristicCode.G14, "clean-code-classes/SKILL.md"),
            Map.entry(HeuristicCode.G17, "clean-code-classes/SKILL.md"),
            Map.entry(HeuristicCode.G18, "clean-code-classes/SKILL.md"),

            // clean-code-naming
            Map.entry(HeuristicCode.N1, "clean-code-naming/SKILL.md"),
            Map.entry(HeuristicCode.N5, "clean-code-naming/SKILL.md"),
            Map.entry(HeuristicCode.N6, "clean-code-naming/SKILL.md"),
            Map.entry(HeuristicCode.N7, "clean-code-naming/SKILL.md"),
            Map.entry(HeuristicCode.G11, "clean-code-naming/SKILL.md"),
            Map.entry(HeuristicCode.G16, "clean-code-naming/SKILL.md"),

            // clean-code-conditionals-and-expressions
            Map.entry(HeuristicCode.G19, "clean-code-conditionals-and-expressions/SKILL.md"),
            Map.entry(HeuristicCode.G23, "clean-code-conditionals-and-expressions/SKILL.md"),
            Map.entry(HeuristicCode.G28, "clean-code-conditionals-and-expressions/SKILL.md"),
            Map.entry(HeuristicCode.G29, "clean-code-conditionals-and-expressions/SKILL.md"),
            Map.entry(HeuristicCode.G33, "clean-code-conditionals-and-expressions/SKILL.md"),
            Map.entry(HeuristicCode.G36, "clean-code-conditionals-and-expressions/SKILL.md"),

            // clean-code-dependency-updates
            Map.entry(HeuristicCode.E1, "clean-code-dependency-updates/SKILL.md"),
            Map.entry(HeuristicCode.E3, "clean-code-dependency-updates/SKILL.md"),

            // clean-code-comments-and-clutter
            Map.entry(HeuristicCode.C2, "clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.C3, "clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.C5, "clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.F4, "clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.G9, "clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.G10, "clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.G12, "clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.G22, "clean-code-comments-and-clutter/SKILL.md"),
            Map.entry(HeuristicCode.G24, "clean-code-comments-and-clutter/SKILL.md"),

            // clean-code-java-idioms
            Map.entry(HeuristicCode.G1, "clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.J1, "clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.J2, "clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.J3, "clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.G4, "clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.G25, "clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.G37, "clean-code-java-idioms/SKILL.md"),
            Map.entry(HeuristicCode.G26, "clean-code-java-idioms/SKILL.md"),

            // clean-code-test-quality
            Map.entry(HeuristicCode.T1, "clean-code-test-quality/SKILL.md"),
            Map.entry(HeuristicCode.T3, "clean-code-test-quality/SKILL.md"),
            Map.entry(HeuristicCode.T4, "clean-code-test-quality/SKILL.md")
    );

    public static String skillFileFor(final HeuristicCode code) {
        return SKILL_FILES.get(code);
    }

    public static String skillPathFor(final HeuristicCode code, final String skillsDir) {
        final String file = SKILL_FILES.get(code);
        return file == null ? null : skillsDir + "/" + file;
    }

    public static boolean hasSkill(final HeuristicCode code) {
        return SKILL_FILES.containsKey(code);
    }
}
