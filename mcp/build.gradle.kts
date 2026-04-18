plugins {
    id("cleancode.java-library")
    application
}

application {
    mainClass.set("org.fiftieshousewife.cleancode.mcp.McpServer")
}

dependencies {
    implementation(project(":refactoring"))
    implementation(project(":annotations"))
    implementation(libs.openrewrite.core)
    implementation(libs.openrewrite.java)
    implementation(libs.gson)
    runtimeOnly(libs.openrewrite.java21)
}

// Bundle runtime deps into the jar so `java -jar mcp.jar` works without
// needing a Gradle classpath. Matches the plugin module's bundling pattern;
// duplicate META-INF entries are excluded so the merged jar stays loadable.
tasks.jar {
    // Gradle 9 requires the fat-jar's implicit dependencies on sibling
    // project jars to be declared explicitly; resolving runtimeClasspath
    // here pulls in annotations/refactoring/etc so declare each explicitly.
    dependsOn(configurations.runtimeClasspath)
    manifest {
        attributes["Main-Class"] = "org.fiftieshousewife.cleancode.mcp.McpServer"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
                "module-info.class")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
