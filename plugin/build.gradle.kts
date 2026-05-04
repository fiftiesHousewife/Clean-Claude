import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata

plugins {
    id("cleancode.java-conventions")
    `java-gradle-plugin`
    alias(libs.plugins.shadow)
    alias(libs.plugins.vanniktech.maven.publish)
}

repositories {
    gradlePluginPortal()
}

// Everything in `bundled` gets embedded in the shadow jar without flowing
// into the published POM as runtime deps. Currently that's the SpotBugs
// Gradle plugin (gradlePluginPortal-only, no Maven Central coordinate).
// compileOnly extends bundled so callers see the classes at compile time.
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

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(bundled)
}

// Slow self-apply scenarios that run TestKit against the plugin's own
// source tree. Kept in a dedicated source set so they don't run on every
// :plugin:test invocation. Wired into :plugin:check so CI catches them.
val integrationTest by sourceSets.creating {
    java.srcDir("src/integrationTest/java")
    resources.srcDir("src/integrationTest/resources")
    compileClasspath += sourceSets.test.get().compileClasspath
    runtimeClasspath += sourceSets.test.get().runtimeClasspath
}

configurations["integrationTestImplementation"]
    .extendsFrom(configurations.testImplementation.get())
configurations["integrationTestRuntimeOnly"]
    .extendsFrom(configurations.testRuntimeOnly.get())

gradlePlugin {
    testSourceSets(sourceSets["test"], integrationTest)
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs the self-apply harness via TestKit against the plugin's own sources"
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
    systemProperty("cleancode.repoRoot", rootProject.projectDir.absolutePath)
}

tasks.named("check") {
    dependsOn(integrationTestTask)
}

tasks.named<ProcessResources>("processResources") {
    from(rootProject.file(".claude/skills")) {
        into("skills")
    }
    from(rootProject.file("HEURISTICS.md")) {
        into("clean-code")
    }
}

// The shadow jar is the single self-contained artifact we publish: the
// sibling project modules (core, adapters, claude-review, annotations,
// refactoring) never get their own Maven coordinate, so consumers can
// only resolve them if they're embedded here. External deps (openrewrite,
// gson, spotless) are bundled too so a user applying the plugin from
// Maven Central gets a working classpath without transitive surprises.
tasks.shadowJar {
    archiveClassifier.set("")
    isZip64 = true
    mergeServiceFiles()
    from(bundled.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    // Gradle's own APIs are provided by the runtime — shading them breaks
    // plugin isolation and bloats the jar for no gain.
    dependencies {
        exclude(dependency("org.gradle:.*"))
    }
}

// Publishing ships shadowJar as the primary artifact. Leaving the thin
// jar task enabled would put it on disk and confuse Vanniktech about
// which artifact to sign.
tasks.jar {
    enabled = false
}

gradlePlugin {
    plugins {
        create("cleanCode") {
            id = "io.github.fiftieshousewife.cleancode"
            implementationClass = "io.github.fiftieshousewife.cleancode.plugin.CleanCodePlugin"
            displayName = "Clean Code"
            description = "Aggregates PMD, Checkstyle, SpotBugs, CPD, JaCoCo, " +
                    "OpenRewrite recipe detectors, and an optional Claude " +
                    "review pass into a single clean-code analysis for Gradle."
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    // Skip signing on CI runners (no signatory configured); local publish
    // and the release pipeline still sign.
    if (System.getenv("CI") == null) {
        signAllPublications()
    }
    configure(GradlePlugin(javadocJar = JavadocJar.Empty()))
    coordinates("io.github.fiftieshousewife", "clean-code", project.version.toString())

    pom {
        name.set("Clean Code")
        description.set(
            "Aggregates PMD, Checkstyle, SpotBugs, CPD, JaCoCo, OpenRewrite " +
                "recipe detectors, and an optional Claude review pass into a " +
                "single clean-code analysis for Gradle. Published as a " +
                "self-contained plugin jar — no extra transitive deps."
        )
        url.set("https://github.com/fiftiesHousewife/Clean-Claude")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("fiftiesHousewife")
                name.set("Pippa Newbold")
                email.set("pippa.newbold@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/fiftiesHousewife/Clean-Claude")
            connection.set("scm:git:git://github.com/fiftiesHousewife/Clean-Claude.git")
            developerConnection.set("scm:git:ssh://git@github.com/fiftiesHousewife/Clean-Claude.git")
        }
    }
}

// `java-gradle-plugin` wires the thin jar (from components["java"]) into the
// pluginMaven publication. We disable that thin jar and substitute shadowJar so
// the published artifact is self-contained. Vanniktech's GradlePlugin platform
// attaches the sources + empty javadoc jars automatically — don't re-add them.
//
// We also strip <dependencies> from the POM: every implementation dep is shaded
// into the shadow jar, and the sibling project modules (core, adapters, ...)
// are never published, so leaving them in the POM would either double-load or
// fail to resolve at consumer side.
afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven").configure {
        artifacts.removeIf { it.classifier.isNullOrEmpty() }
        artifact(tasks.named("shadowJar"))
        pom.withXml {
            val root = asNode()
            (root.get("dependencies") as groovy.util.NodeList).forEach {
                root.remove(it as groovy.util.Node)
            }
        }
    }
}

// Suppress Gradle module metadata: it would otherwise advertise apiElements /
// runtimeElements variants pulling in the unpublished sibling modules. POM-only
// publishing keeps consumers (Gradle and Maven alike) on the shadow jar.
tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}

tasks.register<JavaExec>("runHarnessPass") {
    mainClass.set("io.github.fiftieshousewife.cleancode.plugin.rework.HarnessRecipePassCli")
    classpath = sourceSets.main.get().runtimeClasspath
    val dir = project.findProperty("dir")?.toString() ?: rootProject.projectDir.absolutePath
    val recipe = project.findProperty("recipe")?.toString()
    args = if (recipe != null) listOf(dir, "--recipe=$recipe") else listOf(dir)
}

tasks.register<JavaExec>("wholeCodebaseSummary") {
    description = "Counts OpenRewrite findings across every module; use before/after a sweep for deltas."
    group = "verification"
    mainClass.set("io.github.fiftieshousewife.cleancode.plugin.rework.WholeCodebaseSummaryCli")
    classpath = sourceSets.main.get().runtimeClasspath
    args = listOf(project.findProperty("dir")?.toString() ?: rootProject.projectDir.absolutePath)
}
