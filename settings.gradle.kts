pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "CleanClaude"

include("annotations", "core", "recipes", "refactoring", "adapters", "plugin")
