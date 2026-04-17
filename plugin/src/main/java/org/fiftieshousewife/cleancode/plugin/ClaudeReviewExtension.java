package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

@SuppressWarnings("this-escape")
public abstract class ClaudeReviewExtension {

    public abstract Property<Boolean> getEnabled();

    public abstract Property<String> getModel();

    public abstract Property<Integer> getMaxFilesPerRun();

    public abstract Property<Integer> getMinFileLines();

    public abstract ListProperty<String> getCodes();

    public abstract ListProperty<String> getExcludePatterns();

    @Inject
    public ClaudeReviewExtension() {
        getEnabled().convention(false);
        getModel().convention("claude-sonnet-4-6");
        getMaxFilesPerRun().convention(50);
        getMinFileLines().convention(10);
        getCodes().convention(java.util.List.of("G6", "G20", "N4"));
        getExcludePatterns().convention(java.util.List.of("**/generated/**"));
    }
}
