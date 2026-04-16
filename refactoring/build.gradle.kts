plugins {
    id("cleancode.java-library")
}

dependencies {
    implementation(project(":annotations"))
    implementation(libs.openrewrite.core)
    implementation(libs.openrewrite.java)
    runtimeOnly(libs.openrewrite.java21)
    testImplementation(libs.openrewrite.test)
}
