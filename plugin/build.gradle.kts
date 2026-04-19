import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata

plugins {
    id("cleancode.java-conventions")
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
}

// The SpotBugs Gradle plugin is only published to gradlePluginPortal. Bundling its
// classes into our plugin jar lets consumers apply this plugin with only
// mavenLocal() in pluginManagement — no portal entry required. `bundled` extends
// compileOnly so the classes are visible at compile time but never flow into the
// published POM as runtime dependencies.
val bundled: Configuration = configurations.create("bundled") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations.compileOnly.get().extendsFrom(bundled)

dependencies {
    implementation(project(":core"))
    implementation(project(":adapters"))
    implementation(project(":claude-review"))
    implementation(project(":annotations"))
    implementation(project(":refactoring"))
    implementation(libs.openrewrite.core)
    implementation(libs.openrewrite.java)
    implementation(libs.gson)
    implementation(libs.spotless.gradle.plugin)
    "bundled"(libs.spotbugs.gradle.plugin)
    runtimeOnly(libs.openrewrite.java25)
}

tasks.jar {
    from(bundled.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(bundled)
}

tasks.named<ProcessResources>("processResources") {
    from(rootProject.file(".claude/skills")) {
        into("skills")
    }
}

gradlePlugin {
    plugins {
        create("cleanCode") {
            id = "io.github.fiftieshousewife.cleancode"
            implementationClass = "io.github.fiftieshousewife.cleancode.plugin.CleanCodePlugin"
        }
    }
}
