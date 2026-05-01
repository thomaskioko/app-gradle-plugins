package io.github.thomaskioko.gradle.plugins.extensions

import io.github.thomaskioko.gradle.plugins.setup.setupStandaloneLint
import org.gradle.api.Project

@ScaffoldDsl
public abstract class JvmExtension(private val project: Project) {
    public fun useAndroidLint() {
        project.plugins.apply("com.android.lint")

        project.setupStandaloneLint()
    }
}
