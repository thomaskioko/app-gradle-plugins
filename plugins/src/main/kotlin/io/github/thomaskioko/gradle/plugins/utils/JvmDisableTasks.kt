package io.github.thomaskioko.gradle.plugins.utils

import org.gradle.api.Project

/**
 * Disables Kotlin/JVM library tasks.
 *
 * Always disables:
 * - Assembly tasks (libraries don't need to be assembled into JARs)
 * - Lint tasks (aggregated at app level)
 */
internal fun Project.disableKotlinLibraryTasks() {
    disableTasks(listOf("assemble"))
    disableTasks(lintTasksToDisableJvm)
}

/**
 * Lint tasks disabled for JVM-only modules since this is aggregated at app level.
 */
private val lintTasksToDisableJvm = listOf(
    "lint",
    "lintJvm",
    "lintReportJvm",
    "copyJvmLintReports",
    "lintFix",
    "lintFixJvm",
    "updateLintBaseline",
    "updateLintBaselineJvm",
    "lintVital",
    "lintVitalJvm",
    "lintVitalAnalyzeJvmMain",
    "lintVitalReportJvm",
)
