plugins {
    id("cleancode.java-library")
}

dependencies {
    implementation(project(":annotations"))
    implementation(libs.openrewrite.core)
    implementation(libs.openrewrite.java)
    testImplementation(libs.openrewrite.test)
    testRuntimeOnly(libs.openrewrite.java25)
}
