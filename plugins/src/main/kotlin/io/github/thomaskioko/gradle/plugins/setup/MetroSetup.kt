package io.github.thomaskioko.gradle.plugins.setup

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi
import dev.zacsweers.metro.gradle.MetroPluginExtension
import io.github.thomaskioko.gradle.plugins.utils.addImplementationDependency
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import org.gradle.api.Project

@OptIn(ExperimentalMetroGradleApi::class)
internal fun Project.setupMetro() {
    plugins.apply("dev.zacsweers.metro")

    addImplementationDependency(getDependency("metro-runtime"))

    extensions.configure(MetroPluginExtension::class.java) {
        it.generateContributionProviders.set(false) // Temporarily disable
    }
}
