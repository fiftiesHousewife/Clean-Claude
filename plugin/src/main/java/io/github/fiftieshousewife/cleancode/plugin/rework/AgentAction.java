package io.github.fiftieshousewife.cleancode.plugin.rework;

import java.util.Map;

/**
 * One recipe invocation the agent performed (or would like to perform)
 * during a rework pass. {@code options} mirrors the recipe's JSON option
 * shape exactly so the commit-message body can include it verbatim if
 * needed. {@code why} is the agent's one-sentence justification — the
 * piece that makes later review possible.
 */
public record AgentAction(String recipe, Map<String, Object> options, String why) {}
