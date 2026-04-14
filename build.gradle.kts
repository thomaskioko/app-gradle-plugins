plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.publish) apply false

    alias(libs.plugins.app.root)
    alias(libs.plugins.spotless)
    alias(libs.plugins.app.spotless)
}
