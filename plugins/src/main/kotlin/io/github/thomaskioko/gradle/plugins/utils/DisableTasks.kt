package io.github.thomaskioko.gradle.plugins.utils

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project

/**
 * Task disabling utilities compatible with AGP 8.3+
 * Last updated: 2025-01-13
 *
 * Design principles:
 * - Preserves task graph integrity using onlyIf { false }
 * - Handles missing tasks gracefully with findByName
 * - Skips processing during IDE sync to prevent configuration issues
 */

/** Default variant to keep active during debug-only builds */
private const val DEFAULT_ACTIVE_VARIANT = "debug"

/** Minimum supported AGP version for task name compatibility */
private const val MIN_AGP_VERSION = "8.3.0"

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
 * Disables Android tasks for all variants except the specified variant to keep.
 *
 * @param names List of task names to disable, supports {VARIANT} placeholder
 * @param variantToKeep Variant name to preserve (e.g., "debug"), empty means disable for all variants
 *
 * The {VARIANT} placeholder is replaced with the actual variant name (capitalized) to create
 * variant-specific task names. This allows for flexible task disabling across different build types.
 */
private fun Project.disableAndroidTasks(names: List<String>, variantToKeep: String = "") {
    extensions.configure<AndroidComponentsExtension<*, *, *>>("androidComponents") { components ->
        components.onVariants { variant ->
            if (variant.name != variantToKeep) {
                val variantAwareNames = names.map { taskName ->
                    taskName.replace("{VARIANT}", variant.name.replaceFirstChar { it.uppercase() })
                }
                disableTasks(variantAwareNames)
            }
        }
    }
}

/**
 * Core task disabling mechanism that safely disables tasks by name.
 *
 * @param names List of exact task names to disable
 *
 * Safety features:
 * - Skips during IDE sync to prevent build tool conflicts (AGP 8.3+ requirement)
 * - Preserves task graph integrity using onlyIf predicate
 * - Handles missing tasks gracefully with findByName
 */
private fun Project.disableTasks(names: List<String>) {
    if (names.isEmpty()) return

    // Skip during IDE sync to prevent configuration issues with AGP 8.3+
    val isIdeSyncing = providers.systemProperty("idea.sync.active")
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)
        .get()

    if (isIdeSyncing) return

    afterEvaluate {
        names.forEach { name ->
            tasks.findByName(name)?.let { task ->
                task.onlyIf { false }
                task.enabled = false
                task.description = "DISABLED: ${task.description ?: "No description"}"
            }
        }
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
 * Lint tasks disabled for Android libraries when debugOnly=true.
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

/**
 * Disables compiler metrics and reports generation.
 */
private val composeTasksToDisableForDebugOnly = listOf(
    "generateComposeCompilerReports",
    "generateComposeCompilerMetrics",
)

/**
 * Test tasks disabled when debugOnly=true. Keep unit tests but disable device/connected.
 */
private val testTasksToDisableForDebugOnly = listOf(
    "connectedAndroidTest",
    "deviceAndroidTest",
)
