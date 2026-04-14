plugins {
    id("cleancode.java-library")
}

dependencies {
    api(project(":core"))
    api(project(":annotations"))
    implementation(project(":recipes"))
    implementation(libs.openrewrite.core)
    implementation(libs.openrewrite.java)
    runtimeOnly(libs.openrewrite.java21)
}
