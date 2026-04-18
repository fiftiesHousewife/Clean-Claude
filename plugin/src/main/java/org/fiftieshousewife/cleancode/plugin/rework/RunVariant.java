package org.fiftieshousewife.cleancode.plugin.rework;

/**
 * Which subset of the refactoring toolset a rework run exposes to the
 * agent. Lets {@link ReworkCompareTask} decompose the cost/quality
 * signal into three axes: no tools, gradle-wrapping tools only, and
 * the full recipe-plus-gradle set.
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
 *       {@code format}.</li>
 * </ul>
 */
public enum RunVariant {
    VANILLA,
    MCP_GRADLE_ONLY,
    MCP_RECIPES
}
