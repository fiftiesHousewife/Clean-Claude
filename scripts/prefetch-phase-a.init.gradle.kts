// One-shot prefetch for Phase A artefacts that the sandbox network can't
// reach (proxy + redirect chain doesn't honour JVM HTTP client). Run once
// with full network egress to seed ~/.gradle/caches:
//
//   ./gradlew --refresh-dependencies prefetchPhaseA \
//       -I scripts/prefetch-phase-a.init.gradle.kts
//
// After it finishes, delete this file is unnecessary — keeping it costs
// nothing and lets future sessions re-seed if the cache is wiped.

import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment

allprojects {
    afterEvaluate {
        if (project == rootProject) {
            repositories {
                mavenCentral()
                gradlePluginPortal()
            }
            val prefetch by configurations.creating {
                isCanBeResolved = true
                isCanBeConsumed = false
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class, Category.LIBRARY))
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class, LibraryElements.JAR))
                    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class, Bundling.EXTERNAL))
                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment::class, TargetJvmEnvironment.STANDARD_JVM))
                }
            }
            dependencies {
                prefetch("de.thetaphi:forbiddenapis:3.10")
                prefetch("org.openrewrite.recipe:rewrite-static-analysis:2.34.0")
                prefetch("org.openrewrite.recipe:rewrite-logging-frameworks:3.27.2")
                prefetch("org.openrewrite.recipe:rewrite-testing-frameworks:3.35.2")
                prefetch("org.openrewrite.recipe:rewrite-migrate-java:3.34.0")
            }
            tasks.register("prefetchPhaseA") {
                doLast { configurations["prefetch"].resolve() }
            }
        }
    }
}