package io.github.thomaskioko.gradle.plugins

import io.github.thomaskioko.gradle.plugins.extensions.BuildConfigExtension
import io.github.thomaskioko.gradle.tasks.BuildConfigGeneratorTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Gradle plugin that generates BuildConfig.kt with compile-time constants.
 *
 * This plugin integrates with Kotlin Multiplatform projects to provide
 * build-time configuration values without storing secrets in source control.
 *
 * Usage in build.gradle.kts:
 * ```
 * plugins {
 *     id("io.github.thomaskioko.gradle.plugins.buildconfig")
 * }
 *
 * buildConfig {
 *     packageName.set("com.thomaskioko.tvmaniac.core.base")
 *     buildConfigField("TMDB_API_KEY")
 * }
 * ```
 */
public class BuildConfigGeneratorPlugin : Plugin<Project> {

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
