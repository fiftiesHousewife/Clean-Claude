plugins {
    id("cleancode.java-library")
}

dependencies {
    api(project(":core"))
    api(project(":annotations"))
    implementation(libs.anthropic.java)
    implementation(libs.gson)
}
