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
package io.github.thomaskioko.gradle.plugins.extensions

import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import java.net.URI

/**
 * Disables the Kotlin/Native compiler cache for the running Kotlin version.
 *
 * Resolves the matching [DisableCacheInKotlinVersion] constant by reflecting over its sealed
 * subclasses, removing the need to update a literal version constant on every Kotlin bump. The
 * Kotlin Gradle plugin keeps a rolling window of the three most recent releases. Once the running
 * Kotlin version falls outside that window, configuration fails with a clear error so the
 * workaround can be re-evaluated.
 *
 * @param reason Free-form explanation of why the cache is disabled. Surfaces in tooling diagnostics.
 * @param issueUrl Optional link to the upstream issue tracking the bug.
 */
@OptIn(KotlinNativeCacheApi::class)
internal fun Framework.disableNativeCacheForCurrentKotlin(
    reason: String,
    issueUrl: URI? = null,
) {
    val kotlinVersion = target.project.getKotlinPluginVersion()
    val parts = kotlinVersion.substringBefore('-').split('.').mapNotNull(String::toIntOrNull)
    require(parts.size == 3) {
        "Cannot parse Kotlin version '$kotlinVersion' as major.minor.patch."
    }
    val (major, minor, patch) = parts
    val available = DisableCacheInKotlinVersion::class.sealedSubclasses.mapNotNull { it.objectInstance }
    val constant = available.firstOrNull { it.major == major && it.minor == minor && it.patch == patch }
        ?: error(
            "No DisableCacheInKotlinVersion constant for Kotlin $kotlinVersion. " +
                "Available constants: $available. " +
                "If the underlying issue is fixed in this Kotlin version, remove the call to " +
                "disableNativeCacheForCurrentKotlin entirely.",
        )
    disableNativeCache(constant, reason, issueUrl)
}
