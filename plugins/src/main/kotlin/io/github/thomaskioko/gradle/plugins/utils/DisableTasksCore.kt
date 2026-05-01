package io.github.thomaskioko.gradle.plugins.utils

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project

/**
 * Task-disabling primitives shared by `AndroidDisableTasks`, `KmpDisableTasks`, and
 * `JvmDisableTasks`. Compatible with AGP 8.3+.
 *
 * Design principles:
 * - Preserves task graph integrity using `onlyIf { false }`
 * - Handles missing tasks gracefully via `tasks.configureEach` filtering
 * - Skips processing during IDE sync to prevent configuration issues
 */

/** Default variant to keep active during debug-only builds. */
internal const val DEFAULT_ACTIVE_VARIANT: String = "debug"

/**
 * Disables Android tasks for all variants except the specified variant to keep.
 *
 * @param names List of task names to disable, supports the `{VARIANT}` placeholder
 * @param variantToKeep Variant name to preserve (e.g., "debug"), empty means disable for all variants
 *
 * The `{VARIANT}` placeholder is replaced with the actual variant name (capitalized) to create
 * variant-specific task names. This allows for flexible task disabling across different build types.
 */
internal fun Project.disableAndroidTasks(names: List<String>, variantToKeep: String = "") {
    extensions.configure<AndroidComponentsExtension<*, *, *>>("androidComponents") { components ->
        components.onVariants { variant ->
            if (variant.name != variantToKeep) {
                val variantAwareNames = names.map { taskName ->
                    taskName.replace("{VARIANT}", variant.name.capitalizeFirst())
                }
                disableTasks(variantAwareNames)
            }
        }
    }
}

/**
 * Core task-disabling mechanism that safely disables tasks by name.
 *
 * @param names List of exact task names to disable
 *
 * Safety features:
 * - Skips during IDE sync to prevent build tool conflicts (AGP 8.3+ requirement)
 * - Preserves task graph integrity using `onlyIf` predicate
 * - Handles missing tasks gracefully via `tasks.configureEach` filtering
 */
internal fun Project.disableTasks(names: List<String>) {
    if (names.isEmpty()) return

    // Skip during IDE sync to prevent configuration issues with AGP 8.3+
    val isIdeSyncing = providers.systemProperty("idea.sync.active")
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)

    val taskNamesToDisable = names.toSet()

    tasks.configureEach { task ->
        if (task.name in taskNamesToDisable) {
            // Defer IDE sync check to execution time for configuration cache compatibility
            task.onlyIf { !isIdeSyncing.get() }
            task.enabled = false
            task.description = "DISABLED: ${task.description ?: "No description"}"
        }
    }
}

/** Compose compiler reporting tasks disabled when `debugOnly=true`. Shared by Android app + KMP. */
internal val composeTasksToDisableForDebugOnly: List<String> = listOf(
    "generateComposeCompilerReports",
    "generateComposeCompilerMetrics",
)

/** Device / connected test tasks disabled when `debugOnly=true`. Shared by Android app + KMP. */
internal val testTasksToDisableForDebugOnly: List<String> = listOf(
    "connectedAndroidTest",
    "deviceAndroidTest",
)
