plugins {
    java
    id("com.github.ben-manes.versions")
    id("de.thetaphi.forbiddenapis")
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

// Forbidden APIs reports bytecode-level violations from the bundled
// signature files. Warn-only (ignoreFailures = true) so the report
// surfaces in `./gradlew check` without blocking the build. Once the
// codebase is clean (or legitimate sites annotated with
// @SuppressWarnings("forbidden")), flip ignoreFailures to false and
// retire SystemOutRecipe (G4) per docs/sessions/2026-05-04-recipe-research.md.
//
// Bundles selected for signal: jdk-system-out (G4 replacement),
// jdk-deprecated (deprecated method calls), jdk-internal (sun.* APIs).
// jdk-unsafe and jdk-non-portable are intentionally omitted — their
// rules flag String.format / formatted / toLowerCase / toUpperCase
// across the codebase, which is acceptable for internal CLI tooling
// and would drown out the higher-signal warnings.
tasks.withType<de.thetaphi.forbiddenapis.gradle.CheckForbiddenApis>().configureEach {
    bundledSignatures = setOf(
        "jdk-system-out",
        "jdk-deprecated",
        "jdk-internal",
    )
    suppressAnnotations = setOf(
        "**.SuppressForbidden",
        "**.SuppressWarnings",
    )
    ignoreFailures = true
}
