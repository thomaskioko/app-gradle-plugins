package io.github.thomaskioko.gradle.plugins

import io.github.thomaskioko.gradle.plugins.extensions.BaseExtension
import io.github.thomaskioko.gradle.plugins.utils.compilerOptions
import io.github.thomaskioko.gradle.plugins.utils.getVersionOrNull
import io.github.thomaskioko.gradle.plugins.utils.java
import io.github.thomaskioko.gradle.plugins.utils.javaTarget
import io.github.thomaskioko.gradle.plugins.utils.javaToolchainVersion
import io.github.thomaskioko.gradle.plugins.utils.jvmTarget
import io.github.thomaskioko.gradle.plugins.utils.kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

public abstract class BasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("io.github.thomaskioko.gradle.plugins.spotless")

        target.extensions.create("scaffold", BaseExtension::class.java)

        target.makeJarsReproducible()
        target.configureJava()
        target.configureKotlin()
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
