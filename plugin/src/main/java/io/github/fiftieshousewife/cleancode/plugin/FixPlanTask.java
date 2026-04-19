package io.github.fiftieshousewife.cleancode.plugin;

import io.github.fiftieshousewife.cleancode.core.AggregatedReport;
import io.github.fiftieshousewife.cleancode.core.FixBriefGenerator;
import io.github.fiftieshousewife.cleancode.core.JsonReportReader;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.nio.file.Path;
import java.util.List;

@DisableCachingByDefault(because = "reads findings.json and walks project source via convention paths resolved at runtime")
public abstract class FixPlanTask extends DefaultTask {

    @TaskAction
    public void generate() throws Exception {
        final Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        final Path reportFile = buildDir.resolve("reports/clean-code/findings.json");
        final Path outputDir = buildDir.resolve("reports/clean-code/fix-briefs");
        final Path projectRoot = getProject().getProjectDir().toPath();

        final AggregatedReport report = JsonReportReader.read(reportFile);
        final List<Path> written = FixBriefGenerator.generate(report, outputDir, projectRoot);

        getLogger().lifecycle("Wrote {} fix briefs to {}", written.size() - 1, outputDir);
    }
}
