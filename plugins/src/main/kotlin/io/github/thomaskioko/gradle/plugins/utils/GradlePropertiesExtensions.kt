package io.github.thomaskioko.gradle.plugins.utils

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
