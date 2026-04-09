package io.github.thomaskioko.gradle.plugins

import io.github.thomaskioko.gradle.tasks.MokoResourceGeneratorTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

public class ResourceGeneratorPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val generateStringsTask = target.tasks.register("generateMokoStrings", MokoResourceGeneratorTask::class.java) { task ->
            task.group = "build"
            task.description = "Generates resource sealed class from Moko resources"
        }

        target.pluginManager.withPlugin("dev.icerock.mobile.multiplatform-resources") {
            generateStringsTask.configure { task ->
                task.dependsOn(target.tasks.named("generateMRcommonMain"))
            }
        }

        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            target.extensions.configure(KotlinMultiplatformExtension::class.java) { kotlin ->
                kotlin.sourceSets.named("commonMain") { sourceSet ->
                    sourceSet.kotlin.srcDir(generateStringsTask.map { it.commonMainOutput })
                }

                // Wire dependency only to compilations that include commonMain
                kotlin.targets.configureEach { kmpTarget ->
                    kmpTarget.compilations.configureEach { compilation ->
                        if (compilation.defaultSourceSet.dependsOn.any { it.name == "commonMain" } ||
                            compilation.defaultSourceSet.name == "commonMain"
                        ) {
                            compilation.compileTaskProvider.configure { task ->
                                task.dependsOn(generateStringsTask)
                            }
                        }
                    }
                }
            }
        }
    }
}
