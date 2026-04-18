pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "CleanClaude"

include("annotations", "core", "recipes", "refactoring", "adapters", "claude-review", "plugin", "mcp")

// Sandbox fixtures opt in via a property so CI's default `./gradlew build`
// doesn't need the Clean Code plugin on the classpath. Humans running the
// rework harness pass `-PcleanCodeSelfApply=true` after publishing the
// plugin to mavenLocal.
if (providers.gradleProperty("cleanCodeSelfApply").orElse("false").get() == "true") {
    include("sandbox")
}
