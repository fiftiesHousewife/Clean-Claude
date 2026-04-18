package org.fiftieshousewife.cleancode.plugin.rework;

import java.util.Map;

/**
 * A recipe invocation the agent tried and the recipe refused — or a
 * fix the agent considered and chose not to attempt. Recorded in the
 * {@link ReworkReport} so the commit message can tell the reviewer
 * what got left behind and why.
 */
public record AgentRejection(String recipe, Map<String, Object> options, String why) {}
