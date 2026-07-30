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

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi
import dev.zacsweers.metro.gradle.MetroPluginExtension
import io.github.thomaskioko.gradle.plugins.utils.addImplementationDependency
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import org.gradle.api.Project

@OptIn(ExperimentalMetroGradleApi::class)
internal fun Project.setupMetro() {
    plugins.apply("dev.zacsweers.metro")

    addImplementationDependency(getDependency("metro-runtime"))

    extensions.configure(MetroPluginExtension::class.java) {
        it.generateContributionProviders.set(false) // Temporarily disable
    }
}
