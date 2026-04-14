package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class CleanCodeExtension {

    public abstract Property<Boolean> getFailOnViolation();

    @Inject
    public CleanCodeExtension(ObjectFactory objects) {
        getFailOnViolation().convention(true);
    }
}
