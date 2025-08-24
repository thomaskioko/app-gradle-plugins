package io.github.thomaskioko.gradle.plugins.extensions

import io.github.thomaskioko.gradle.plugins.utils.configureStandaloneLint
import org.gradle.api.Project

public abstract class JvmExtension(private val project: Project) {
    public fun useAndroidLint() {
        project.plugins.apply("com.android.lint")

        project.configureStandaloneLint()
    }
}
