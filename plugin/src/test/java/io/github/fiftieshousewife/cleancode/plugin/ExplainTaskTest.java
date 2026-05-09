package io.github.fiftieshousewife.cleancode.plugin;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.core.SkillPathRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExplainTaskTest {

    private static final String SKILLS_DIR = ".claude/skills";

    @Test
    void resolvesFriendlyAliases() {
        assertAll(
                () -> assertEquals(".claude/skills/clean-code-exception-handling/SKILL.md",
                        ExplainTask.resolveSkillPath("error-handling", SKILLS_DIR)),
                () -> assertEquals(".claude/skills/clean-code-null-handling/SKILL.md",
                        ExplainTask.resolveSkillPath("nulls", SKILLS_DIR)),
                () -> assertEquals(".claude/skills/clean-code-classes/SKILL.md",
                        ExplainTask.resolveSkillPath("classes", SKILLS_DIR)),
                () -> assertEquals(".claude/skills/clean-code-functions/SKILL.md",
                        ExplainTask.resolveSkillPath("functions", SKILLS_DIR))
        );
    }

    @Test
    void resolvesRawHeuristicCodes() {
        assertAll(
                () -> assertEquals(".claude/skills/clean-code-classes/SKILL.md",
                        ExplainTask.resolveSkillPath("G18", SKILLS_DIR),
                        "G18 is mapped to clean-code-classes — bug from CLEANCODE_PLUGIN_FEEDBACK.md (v0.1.3)"),
                () -> assertEquals(".claude/skills/clean-code-conditionals-and-expressions/SKILL.md",
                        ExplainTask.resolveSkillPath("G19", SKILLS_DIR)),
                () -> assertEquals(".claude/skills/clean-code-functions/SKILL.md",
                        ExplainTask.resolveSkillPath("G30", SKILLS_DIR)),
                () -> assertEquals(".claude/skills/clean-code-java-idioms/SKILL.md",
                        ExplainTask.resolveSkillPath("J3", SKILLS_DIR))
        );
    }

    @Test
    void resolvesChapterCodesWithEitherDotOrUnderscore() {
        assertAll(
                () -> assertEquals(".claude/skills/clean-code-exception-handling/SKILL.md",
                        ExplainTask.resolveSkillPath("Ch7.1", SKILLS_DIR),
                        "users see Ch7.1 in the report; the enum constant is Ch7_1"),
                () -> assertEquals(".claude/skills/clean-code-exception-handling/SKILL.md",
                        ExplainTask.resolveSkillPath("Ch7_1", SKILLS_DIR)),
                () -> assertEquals(".claude/skills/clean-code-null-handling/SKILL.md",
                        ExplainTask.resolveSkillPath("Ch7.2", SKILLS_DIR))
        );
    }

    @Test
    void returnsNullForUnknownInput() {
        assertAll(
                () -> assertNull(ExplainTask.resolveSkillPath("not-a-real-code", SKILLS_DIR)),
                () -> assertNull(ExplainTask.resolveSkillPath("Z99", SKILLS_DIR)),
                () -> assertNull(ExplainTask.resolveSkillPath("", SKILLS_DIR)),
                () -> assertNull(ExplainTask.resolveSkillPath("   ", SKILLS_DIR))
        );
    }

    @Test
    void everyMappedCodeResolvesViaTheTaskLookup() {
        final List<String> unresolvable = new ArrayList<>();
        for (final HeuristicCode code : HeuristicCode.values()) {
            if (!SkillPathRegistry.hasSkill(code)) {
                continue;
            }
            final String resolved = ExplainTask.resolveSkillPath(code.name(), SKILLS_DIR);
            if (resolved == null) {
                unresolvable.add(code.name());
            }
        }
        assertTrue(unresolvable.isEmpty(),
                "Every code in SkillPathRegistry must be resolvable by ExplainTask.resolveSkillPath. "
                        + "These are mapped but unresolvable: " + unresolvable);
    }
}
