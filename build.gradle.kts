plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.publish) apply false

    alias(libs.plugins.app.root)
    alias(libs.plugins.spotless)
    alias(libs.plugins.app.spotless)
}

group = "io.github.thomaskioko.gradle.plugins"
version = providers.gradleProperty("VERSION_NAME").get()
