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
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

/**
 * Adds [dependency] to the `implementation` configuration, or to `commonMainImplementation` when the
 * project is Kotlin Multiplatform.
 *
 * Accepts both a single dependency and a bundle.
 *
 * @param dependency Dependency to add, ignored when null.
 */
internal fun <T : Any> Project.addImplementationDependency(dependency: Provider<T>?) {
    if (dependency == null) return

    val configuration = when (kotlinExtension) {
        is KotlinMultiplatformExtension -> "commonMainImplementation"
        else -> "implementation"
    }
    dependencies.add(configuration, dependency)
}

/**
 * Registers a KSP processor against every target of the project.
 *
 * For Kotlin Multiplatform projects, attaches [dependency] to the per-target `ksp<Target>`
 * configurations. Single-platform projects fall back to the default `ksp` configuration.
 */
internal fun Project.addKspDependencyForAllTargets(dependency: Provider<MinimalExternalModuleDependency>) {
    if (!isKmpProject()) {
        dependencies.add("ksp", dependency)
        return
    }

    extensions.getByType(KotlinMultiplatformExtension::class.java).targets.names
        .asSequence()
        .map { it.replaceFirstChar(Char::uppercaseChar) }
        .map { target -> if (target == "Metadata") "CommonMainMetadata" else target }
        .forEach { targetConfigSuffix ->
            dependencies.add("ksp$targetConfigSuffix", dependency)
        }
}

/**
 * Registers a KSP processor that should run only against `commonMain` sources.
 *
 * For Kotlin Multiplatform projects, attaches [dependency] to the `kspCommonMainMetadata`
 * configuration so the processor runs once on shared sources and downstream targets pick the
 * generated symbols up through the `commonMain` source set wired by `setupKsp`. Single-platform
 * projects fall back to the default `ksp` configuration.
 *
 * Prefer this over [addKspDependencyForAllTargets] for processors whose annotations live in
 * `commonMain`, since attaching the same processor to per-target configurations would generate
 * the same classes again from each target compilation and trigger redeclaration errors.
 */
internal fun Project.addKspDependencyForCommonMain(dependency: Provider<MinimalExternalModuleDependency>) {
    when {
        isKmpProject() -> dependencies.add("kspCommonMainMetadata", dependency)
        else -> dependencies.add("ksp", dependency)
    }
}

private fun Project.isKmpProject(): Boolean = extensions.findByType(KotlinMultiplatformExtension::class.java) != null
