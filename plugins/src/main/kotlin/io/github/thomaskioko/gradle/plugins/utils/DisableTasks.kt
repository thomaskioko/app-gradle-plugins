package io.github.thomaskioko.gradle.plugins.utils

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project

/**
 * Disables Android application tasks based on debugOnly mode.
 *
 * When debugOnly=true: Disables release variants, most lint tasks, compose reports, and device tests.
 * When debugOnly=false: Only disables specific lint analyze tasks for non-debug variants.
 */
internal fun Project.disableAndroidApplicationTasks() {
    if (isDebugOnlyBuild()) {
        disableAndroidTasks(androidAppTasksToDisableForDebugOnly)
        disableAndroidTasks(androidAppLintTasksToDisableExceptOneVariant, "debug")
        disableTasks(composeTasksToDisableForDebugOnly)
        disableTasks(testTasksToDisableForDebugOnly)
    } else {
        disableAndroidTasks(androidAppLintTasksToDisableExceptOneVariant, "debug")
    }
}

/**
 * Disables Android library tasks.
 *
 * Always disables:
 * - AAR bundling tasks (libraries consumed as individual elements by AGP)
 * - Most lint reporting tasks (aggregated at app level)
 *
 * When debugOnly=true, additionally disables:
 * - All library lint tasks
 * - Release variant tasks
 * - Compose compiler reports
 */
internal fun Project.disableAndroidLibraryTasks() {
    disableAndroidTasks(androidLibraryTasksToDisable)
    if (isDebugOnlyBuild()) {
        disableAndroidTasks(androidLibraryLintTasksToDisable)
        disableAndroidTasks(androidLibraryTasksToDisableForDebugOnly)
        disableTasks(composeTasksToDisableForDebugOnly)
    }
    disableAndroidTasks(androidLibraryLintTasksToDisableExceptOneVariant, "debug")
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
                val variantAwareNames =
                    names.map { it.replace("{VARIANT}", variant.name.replaceFirstChar(Char::titlecase)) }
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
 * - Skips during IDE sync to prevent build tool conflicts
 * - Sets task.enabled = false to prevent execution
 * - Clears dependencies to break task dependency chains
 * - Updates description to indicate disabled state
 *
 * This is the foundation method used by all other task disabling functions.
 */
private fun Project.disableTasks(names: List<String>) {
    // since AGP 8.3 the tasks.named will fail during project sync
    if (providers.systemProperty("idea.sync.active").getOrElse("false").toBoolean()) {
        return
    }

    afterEvaluate {
        names.forEach { name ->
            tasks.findByName(name)?.let { task ->
                task.enabled = false
                task.description = "DISABLED"
                task.setDependsOn(mutableListOf<Any>())
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
private val androidLibraryLintTasksToDisable
    get() = listOf(
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
private val androidAppLintTasksToDisableExceptOneVariant
    get() = listOf(
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
 * Lint tasks disabled for JVM-only modules since this is aggregated at app level..
 */
private val lintTasksToDisableJvm
    get() = listOf(
        "lint",
        "lintFix",
        "updateLintBaseline",
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
 * Disables release variant assembly and bundling that aren't needed during
 * debug-only development cycles.
 */
private val androidLibraryTasksToDisableForDebugOnly = listOf(
    "assemble{VARIANT}",
    "bundle{VARIANT}Aar",
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
