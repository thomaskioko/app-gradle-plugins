package io.github.thomaskioko.gradle.plugins.extensions

import io.github.thomaskioko.gradle.plugins.utils.androidApp
import java.io.File
import org.gradle.api.Project

public abstract class AppExtension(private val project: Project) {
    public fun applicationId(applicationId: String) {
        project.androidApp {
            defaultConfig.applicationId = applicationId
        }
    }

    public fun applicationIdSuffix(buildType: String, suffix: String) {
        project.androidApp {
            buildTypes.getByName(buildType).applicationIdSuffix = suffix
        }
    }

    public fun minify(vararg files: File) {
        project.androidApp {
            buildTypes.getByName("release") {
                it.isMinifyEnabled = true
                it.proguardFiles += files
            }
        }
    }
}
