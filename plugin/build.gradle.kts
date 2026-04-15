plugins {
    id("cleancode.java-conventions")
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(project(":core"))
    implementation(project(":adapters"))
    implementation(project(":annotations"))
    implementation(libs.spotbugs.gradle.plugin)
}

tasks.named<ProcessResources>("processResources") {
    from(rootProject.file(".claude/skills")) {
        into("skills")
    }
}

gradlePlugin {
    plugins {
        create("cleanCode") {
            id = "org.fiftieshousewife.cleancode"
            implementationClass = "org.fiftieshousewife.cleancode.plugin.CleanCodePlugin"
        }
    }
}
