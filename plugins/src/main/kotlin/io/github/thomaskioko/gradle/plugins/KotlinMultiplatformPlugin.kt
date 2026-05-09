package io.github.thomaskioko.gradle.plugins

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import io.github.thomaskioko.gradle.plugins.setup.configureCommonAndroid
import io.github.thomaskioko.gradle.plugins.setup.setupTests
import io.github.thomaskioko.gradle.plugins.utils.addIfNotNull
import io.github.thomaskioko.gradle.plugins.utils.compilerOptions
import io.github.thomaskioko.gradle.plugins.utils.disableMultiplatformTasks
import io.github.thomaskioko.gradle.plugins.utils.getDependencyOrNull
import io.github.thomaskioko.gradle.plugins.utils.getPackageNameProvider
import io.github.thomaskioko.gradle.plugins.utils.kotlin
import io.github.thomaskioko.gradle.plugins.utils.kotlinMultiplatform
import io.github.thomaskioko.gradle.tasks.CopyMokoResourceBundlesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import io.github.thomaskioko.gradle.plugins.utils.capitalizeFirst
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

/**
 * Configures a Kotlin Multiplatform module with the project's default targets and settings.
 *
 * Apply this plugin on a shared module that ships JVM, Android, and iOS code. The plugin applies
 * `org.jetbrains.kotlin.multiplatform`, the Android KMP library plugin, and chains [BasePlugin].
 * It then enables the default source set hierarchy template, registers the JVM target,
 * `iosArm64`, and `iosSimulatorArm64`, applies the project's common Android configuration, sets
 * up host tests when `commonTest` or `androidHostTest` source sets exist, wires core library
 * desugaring when the consumer declares the artifact, applies the project's iOS compiler and
 * linker options to every native target, hooks variant tests into the [BasePlugin.LINUX_TEST]
 * and [BasePlugin.IOS_TEST] aggregates, and copies Moko resource bundles next to every Kotlin
 * Native binary so iOS tests can resolve localized strings.
 *
 * Most modules can stop here. Modules that need additional iOS targets, an XCFramework, or
 * iOS-specific compiler tweaks should call into the `scaffold {}` DSL ([io.github.thomaskioko.gradle.plugins.extensions.BaseExtension])
 * for those options.
 *
 * ```kotlin
 * plugins {
 *   id("io.github.thomaskioko.gradle.plugins.multiplatform")
 * }
 *
 * scaffold {
 *   useMetro()
 *   addAndroidTarget {
 *     useCompose()
 *   }
 * }
 * ```
 */
public abstract class KotlinMultiplatformPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.kotlin.multiplatform")
        target.plugins.apply("com.android.kotlin.multiplatform.library")
        target.plugins.apply(BasePlugin::class.java)

        target.kotlinMultiplatform {
            applyDefaultHierarchyTemplate()

            extensions.findByType(KotlinMultiplatformAndroidLibraryTarget::class.java)?.apply {
                configureCommonAndroid(target)

                val hasCommonTest = target.file("src/commonTest").exists()
                val hasAndroidHostTest = target.file("src/androidHostTest").exists()
                if (hasCommonTest || hasAndroidHostTest) {
                    withHostTest {
                        isIncludeAndroidResources = true
                    }
                }

                val desugarLibrary = target.getDependencyOrNull("android-desugarJdkLibs")
                target.dependencies.addIfNotNull("coreLibraryDesugaring", desugarLibrary)
                enableCoreLibraryDesugaring = true
            }

            jvm()

            iosArm64()
            iosSimulatorArm64()

            targets.withType(KotlinNativeTarget::class.java).configureEach {
                it.binaries.configureEach { framework ->
                    framework.linkerOpts("-lsqlite3")
                    framework.binaryOption("bundleId", target.getPackageNameProvider().get())
                }

                it.compilations.configureEach { compilation ->
                    compilation.compileTaskProvider.configure { compileTask ->
                        compileTask.compilerOptions.freeCompilerArgs.addAll(
                            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                            "-opt-in=kotlinx.cinterop.BetaInteropApi",
                            "-Xallocator=custom",
                            "-Xadd-light-debug=enable",
                            "-Xexpect-actual-classes",
                        )
                    }
                }
            }
        }

        target.kotlin {
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
                optIn.add("kotlin.time.ExperimentalTime")
            }
        }

        target.tasks.withType(Test::class.java).configureEach(Test::setupTests)
        target.configureMultiplatformTests()
        target.disableMultiplatformTasks()

        configureMokoResourceBundleCopy(target)
    }

    private fun Project.configureMultiplatformTests() {
        kotlinMultiplatform {
            targets.withType(KotlinTargetWithTests::class.java).configureEach { target ->
                target.compilations.configureEach { compilation ->
                    if (compilation.name.contains("test", ignoreCase = true)) {
                        val aggregateTaskName = when (target.name) {
                            "iosSimulatorArm64" -> BasePlugin.IOS_TEST
                            else -> BasePlugin.LINUX_TEST
                        }
                        val testTaskName = "${target.name}${compilation.name.capitalizeFirst()}"
                        rootProject.tasks.named(aggregateTaskName).configure {
                            it.dependsOn("${path}:$testTaskName")
                        }
                    }
                }
            }
        }
    }

    private fun configureMokoResourceBundleCopy(project: Project) {
        val buildDir = project.layout.buildDirectory
        project.tasks.withType(KotlinNativeLink::class.java).configureEach { linkTask ->
            val tempDir = buildDir.dir("tmp/mokoResourceBundles/${linkTask.name}").get().asFile
            linkTask.doLast("copyMokoResourceBundles") {
                CopyMokoResourceBundlesTask.copyBundles(
                    klibs = linkTask.libraries.plus(linkTask.sources),
                    outputDir = linkTask.outputFile.get().parentFile,
                    tempDir = tempDir,
                    logger = linkTask.logger,
                )
            }
        }
    }
}
