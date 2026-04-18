plugins {
    id("cleancode.java-library")
    id("org.fiftieshousewife.cleancode") version "1.0-SNAPSHOT"
}

// Fixture module for the rework harness. Only included in the build when
// `-PcleanCodeSelfApply=true` is set (see settings.gradle.kts) — CI's
// default `./gradlew build` skips this module entirely so the self-apply
// never needs the plugin on a classpath. Rework sessions run:
//
//   ./gradlew :plugin:publishToMavenLocal
//   ./gradlew -PcleanCodeSelfApply=true :sandbox:reworkCompare -Pfiles=...

cleanCode {
    enforceFormatting.set(false)
}
