package io.github.fiftieshousewife.cleancode.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@DisableCachingByDefault(because = "writes a single file from a packaged resource; cheaper to re-run than to cache")
public abstract class WriteHeuristicsTask extends DefaultTask {

    private static final String RESOURCE_PATH = "/clean-code/HEURISTICS.md";
    private static final String OUTPUT_RELATIVE = "docs/clean-code/HEURISTICS.md";

    @TaskAction
    public void run() throws IOException {
        final Path target = getProject().getRootProject()
                .getProjectDir().toPath().resolve(OUTPUT_RELATIVE);
        try (InputStream stream = getClass().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Bundled heuristics resource missing from plugin jar: " + RESOURCE_PATH);
            }
            final byte[] bundled = stream.readAllBytes();
            if (Files.exists(target) && Files.size(target) == bundled.length) {
                final byte[] current = Files.readAllBytes(target);
                if (java.util.Arrays.equals(current, bundled)) {
                    getLogger().lifecycle("HEURISTICS.md already up to date at {}", target);
                    return;
                }
            }
            Files.createDirectories(target.getParent());
            Files.write(target, bundled);
            getLogger().lifecycle("Wrote {}", target);
        }
    }
}