buildscript {
    // Lazy file reference — Gradle doesn't resolve this until `apply(plugin)`
    // below actually runs. CI's `./gradlew build` leaves the apply off and
    // never dereferences the jar, so a missing `plugin/build/libs/*.jar` is
    // only fatal for rework sessions that opted in.
    dependencies {
        classpath(files(rootProject.projectDir.resolve("plugin/build/libs/plugin-1.0-SNAPSHOT.jar")))
    }
}

plugins {
    id("cleancode.java-library")
}

// Fixture module for the rework harness. The classes here deliberately carry
// G30 / Ch10.1 findings so the paired rework runs have a stable benchmark.
//
// The Clean Code plugin self-applies here only when `-PcleanCodeSelfApply=true`
// is set. This keeps CI's `./gradlew build` clean (it never applies the plugin
// so it never needs the jar on disk) while letting rework sessions opt in with
// `./gradlew -PcleanCodeSelfApply=true :sandbox:reworkCompare …`. Paired with
// a prior `:plugin:jar` so the classpath file reference above actually exists.
if (project.findProperty("cleanCodeSelfApply") == "true") {
    apply<org.fiftieshousewife.cleancode.plugin.CleanCodePlugin>()
    configure<org.fiftieshousewife.cleancode.plugin.CleanCodeExtension> {
        enforceFormatting.set(false)
    }
}
