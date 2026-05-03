plugins {
    java
    id("com.github.ben-manes.versions")
}

group = "io.github.fiftieshousewife.cleancode"
version = rootProject.version

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(platform(catalog.findLibrary("junit-bom").get()))
    testImplementation(catalog.findLibrary("junit-jupiter").get())
    testRuntimeOnly(catalog.findLibrary("junit-platform-launcher").get())
}

tasks.withType<JavaCompile>().configureEach {
    // Compile with the JDK 25 toolchain but emit JDK 21 bytecode so the
    // plugin is consumable from any Gradle build running on JDK 21 or
    // newer. Without --release, the toolchain version becomes the
    // minimum runtime requirement (class file version 69), which forced
    // every consumer onto JDK 25.
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>().configureEach {
    outputFormatter = "json"
    rejectVersionIf {
        candidate.version.substringBefore(".") != currentVersion.substringBefore(".")
    }
}
