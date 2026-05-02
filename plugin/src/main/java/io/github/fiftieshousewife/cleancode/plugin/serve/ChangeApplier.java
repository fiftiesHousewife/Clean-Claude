package io.github.fiftieshousewife.cleancode.plugin.serve;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies a batch of {@link PendingChange}s against the project root.
 *
 * <p>Build-script edits are accumulated against a single in-memory
 * {@link BuildScriptEditor} instance and saved once at the end so two
 * staged disables in the same batch produce one file write. Source-file
 * edits are likewise grouped per file.
 *
 * <p>If any change fails, all preceding changes are still applied —
 * partial success is preferred to silently dropping work, and the
 * client can re-stage what failed. The response carries per-change
 * error messages.
 */
public final class ChangeApplier {

    private final Path projectRoot;
    private final Path buildScriptPath;

    public ChangeApplier(final Path projectRoot) {
        this.projectRoot = projectRoot;
        this.buildScriptPath = projectRoot.resolve("build.gradle.kts");
    }

    public ApplyChangesResponse apply(final List<PendingChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return ApplyChangesResponse.ok(0);
        }
        final List<String> errors = new ArrayList<>();
        int applied = 0;

        BuildScriptEditor scriptEditor = null;
        final Map<Path, SourceFileEditor> sourceEditors = new HashMap<>();

        for (final PendingChange change : sortedForApply(changes)) {
            try {
                switch (change.kind()) {
                    case "disableRecipe" -> {
                        scriptEditor = scriptEditor != null ? scriptEditor : new BuildScriptEditor(buildScriptPath);
                        scriptEditor.disableRecipe(change.requireParam("code"));
                        applied++;
                    }
                    case "tuneThreshold" -> {
                        scriptEditor = scriptEditor != null ? scriptEditor : new BuildScriptEditor(buildScriptPath);
                        final int newValue = Integer.parseInt(change.requireParam("newValue"));
                        scriptEditor.tuneThreshold(change.requireParam("threshold"), newValue);
                        applied++;
                    }
                    case "suppressFinding" -> {
                        final Path file = projectRoot.resolve(change.requireParam("file"));
                        final SourceFileEditor editor = sourceEditors.computeIfAbsent(file, this::newSourceEditor);
                        final SourceFileEditor.Result result = editor.suppressFinding(
                                Integer.parseInt(change.requireParam("line")),
                                change.requireParam("code"),
                                change.reason());
                        if (result.success()) {
                            applied++;
                        } else {
                            errors.add(describe(change) + ": " + result.error());
                        }
                    }
                    default -> errors.add("unknown change kind: " + change.kind());
                }
            } catch (RuntimeException | IOException e) {
                errors.add(describe(change) + ": " + e.getMessage());
            }
        }

        try {
            if (scriptEditor != null) {
                scriptEditor.save();
            }
            for (final SourceFileEditor editor : sourceEditors.values()) {
                editor.save();
            }
        } catch (IOException e) {
            errors.add("write failed: " + e.getMessage());
        }

        if (errors.isEmpty()) {
            return ApplyChangesResponse.ok(applied);
        }
        return new ApplyChangesResponse(false, applied, errors);
    }

    /**
     * Apply suppressFinding changes within each file in descending line
     * order so an earlier insertion doesn't shift the line number of a
     * later suppression. Build-script changes go first since they're
     * file-independent and order-insensitive.
     */
    private static List<PendingChange> sortedForApply(final List<PendingChange> changes) {
        final List<PendingChange> buildScriptChanges = new ArrayList<>();
        final Map<String, List<PendingChange>> suppressionsByFile = new LinkedHashMap<>();
        for (final PendingChange c : changes) {
            if ("suppressFinding".equals(c.kind())) {
                suppressionsByFile.computeIfAbsent(c.params().getOrDefault("file", ""),
                        k -> new ArrayList<>()).add(c);
            } else {
                buildScriptChanges.add(c);
            }
        }
        final List<PendingChange> ordered = new ArrayList<>(buildScriptChanges);
        suppressionsByFile.values().forEach(group -> {
            group.sort(Comparator.comparingInt(
                    (PendingChange c) -> Integer.parseInt(c.params().getOrDefault("line", "0"))).reversed());
            ordered.addAll(group);
        });
        return ordered;
    }

    private SourceFileEditor newSourceEditor(final Path file) {
        try {
            return new SourceFileEditor(file);
        } catch (IOException e) {
            throw new RuntimeException("could not read " + file + ": " + e.getMessage(), e);
        }
    }

    private static String describe(final PendingChange change) {
        return change.kind() + " " + change.params();
    }
}
