package io.github.thomaskioko.gradle.plugins.utils

import org.gradle.api.Project

/**
 * Disables Kotlin Multiplatform tasks when in debug-only mode.
 *
 * When debugOnly=true, disables:
 * - All platform tests (keeps only unit tests)
 * - Maven publication tasks
 * - Compose compiler reports
 * - Device/connected tests
 *
 * When iOS targets are not enabled, additionally disables:
 * - iOS linking, assembly, and framework tasks
 */
internal fun Project.disableMultiplatformTasks() {
    if (isDebugOnlyBuild()) {
        disableTasks(kmpTasksToDisableForDebugOnly)
        disableTasks(composeTasksToDisableForDebugOnly)
        disableTasks(testTasksToDisableForDebugOnly)
    }

    // Disable iOS tasks when iOS targets are not enabled
    if (!isIosDebugBuildEnabled()) {
        disableTasks(iosTasksToDisable)
    }
}

/**
 * Disables cross-platform test execution and Maven publication tasks.
 */
private val kmpTasksToDisableForDebugOnly = listOf(
    "allTests",
    "publishAllPublicationsToMavenLocalRepository",
)

/**
 * iOS-specific tasks disabled when iOS targets are not enabled.
 */
private val iosTasksToDisable = listOf(
    // iOS linking tasks (final framework creation)
    "linkDebugFrameworkIosArm64",
    "linkReleaseFrameworkIosArm64",
    "linkDebugFrameworkIosSimulatorArm64",
    "linkReleaseFrameworkIosSimulatorArm64",
    // iOS test execution tasks
    "iosArm64Test",
    "iosSimulatorArm64Test",
    // iOS assembly tasks
    "assembleIosArm64",
    "assembleIosSimulatorArm64",
    // iOS XCFramework tasks
    "assembleDebugXCFramework",
    "assembleReleaseXCFramework",
    "assembleXCFramework",
)
