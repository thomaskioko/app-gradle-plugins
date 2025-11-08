plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.publish) apply false

    alias(libs.plugins.app.root)
    alias(libs.plugins.app.spotless)
}

allprojects {
    group = "io.github.thomaskioko.gradle.plugins"
    version = providers.gradleProperty("VERSION_NAME").get()
}

dependencyAnalysis {
    issues {
        all {
            onAny {
                severity("fail")
            }
        }
    }
}
