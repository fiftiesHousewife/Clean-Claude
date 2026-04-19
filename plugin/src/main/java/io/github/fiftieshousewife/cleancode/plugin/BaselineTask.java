package io.github.fiftieshousewife.cleancode.plugin;

import io.github.fiftieshousewife.cleancode.core.AggregatedReport;
import io.github.fiftieshousewife.cleancode.core.BaselineManager;
import io.github.fiftieshousewife.cleancode.core.JsonReportReader;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.nio.file.Path;

@DisableCachingByDefault(because = "one-shot baseline writer reading the latest findings.json; no stable cache key")
public abstract class BaselineTask extends DefaultTask {

    @TaskAction
    public void baseline() throws Exception {
        Path buildDir = getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();
        Path reportFile = buildDir.resolve("reports/clean-code/findings.json");
        Path baselineFile = getProject().getProjectDir().toPath().resolve("clean-code-baseline.json");

        AggregatedReport report = JsonReportReader.read(reportFile);
        BaselineManager.writeBaseline(report, baselineFile);

        getLogger().lifecycle("Baseline written with {} findings", report.findings().size());
    }
}
