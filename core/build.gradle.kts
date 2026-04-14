plugins {
    id("cleancode.java-library")
}

dependencies {
    api(project(":annotations"))
    implementation(libs.javaparser.core)
    implementation(libs.gson)
}
