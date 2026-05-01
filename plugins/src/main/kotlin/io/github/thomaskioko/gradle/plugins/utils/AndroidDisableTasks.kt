package io.github.thomaskioko.gradle.plugins.utils

import org.gradle.api.Project

/**
 * Disables Android application tasks.
 *
 * Always disables:
 * - Lint tasks for non-debug variants (avoid duplicate work)
 *
 * When debugOnly=true, additionally disables:
 * - Release variant assembly, bundling, and installation
 * - Compose compiler reports
 * - Device/connected tests
 */
internal fun Project.disableAndroidApplicationTasks() {
    // Always disable lint for non-debug variants
    disableAndroidTasks(androidAppLintTasksToDisableExceptOneVariant, DEFAULT_ACTIVE_VARIANT)

    // Conditionally disable builds and tests
    if (isDebugOnlyBuild()) {
        disableAndroidTasks(androidAppTasksToDisableForDebugOnly, DEFAULT_ACTIVE_VARIANT)
        disableTasks(composeTasksToDisableForDebugOnly)
        disableTasks(testTasksToDisableForDebugOnly)
    }
}

/**
 * Disables Android library tasks.
 *
 * Always disables:
 * - Assembly and AAR bundling for non-debug variants (libraries consumed as individual elements by AGP)
 * - All lint reporting and fixing tasks (lint is aggregated at app level)
 * - Lint analysis for non-debug variants (avoid duplicate work)
 *
 * When debugOnly=true, additionally disables:
 * - Compose compiler reports
 */
internal fun Project.disableAndroidLibraryTasks() {
    // Always disable AAR tasks for non-debug variants
    disableAndroidTasks(androidLibraryTasksToDisable, DEFAULT_ACTIVE_VARIANT)

    // Always disable lint reports - aggregated at app level
    disableAndroidTasks(androidLibraryLintTasksToDisable, DEFAULT_ACTIVE_VARIANT)

    // Always disable lint analysis for non-debug variants
    disableAndroidTasks(androidLibraryLintTasksToDisableExceptOneVariant, DEFAULT_ACTIVE_VARIANT)

    // Conditionally disable compose reports
    if (isDebugOnlyBuild()) {
        disableTasks(composeTasksToDisableForDebugOnly)
    }
}

/**
 * Tasks that are always disabled for Android libraries.
 * Modern AGP consumes library modules as individual elements, not bundled AARs.
 */
private val androidLibraryTasksToDisable = listOf(
    "assemble",
    "assemble{VARIANT}",
    "bundle{VARIANT}Aar",
)

/**
 * Lint tasks disabled for Android libraries.
 * Removes lint reporting from libraries since we have aggregated reporting at the app level.
 */
private val androidLibraryLintTasksToDisable = listOf(
    // report
    "lint",
    "lint{VARIANT}",
    "lintReport{VARIANT}",
    "copy{VARIANT}LintReports",
    // fix
    "lintFix",
    "lintFix{VARIANT}",
    // baseline
    "updateLintBaseline",
    "updateLintBaseline{VARIANT}",
)

/**
 * Lint analyze tasks disabled for all variants except one (typically debug is kept).
 * Only run lint analysis on one variant to avoid duplicate work.
 */
private val androidLibraryLintTasksToDisableExceptOneVariant = listOf(
    // analyze
    "lintAnalyze{VARIANT}",
)

/**
 * Lint tasks disabled for all variants except one (typically debug is kept) in Android apps.
 */
private val androidAppLintTasksToDisableExceptOneVariant = listOf(
    // analyze
    "lintAnalyze{VARIANT}",
    // report
    "lint{VARIANT}",
    "lintReport{VARIANT}",
    "copy{VARIANT}LintReports",
    // fix
    "lintFix{VARIANT}",
    // baseline
    "updateLintBaseline{VARIANT}",
)

/**
 * Disables all release variant tasks including:
 * - Assembly (APK building)
 * - Bundling (AAB creation)
 * - Installation tasks
 */
private val androidAppTasksToDisableForDebugOnly = listOf(
    "assemble{VARIANT}",
    "bundle{VARIANT}",
    "install{VARIANT}",
)
