pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "CleanClaude"

include("annotations", "core", "recipes", "refactoring", "adapters", "claude-review", "plugin")
