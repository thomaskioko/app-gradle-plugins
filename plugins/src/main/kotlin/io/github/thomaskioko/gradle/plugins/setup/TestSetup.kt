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
package io.github.thomaskioko.gradle.plugins.setup

import org.gradle.api.tasks.testing.Test

internal fun Test.setupTests() {
    val projectNameProvider = project.provider {
        project.path
            .replace("projects", "")
            .replaceFirst(":", "")
            .replace(":", "/")
    }

    reports.html.outputLocation.set(
        project.rootProject.layout.buildDirectory.dir(
            projectNameProvider.map { "reports/tests/$it" },
        ),
    )
    reports.junitXml.outputLocation.set(
        project.rootProject.layout.buildDirectory.dir(
            projectNameProvider.map { "reports/tests/$it" },
        ),
    )

    maxParallelForks = project.providers.systemProperty("test.maxParallelForks")
        .map { it.toIntOrNull() ?: (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1) }
        .orElse((Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1))
        .get()
}
