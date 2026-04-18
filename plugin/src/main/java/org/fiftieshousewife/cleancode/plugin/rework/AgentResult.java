package org.fiftieshousewife.cleancode.plugin.rework;

import java.util.Optional;

/**
 * What {@link AgentRunner#run} returns: the agent's textual response
 * (the input to {@link AgentResponseParser}) plus optional token
 * accounting. Usage is optional because a fake runner in a test may
 * not have it, and because older {@code claude} versions may not emit
 * the {@code usage} field in their JSON envelope.
 */
public record AgentResult(String text, Optional<AgentUsage> usage) {

    public static AgentResult textOnly(final String text) {
        return new AgentResult(text, Optional.empty());
    }
}
