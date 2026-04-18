package org.fiftieshousewife.cleancode.plugin.rework;

import java.nio.file.Path;
import java.util.List;

/**
 * One file handed to the agent in a batched rework session: its
 * absolute path, its project-relative path (what the prompt and
 * action log quote), and the list of findings that apply to it.
 */
public record FileTarget(Path absolutePath, String relativePath, List<Suggestion> suggestions) {}
