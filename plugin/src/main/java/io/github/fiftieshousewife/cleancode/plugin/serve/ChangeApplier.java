package io.github.fiftieshousewife.cleancode.plugin.serve;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final RefactoringApplier refactoringApplier;

    public ChangeApplier(final Path projectRoot) {
        this(projectRoot, new RefactoringApplier(projectRoot));
    }

    ChangeApplier(final Path projectRoot, final RefactoringApplier refactoringApplier) {
        this.projectRoot = projectRoot;
        this.buildScriptPath = projectRoot.resolve("build.gradle.kts");
        this.refactoringApplier = refactoringApplier;
    }

    public ApplyChangesResponse apply(final List<PendingChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return ApplyChangesResponse.ok(0);
        }
        // Refuse Fix clicks (applyRefactoring) when the working tree has
        // uncommitted changes — recipes can rewrite hundreds of lines and
        // mixing recipe output with in-progress edits makes "git restore" no
        // longer a clean undo. Suppression edits and build-script tweaks are
        // small and reviewable, so they're allowed through.
        final boolean hasRefactoring = changes.stream().anyMatch(c -> "applyRefactoring".equals(c.kind()));
        if (hasRefactoring) {
            final Optional<String> dirty = workingTreeDirty();
            if (dirty.isPresent()) {
                return new ApplyChangesResponse(false, 0, List.of(
                        "Working tree has uncommitted changes — commit or stash before clicking Fix. "
                                + "Recipe rewrites can touch many lines; a clean tree keeps `git restore` "
                                + "as a single-command undo.",
                        dirty.get()));
            }
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
                    case "applyRefactoring" -> {
                        // Run on a clean disk state so a same-batch suppress
                        // edit lands first; that's why applyRefactoring is
                        // sorted last and applied after the BuildScript +
                        // SourceFile editors have saved.
                        if (scriptEditor != null) {
                            scriptEditor.save();
                            scriptEditor = null;
                        }
                        for (final SourceFileEditor editor : sourceEditors.values()) {
                            editor.save();
                        }
                        sourceEditors.clear();

                        final HeuristicCode code = HeuristicCode.valueOf(change.requireParam("code"));
                        final Optional<Path> targetFile = Optional.ofNullable(change.params().get("file"))
                                .filter(s -> !s.isBlank())
                                .map(projectRoot::resolve);
                        final RefactoringApplier.Outcome outcome = refactoringApplier.apply(code, targetFile);
                        if (outcome.success()) {
                            applied += outcome.filesChanged();
                        } else {
                            outcome.errors().forEach(err -> errors.add(describe(change) + ": " + err));
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
     * Apply order:
     * <ol>
     *   <li>Build-script edits — file-independent, order-insensitive.</li>
     *   <li>{@code suppressFinding} per file in descending line order so
     *       an earlier insertion doesn't shift the line number of a
     *       later suppression.</li>
     *   <li>{@code applyRefactoring} last, deduplicated by (code, file)
     *       so two staged Fix clicks on the same file run the recipe
     *       once. Recipes mutate the AST and would rewrite the
     *       suppression annotations the user just added if they ran
     *       earlier in the batch.</li>
     * </ol>
     */
    private static List<PendingChange> sortedForApply(final List<PendingChange> changes) {
        final List<PendingChange> buildScriptChanges = new ArrayList<>();
        final Map<String, List<PendingChange>> suppressionsByFile = new LinkedHashMap<>();
        final Map<String, PendingChange> dedupedFixes = new LinkedHashMap<>();
        for (final PendingChange c : changes) {
            switch (c.kind()) {
                case "suppressFinding" -> suppressionsByFile.computeIfAbsent(
                        c.params().getOrDefault("file", ""), k -> new ArrayList<>()).add(c);
                case "applyRefactoring" -> dedupedFixes.putIfAbsent(
                        c.params().getOrDefault("code", "") + "|" + c.params().getOrDefault("file", ""), c);
                default -> buildScriptChanges.add(c);
            }
        }
        final List<PendingChange> ordered = new ArrayList<>(buildScriptChanges);
        suppressionsByFile.values().forEach(group -> {
            group.sort(Comparator.comparingInt(
                    (PendingChange c) -> Integer.parseInt(c.params().getOrDefault("line", "0"))).reversed());
            ordered.addAll(group);
        });
        ordered.addAll(dedupedFixes.values());
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

    /**
     * Returns the porcelain status output if the working tree is dirty,
     * empty if clean, or empty if git is unavailable (we don't want a
     * missing git binary to block the fix flow on systems where the
     * project happens to live outside version control).
     */
    Optional<String> workingTreeDirty() {
        try {
            final Process process = new ProcessBuilder("git", "status", "--porcelain")
                    .directory(projectRoot.toFile())
                    .redirectErrorStream(true)
                    .start();
            final String output = new String(process.getInputStream().readAllBytes()).trim();
            final boolean exited = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                return Optional.empty();
            }
            if (process.exitValue() != 0) {
                return Optional.empty();
            }
            if (output.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(output);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }
}
