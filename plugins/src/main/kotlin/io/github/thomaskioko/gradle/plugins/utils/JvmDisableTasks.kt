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
