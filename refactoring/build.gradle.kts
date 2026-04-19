plugins {
    id("cleancode.java-library")
}

dependencies {
    implementation(project(":annotations"))
    implementation(libs.openrewrite.core)
    implementation(libs.openrewrite.java)
    implementation(libs.fifties.system.out.to.lombok.log4j) {
        // The published recipe pulls rewrite-java-25 transitively (Java 25
        // bytecode). The Gradle daemon is currently pinned to JDK 21 and
        // can't analyse those class files. We already provide rewrite-java-21
        // for parsing, which is sufficient for the recipe's transforms.
        // Drop the exclusion as part of the JDK 25 upgrade.
        exclude(group = "org.openrewrite", module = "rewrite-java-25")
    }
    runtimeOnly(libs.openrewrite.java21)
    testImplementation(libs.openrewrite.test)
}

tasks.register<JavaExec>("extractMethod") {
    group = "clean code"
    description = "Runs ExtractMethodRecipe on a single file. See docs/extract-method-recipe.md."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.fiftieshousewife.cleancode.refactoring.extractmethod.ExtractMethodCli")
    systemProperty("extractmethod.file", findProperty("file")?.toString() ?: "")
    systemProperty("extractmethod.startLine", findProperty("startLine")?.toString() ?: "")
    systemProperty("extractmethod.endLine", findProperty("endLine")?.toString() ?: "")
    systemProperty("extractmethod.newMethodName", findProperty("newMethodName")?.toString() ?: "")
}
