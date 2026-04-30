package io.github.thomaskioko.gradle.plugins.setup

import io.github.thomaskioko.gradle.plugins.utils.addImplementationDependency
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import org.gradle.api.Project

internal fun Project.setupSerialization() {
    plugins.apply("org.jetbrains.kotlin.plugin.serialization")

    addImplementationDependency(getDependency("kotlin-serialization-core"))
}
