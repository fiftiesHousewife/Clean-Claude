package io.github.fiftieshousewife.cleancode.plugin.rework;

/**
 * Which subset of the refactoring toolset a rework run exposes to the
 * agent. Lets {@link ReworkCompareTask} decompose the cost/quality
 * signal into five axes.
 *
 * <ul>
 *   <li>{@link #VANILLA} — no MCP tools advertised. The agent uses
 *       its built-in Edit/Write/Bash to do both refactors and
 *       verification.</li>
 *   <li>{@link #MCP_GRADLE_ONLY} — the server's {@code verify_build},
 *       {@code run_tests}, and {@code format} tools are offered, but
 *       not {@code extract_method}. Isolates the "compact Gradle
 *       output" win from the "recipe does the refactor" win.</li>
 *   <li>{@link #MCP_RECIPES} — the full MCP server surface:
 *       {@code extract_method}, {@code verify_build}, {@code run_tests},
 *       {@code format}. Agent decides when to use each tool.</li>
 *   <li>{@link #HARNESS_RECIPES_THEN_AGENT} — the harness applies all
 *       deterministic refactoring recipes to every target file FIRST,
 *       then hands the partially-fixed file to the agent (with the
 *       MCP_RECIPES tool surface) for the remaining judgement-call
 *       findings.</li>
 *   <li>{@link #RECIPES_ONLY} — the harness applies every deterministic
 *       recipe and stops. No agent, no cost, no tokens. Establishes
 *       the lower bound on findings-removed-by-pure-OpenRewrite and
 *       the upper bound on how much recipe coverage we currently have.
 *       A variant with "final findings ≈ HARNESS's final findings"
 *       means the agent is adding no durable quality on top of recipes.</li>
 * </ul>
 */
public enum RunVariant {
    VANILLA,
    MCP_GRADLE_ONLY,
    MCP_RECIPES,
    HARNESS_RECIPES_THEN_AGENT,
    RECIPES_ONLY
}
