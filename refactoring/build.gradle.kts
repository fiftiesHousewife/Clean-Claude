plugins {
    id("cleancode.java-library")
}

dependencies {
    implementation(project(":annotations"))
    implementation(libs.openrewrite.core)
    implementation(libs.openrewrite.java)
    implementation(libs.fifties.system.out.to.lombok.log4j)
    runtimeOnly(libs.openrewrite.java25)
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
