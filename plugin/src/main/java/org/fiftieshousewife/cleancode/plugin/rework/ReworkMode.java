package org.fiftieshousewife.cleancode.plugin.rework;

/**
 * How {@link ReworkOrchestrator} operates on a class.
 *
 * <ul>
 *   <li>{@link #SUGGEST_ONLY} — emits suggestions from the plugin's own
 *       detector, no LLM, no edits. Cheap sanity check and a diagnostic
 *       when we want to see what the plugin alone thinks is worth doing.</li>
 *   <li>{@link #AGENT_DRIVEN} — spawns {@code claude -p} with a brief
 *       containing the file and its findings, captures the agent's
 *       structured JSON output, and returns actions/rejections for the
 *       caller to use in the commit message.</li>
 * </ul>
 *
 * <p>A future {@code RECIPE_DIRECT} mode will invoke named recipes
 * programmatically when the detector can produce concrete option tuples.
 */
public enum ReworkMode {
    SUGGEST_ONLY,
    AGENT_DRIVEN
}
