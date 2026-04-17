package org.fiftieshousewife.cleancode.plugin;

import org.gradle.api.Project;

import java.util.List;

final class RuntimeDependencies {

    private static final String RUNTIME_CLASSPATH = "runtimeClasspath";

    private RuntimeDependencies() {
    }

    static List<String> resolve(final Project project) {
        return project.getConfigurations().stream()
                .filter(c -> RUNTIME_CLASSPATH.equals(c.getName()))
                .flatMap(c -> c.getResolvedConfiguration().getResolvedArtifacts().stream())
                .map(a -> a.getModuleVersion().getId().getGroup()
                        + ":" + a.getModuleVersion().getId().getName())
                .distinct()
                .toList();
    }
}
