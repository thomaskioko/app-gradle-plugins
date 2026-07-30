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
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

internal fun Project.addImplementationDependency(
    dependency: Provider<MinimalExternalModuleDependency>?,
    limitToTargets: Set<KotlinPlatformType>? = null,
) {
    addDependencyInternal(
        dependency = dependency,
        notMultiplatformConfiguration = "implementation",
        commonConfiguration = "commonMainImplementation",
        targetConfiguration = KotlinTarget::implementationConfigName,
        limitToTargets = limitToTargets,
    )
}

internal fun Project.addBundleImplementationDependency(
    dependency: Provider<ExternalModuleDependencyBundle>,
    limitToTargets: Set<KotlinPlatformType>? = null,
) {
    addDependencyInternal(
        dependency = dependency,
        notMultiplatformConfiguration = "implementation",
        commonConfiguration = "commonMainImplementation",
        targetConfiguration = KotlinTarget::implementationConfigName,
        limitToTargets = limitToTargets,
    )
}

private fun <T : Any> Project.addDependencyInternal(
    dependency: Provider<T>?,
    notMultiplatformConfiguration: String,
    commonConfiguration: String,
    targetConfiguration: KotlinTarget.() -> String,
    limitToTargets: Set<KotlinPlatformType>?,
) {
    if (dependency == null) return

    val extension = kotlinExtension
    if (extension !is KotlinMultiplatformExtension) {
        dependencies.add(notMultiplatformConfiguration, dependency)
        return
    }

    if (limitToTargets == null) {
        dependencies.add(commonConfiguration, dependency)
        return
    }

    extension.targets.configureEach {
        if (it.platformType in limitToTargets) {
            dependencies.add(it.targetConfiguration(), dependency)
        }
    }
}

internal fun Project.addKspDependencyForAllTargets(dependency: Provider<MinimalExternalModuleDependency>) = addKspDependencyForAllTargets("", dependency)

private fun Project.addKspDependencyForAllTargets(
    configurationNameSuffix: String,
    dependency: Provider<MinimalExternalModuleDependency>,
) {
    when {
        isKmpProject() -> addKspDependencyForKmp(configurationNameSuffix, dependency)
        else -> addKspDependencyForSinglePlatform(configurationNameSuffix, dependency)
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

private fun Project.addKspDependencyForKmp(
    configurationNameSuffix: String,
    dependency: Provider<MinimalExternalModuleDependency>,
) {
    val kmpExtension = extensions.getByType(KotlinMultiplatformExtension::class.java)
    kmpExtension.targets.names
        .asSequence()
        .map { it.replaceFirstChar(Char::uppercaseChar) }
        .map { target -> if (target == "Metadata") "CommonMainMetadata" else target }
        .forEach { targetConfigSuffix ->
            dependencies.add("ksp$targetConfigSuffix$configurationNameSuffix", dependency)
        }
}

private fun Project.addKspDependencyForSinglePlatform(
    configurationNameSuffix: String,
    dependency: Provider<MinimalExternalModuleDependency>,
) {
    dependencies.add("ksp$configurationNameSuffix", dependency)
}

internal fun KotlinTarget.implementationConfigName(): String {
    return when (targetName) {
        "main" -> "implementation"
        else -> "${targetName}MainImplementation"
    }
}
