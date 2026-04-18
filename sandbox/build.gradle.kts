plugins {
    id("cleancode.java-library")
    id("org.fiftieshousewife.cleancode") version "1.0-SNAPSHOT"
}

// Fixture module for the rework harness. The classes here deliberately carry
// G30 / Ch10.1 findings so the paired rework runs have a stable benchmark.
//
// The plugin is resolved from mavenLocal — run `./gradlew :plugin:publishToMavenLocal`
// first (or use `scripts/rework-compare.sh` which handles the publish step).
// Rework comparisons mutate the files here in place and rely on `git restore`
// between runs, so nothing in sandbox/ is production code.

cleanCode {
    enforceFormatting.set(false)
}
