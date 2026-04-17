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
        classpath("com.github.ben-manes:gradle-versions-plugin:0.53.0")
    }
}

val javaModules = setOf(
    "annotations", "core", "adapters", "claude-review",
    "plugin", "recipes", "refactoring"
)

// The Gradle root project owns gradle/libs.versions.toml, so E1 dependency
// findings are emitted only there. Sub-modules see the catalog as an ancestor
// and skip E1 to avoid the cross-module duplication that made the per-module
// HTML reports list the same outdated deps five times.
allprojects {
    val isRoot = project == rootProject
    if (!isRoot && name !in javaModules) return@allprojects
    if (isRoot) {
        repositories {
            mavenCentral()
        }
        apply<com.github.benmanes.gradle.versions.VersionsPlugin>()
        tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>().configureEach {
            outputFormatter = "json"
            rejectVersionIf {
                candidate.version.substringBefore(".") != currentVersion.substringBefore(".")
            }
        }
    }
    apply<org.fiftieshousewife.cleancode.plugin.CleanCodePlugin>()
}
