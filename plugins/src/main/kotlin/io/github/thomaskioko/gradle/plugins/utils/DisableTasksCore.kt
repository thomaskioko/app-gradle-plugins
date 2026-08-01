/*
 * Copyright 2026 Thomas Kioko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thomaskioko.gradle.plugins.utils

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project

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
 * Disables tasks by name, prefixing each disabled task's description with `DISABLED:` so the reason
 * is visible in `./gradlew tasks`.
 *
 * Tolerates names that match no task, since it filters inside `tasks.configureEach` rather than
 * resolving through `tasks.named`.
 *
 * @param names List of exact task names to disable
 */
internal fun Project.disableTasks(names: List<String>) {
    if (names.isEmpty()) return

    val taskNamesToDisable = names.toSet()

    tasks.configureEach { task ->
        if (task.name in taskNamesToDisable) {
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
