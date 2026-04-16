// Gradle init script to make the Clean Code plugin resolvable in any project.
//
// Install:   cp scripts/cleancode.init.gradle.kts ~/.gradle/init.d/
// Activate:  ./gradlew publishToMavenLocal    (from this repo, once)
//
// With this init script on the classpath of any project on this machine,
// `plugins { id("org.fiftieshousewife.cleancode") version "..." }` will
// resolve both the plugin jar (from mavenLocal) and its SpotBugs transitive
// dependency (from gradlePluginPortal). Without the portal entry SpotBugs
// cannot resolve — it is not published to Maven Central.

beforeSettings {
    pluginManagement {
        repositories {
            mavenLocal()
            gradlePluginPortal()
            mavenCentral()
        }
    }
}
