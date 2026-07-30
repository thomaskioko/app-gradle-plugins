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

import io.github.thomaskioko.gradle.plugins.properties.scaffoldProperties
import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * Retrieves a string property from the project's Gradle properties.
 */
internal fun Project.stringProperty(name: String): Provider<String> = providers.gradleProperty(name)

/**
 * Retrieves a boolean property from the project's properties..
 */
internal fun Project.booleanProperty(name: String, defaultValue: Boolean): Provider<Boolean> {
    return stringProperty(name).map { it.toBoolean() }.orElse(defaultValue)
}

/**
 * Checks if debug-only build optimizations should be enabled.
 */
internal fun Project.isDebugOnlyBuild(): Boolean = scaffoldProperties().debugOnly.get()

/**
 * Checks if iOS targets should be enabled in KMP compilation.
 */
internal fun Project.isIosDebugBuildEnabled(): Boolean {
    val properties = scaffoldProperties()
    return properties.enableIos.get() || !properties.debugOnly.get()
}
