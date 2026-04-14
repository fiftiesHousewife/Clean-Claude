plugins {
    java
}

group = "org.fiftieshousewife.cleancode"
version = rootProject.version

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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

tasks.test {
    useJUnitPlatform()
}
