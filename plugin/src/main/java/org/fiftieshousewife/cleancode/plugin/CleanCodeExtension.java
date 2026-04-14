package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.provider.Property;

@SuppressWarnings("this-escape")
public abstract class CleanCodeExtension {

    public abstract Property<Boolean> getFailOnViolation();

    public CleanCodeExtension() {
        getFailOnViolation().convention(true);
    }
}
