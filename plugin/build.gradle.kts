plugins {
    id("cleancode.java-conventions")
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":core"))
    implementation(project(":adapters"))
    implementation(project(":annotations"))
}

gradlePlugin {
    plugins {
        create("cleanCode") {
            id = "org.fiftieshousewife.cleancode"
            implementationClass = "org.fiftieshousewife.cleancode.plugin.CleanCodePlugin"
        }
    }
}
