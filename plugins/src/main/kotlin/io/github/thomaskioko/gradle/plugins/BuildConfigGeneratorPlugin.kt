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
package io.github.thomaskioko.gradle.plugins

import io.github.thomaskioko.gradle.plugins.extensions.BuildConfigExtension
import io.github.thomaskioko.gradle.tasks.BuildConfigGeneratorTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Generates a Kotlin `BuildConfig` object with compile-time constants.
 *
 * Apply this plugin on a Kotlin Multiplatform module that needs build-time constants without
 * storing secrets in source control. The plugin registers the `buildConfig {}` extension
 * ([io.github.thomaskioko.gradle.plugins.extensions.BuildConfigExtension]), the
 * `generateBuildConfig` task, makes every Kotlin compile task depend on the generator, and adds
 * the generated sources to `commonMain`. Constants can be declared as literal values or read at
 * configuration time from `local.properties` or environment variables.
 *
 * ```kotlin
 * plugins {
 *   id("io.github.thomaskioko.gradle.plugins.buildconfig")
 * }
 *
 * buildConfig {
 *   packageName.set("com.thomaskioko.tvmaniac.core.base")
 *   buildConfigField("TMDB_API_KEY")
 * }
 * ```
 */
public class BuildConfigGeneratorPlugin : Plugin<Project> {

    /**
     * Creates `buildConfig {}`, registers `generateBuildConfig` and makes every Kotlin
     * compilation wait for it.
     */
    override fun apply(target: Project) {
        val extension = target.extensions.create(
            "buildConfig",
            BuildConfigExtension::class.java,
            target,
        )

        val generateBuildConfigTask = target.tasks.register(
            "generateBuildConfig",
            BuildConfigGeneratorTask::class.java,
        ) { task ->
            task.group = "build"
            task.description = "Generates BuildConfig with compile-time constants"

            task.packageName.set(extension.packageName)
            task.stringFields.set(extension.stringFields)
            task.booleanFields.set(extension.booleanFields)
            task.intFields.set(extension.intFields)
        }

        target.tasks.withType(KotlinCompile::class.java).configureEach { compileTask ->
            compileTask.dependsOn(generateBuildConfigTask)
        }

        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            target.extensions.configure(KotlinMultiplatformExtension::class.java) { kotlin ->
                kotlin.sourceSets.named("commonMain") { sourceSet ->
                    // Add the generated BuildConfig.kt to commonMain sources
                    sourceSet.kotlin.srcDir(generateBuildConfigTask.map { it.outputDirectory })
                }
            }
        }

        target.afterEvaluate {
            val pkg = extension.packageName.orNull
            if (pkg.isNullOrBlank()) {
                target.logger.warn(
                    """
                    |⚠️  BuildConfig plugin: packageName is not set!
                    |   Please configure in build.gradle.kts:
                    |   buildConfig {
                    |       packageName.set("your.package.name")
                    |   }
                    """.trimMargin(),
                )
            } else {
                target.logger.info("BuildConfig plugin configured for package: $pkg")
            }
        }
    }
}
