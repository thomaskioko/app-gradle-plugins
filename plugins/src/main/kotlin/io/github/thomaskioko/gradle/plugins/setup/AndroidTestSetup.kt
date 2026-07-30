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

import com.android.build.api.dsl.TestExtension
import com.android.build.api.dsl.TestOptions
import io.github.thomaskioko.gradle.plugins.utils.androidApp
import io.github.thomaskioko.gradle.plugins.utils.androidLibrary
import org.gradle.api.Project

/**
 * Applies the given configuration block to the active Android plugin's `testOptions`.
 *
 * Routes to the right extension type based on which Android plugin is applied
 * (`com.android.application`, `com.android.library`, or `com.android.test`).
 * Modules without any Android plugin applied are no-ops.
 */
internal fun Project.configureTestOptions(block: TestOptions.() -> Unit) {
    when {
        plugins.hasPlugin("com.android.application") ->
            androidApp { testOptions(block) }

        plugins.hasPlugin("com.android.library") ->
            androidLibrary { testOptions(block) }

        plugins.hasPlugin("com.android.test") ->
            extensions.configure(TestExtension::class.java) { it.testOptions(block) }
    }
}
