package io.github.thomaskioko.gradle.plugins

import io.github.thomaskioko.gradle.plugins.extensions.BaseExtension
import io.github.thomaskioko.gradle.plugins.properties.scaffoldProperties
import io.github.thomaskioko.gradle.plugins.utils.compilerOptions
import io.github.thomaskioko.gradle.plugins.utils.getVersionOrNull
import io.github.thomaskioko.gradle.plugins.utils.java
import io.github.thomaskioko.gradle.plugins.utils.javaTarget
import io.github.thomaskioko.gradle.plugins.utils.javaToolchainVersion
import io.github.thomaskioko.gradle.plugins.utils.jvmTarget
import io.github.thomaskioko.gradle.plugins.utils.kotlin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/**
 * Common Kotlin and JVM configuration applied to every subproject in the suite.
 *
 * Every other subproject plugin (`app`, `android`, `jvm`, `multiplatform`) applies this plugin
 * itself, so consumers do not apply it directly. Applying any of those plugins without
 * `io.github.thomaskioko.gradle.plugins.root` on the root project throws a `GradleException` at
 * apply-time so the missing root setup is caught early.
 *
 * The plugin registers the `scaffold {}` extension, applies Spotless, configures the JVM and
 * Kotlin toolchains, sets the project-wide compiler flags (explicit API mode, language version,
 * progressive mode, opt-ins, JVM target), and turns on reproducible JAR output.
 *
 * ```kotlin
 * // settings.gradle.kts (root)
 * plugins {
 *   id("io.github.thomaskioko.gradle.plugins.root")
 * }
 *
 * // module build.gradle.kts
 * plugins {
 *   id("io.github.thomaskioko.gradle.plugins.multiplatform")
 * }
 *
 * scaffold {
 *   addJvmTarget()
 *   addAndroidTarget()
 * }
 * ```
 */
public abstract class BasePlugin : Plugin<Project> {
    public companion object {
        /**
         * Name of the aggregate task that runs every JVM and Linux-runnable test on the host.
         * Subproject plugins attach their variant test tasks to this aggregate via `dependsOn`.
         */
        public const val LINUX_TEST: String = "linuxTest"

        /**
         * Name of the aggregate task that runs the iOS simulator tests. Wired up only on
         * Kotlin Multiplatform modules with iOS targets.
         */
        public const val IOS_TEST: String = "iosTest"

        /**
         * Name of the aggregate task that runs both [LINUX_TEST] and [IOS_TEST] together. Used
         * by CI pipelines that want a single entry point covering every test on every host.
         */
        public const val ALL_TEST: String = "ciTest"

        internal const val ROOT_PLUGIN_ID: String = "io.github.thomaskioko.gradle.plugins.root"
    }

    override fun apply(target: Project) {
        requireRootPluginApplied(target)

        target.plugins.apply("io.github.thomaskioko.gradle.plugins.spotless")

        target.scaffoldProperties()
        target.extensions.create("scaffold", BaseExtension::class.java)

        target.makeJarsReproducible()
        target.configureJava()
        target.configureKotlin()
        target.configureTests()
    }

    private fun requireRootPluginApplied(target: Project) {
        if (!target.rootProject.plugins.hasPlugin(ROOT_PLUGIN_ID)) {
            throw GradleException(
                "$ROOT_PLUGIN_ID must be applied to the root project. " +
                    "Add `id(\"$ROOT_PLUGIN_ID\")` to the root build.gradle.kts plugins block.",
            )
        }
    }

    private fun Project.configureTests() {
        tasks.withType(Test::class.java).configureEach {
            it.failOnNoDiscoveredTests.set(false)
        }
    }

    private fun Project.makeJarsReproducible() {
        tasks.withType(Jar::class.java).configureEach {
            it.isReproducibleFileOrder = true
            it.isPreserveFileTimestamps = false
        }
    }

    internal fun Project.configureJava() {
        java {
            toolchain {
                it.languageVersion.set(javaToolchainVersion)
                it.vendor.set(JvmVendorSpec.AZUL)
            }
        }
    }

    private fun Project.configureKotlin() {
        kotlin {
            explicitApi()

            jvmToolchain { toolchain ->
                toolchain.languageVersion.set(javaToolchainVersion)
                toolchain.vendor.set(JvmVendorSpec.AZUL)
            }

            val isAndroid = this is KotlinAndroidProjectExtension

            compilerOptions {
                val version = getVersionOrNull("kotlin-language")
                    ?.let(KotlinVersion.Companion::fromVersion) ?: KotlinVersion.Companion.DEFAULT
                languageVersion.set(version)

                // In this mode, some deprecations and bug-fixes for unstable code take effect immediately.
                progressiveMode.set(version >= KotlinVersion.Companion.DEFAULT)

                freeCompilerArgs.addAll(
                    "-Xannotation-default-target=param-property",
                    // https://kotlinlang.org/docs/whatsnew2020.html#data-class-copy-function-to-have-the-same-visibility-as-constructor
                    "-Xconsistent-data-class-copy-visibility",
                    // Enable 2.2.0 feature previews
                    "-Xcontext-parameters",
                    "-Xcontext-sensitive-resolution",
                    // Enable using @all:... annotation use site target
                    // https://kotlinlang.org/docs/annotations.html#all-meta-target
                    "-Xannotation-target-all",
                    // Enable unused return value checks for annotated methods
                    // TODO: change to full which changes it from opt-out by adding @IgnorableReturnValue instead of opt-in by adding @MustUseReturnValues
                    // https://kotlinlang.org/docs/whatsnew23.html#unused-return-value-checker
                    "-Xreturn-value-checker=check",
                    // Makes it possible to use reified exception types in catch clauses
                    // https://kotlinlang.org/docs/whatsnew2220.html#support-for-reified-types-in-catch-clauses
                    "-Xallow-reified-type-in-catch",
                    // opt in to experimental apis
                    // https://kotlinlang.org/docs/whatsnew23.html#explicit-backing-fields
                    "-Xexplicit-backing-fields",
                    "-opt-in=kotlin.time.ExperimentalTime",
                    "-opt-in=kotlin.uuid.ExperimentalUuidApi",
                )

                if (this is KotlinJvmCompilerOptions) {
                    jvmTarget.set(project.jvmTarget)

                    freeCompilerArgs.addAll(
                        "-jvm-default=enable",
                        "-Xassertions=jvm",
                    )

                    if (!isAndroid) {
                        freeCompilerArgs.add("-Xjdk-release=${project.javaTarget}")
                    }
                }
            }
        }
    }
}
