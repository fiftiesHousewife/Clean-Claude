package io.github.fiftieshousewife.cleancode.core;

import java.nio.file.Files;
import java.nio.file.Path;

public record AgentLayout(String instructionsFile, String skillsDir) {

    public static final AgentLayout CLAUDE = new AgentLayout("CLAUDE.md", ".claude/skills");
    public static final AgentLayout AGENTS = new AgentLayout("AGENTS.md", ".agent/skills");

    public static AgentLayout detect(final Path projectDir) {
        if (Files.exists(projectDir.resolve(CLAUDE.instructionsFile))) {
            return CLAUDE;
        }
        if (Files.exists(projectDir.resolve(AGENTS.instructionsFile))) {
            return AGENTS;
        }
        return AGENTS;
    }

    public static AgentLayout resolve(final Path projectDir,
                                      final String instructionsOverride,
                                      final String skillsOverride) {
        final AgentLayout detected = detect(projectDir);
        final String instructions = (instructionsOverride == null || instructionsOverride.isBlank())
                ? detected.instructionsFile : instructionsOverride;
        final String skills = (skillsOverride == null || skillsOverride.isBlank())
                ? detected.skillsDir : skillsOverride;
        return new AgentLayout(instructions, skills);
    }
}
