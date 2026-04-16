// Init script used by scripts/run-experiment.sh to self-apply the Clean Code
// plugin to this project's Java modules. Kept out of committed build files so
// normal ./gradlew build does not require publishToMavenLocal first.
//
// Preconditions (handled by run-experiment.sh):
//   1. The plugin is already in ~/.m2/repository (./gradlew publishToMavenLocal)
//   2. This init script is passed via --init-script
//
// Package-level suppressions come from @SuppressCleanCode on package-info.java
// in each module - no extra config is set here.

initscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    dependencies {
        classpath("org.fiftieshousewife.cleancode:plugin:1.0-SNAPSHOT")
    }
}

val javaModules = setOf(
    "annotations", "core", "adapters", "claude-review",
    "plugin", "recipes", "refactoring"
)

allprojects {
    if (name !in javaModules) return@allprojects
    apply<org.fiftieshousewife.cleancode.plugin.CleanCodePlugin>()
}
