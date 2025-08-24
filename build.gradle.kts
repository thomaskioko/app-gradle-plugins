plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.dependency.analysis)
}

allprojects {
    group = "com.thomaskioko.gradle.plugins"
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