plugins {
    id("io.github.thomaskioko.gradle.plugins.jvm")
}

dependencies {
    api(project(":core"))
    implementation(project(":core:testing"))
}
