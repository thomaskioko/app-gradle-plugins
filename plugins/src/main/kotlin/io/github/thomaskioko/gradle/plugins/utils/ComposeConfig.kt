package io.github.thomaskioko.gradle.plugins.utils

import io.github.thomaskioko.gradle.plugins.properties.scaffoldProperties
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal fun Project.setupCompose() {
    plugins.apply("org.jetbrains.kotlin.plugin.compose")

    val properties = scaffoldProperties()
    val enableMetrics = properties.composeMetrics
    val enableReports = properties.composeReports

    composeCompiler {
        // Needed for Layout Inspector to be able to see all of the nodes in the component tree:
        // https://issuetracker.google.com/issues/338842143
        includeSourceInformation.set(true)

        if (enableMetrics.get()) {
            val metricsFolder = layout.buildDirectory.map { it.dir("compose-metrics") }
            metricsDestination.set(metricsFolder)
        }

        if (enableReports.get()) {
            val reportsFolder = layout.buildDirectory.map { it.dir("compose-reports") }
            reportsDestination.set(reportsFolder)
        }

        if (properties.composeCompilerReports.isPresent) {
            val composeReports = layout.buildDirectory.map { it.dir("reports").dir("compose") }

            if (!enableReports.get()) {
                reportsDestination.set(composeReports)
            }

            if (!enableMetrics.get()) {
                metricsDestination.set(composeReports)
            }
        }

        val stabilityFile =
            project.layout.projectDirectory.file(rootProject.file("compose-stability.conf").absolutePath)
        stabilityConfigurationFiles.add(stabilityFile)

        targetKotlinPlatforms.set(
            KotlinPlatformType.entries
                .filterNot { it == KotlinPlatformType.native || it == KotlinPlatformType.jvm || it == KotlinPlatformType.wasm }
                .asIterable(),
        )
    }
}
