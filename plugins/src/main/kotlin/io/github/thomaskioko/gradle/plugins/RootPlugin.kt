package io.github.thomaskioko.gradle.plugins

import com.autonomousapps.DependencyAnalysisExtension
import com.osacky.doctor.DoctorExtension
import io.github.thomaskioko.gradle.plugins.DependencyExclusions.incorrectConfiguration
import io.github.thomaskioko.gradle.plugins.DependencyExclusions.unusedDependencies
import io.github.thomaskioko.gradle.plugins.DependencyExclusions.usedTransitive
import io.github.thomaskioko.gradle.plugins.utils.booleanProperty
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.buildconfiguration.tasks.UpdateDaemonJvm
import org.gradle.jvm.toolchain.JvmVendorSpec

/**
 * Dependency exclusion lists for dependency analysis plugin.
 * Extracted as constants to avoid recreating these lists for each project configuration.
 */
private object DependencyExclusions {
    val incorrectConfiguration = listOf(
        "org.jetbrains.kotlin:kotlin-stdlib",
        "androidx.core:core-ktx",
        "androidx.lifecycle:lifecycle-runtime-ktx",
        "io.coil-kt:coil-compose",
    )

    val unusedDependencies = listOf(
        "io.coil-kt:coil-compose",
        "io.coil-kt:coil-compose-base",
        "androidx.compose.foundation:foundation",
    )

    val usedTransitive = listOf(
        // Common Kotlin dependencies
        "org.jetbrains.kotlin:kotlin-stdlib",
        // Common Compose dependencies
        "androidx.compose.animation:animation",
        "androidx.compose.material:material-icons-core",
        "androidx.compose.ui:ui-tooling-preview",
        "androidx.compose.ui:ui",
        // Common libraries
        "androidx.lifecycle:lifecycle-runtime-compose",
        "androidx.lifecycle:lifecycle-runtime",
        "org.jetbrains.kotlinx:kotlinx-collections-immutable",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core",
        "dev.icerock.moko:resources",
        "dev.icerock.moko:resources-compose",
        "androidx.compose.material:material-icons-extended",
        // Common Android libraries
        "androidx.activity:activity",
        "androidx.paging:paging-common",
        "androidx.sqlite:sqlite",
        "androidx.datastore:datastore-core",
        // Additional dependencies from the report
        "com.squareup.okhttp3:okhttp",
        "org.jetbrains.kotlinx:kotlinx-serialization-json",
        "app.cash.sqldelight:android-driver",
        "app.cash.sqldelight:runtime",
        "app.cash.sqldelight:sqlite-driver",
        "com.arkivanov.decompose:decompose",
        "androidx.paging:paging-common",
        // Common test dependencies
        "junit:junit",
        "androidx.test.ext:junit",
        "io.kotest:kotest-assertions-shared",
    )
}

/**
 * `RootPlugin` is a base Gradle plugin that configures common settings for all subprojects.
 */
public abstract class RootPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        require(project == project.rootProject) {
            "Root plugin should only be applied on the root project!"
        }

        plugins.apply("com.autonomousapps.dependency-analysis")

        configureDaemonToolchainTask()
        configureDependencyAnalysis()
        configureGradleDoctor()
    }

    @Suppress("UnstableApiUsage")
    private fun Project.configureDaemonToolchainTask() {
        tasks.withType(UpdateDaemonJvm::class.java).configureEach {
            it.vendor.set(JvmVendorSpec.AZUL)
        }
    }

    private fun Project.configureGradleDoctor() {
        pluginManager.withPlugin("com.osacky.doctor") {
            extensions.configure(DoctorExtension::class.java) { doctor ->
                with(doctor) {
                    /**
                     * Do not allow building all apps simultaneously. This is likely not what the user intended.
                     */
                    allowBuildingAllAndroidAppsSimultaneously.set(false)

                    /**
                     * Warn if using Android Jetifier. It slows down builds.
                     */
                    warnWhenJetifierEnabled.set(true)

                    /**
                     * The level at which to warn when a build spends more than this percent garbage collecting.
                     */
                    GCWarningThreshold.set(0.10f)

                    javaHome { handler ->
                        with(handler) {
                            /**
                             * Ensure that we are using JAVA_HOME to build with this Gradle.
                             */
                            ensureJavaHomeMatches.set(true)

                            /**
                             * Ensure we have JAVA_HOME set.
                             */
                            ensureJavaHomeIsSet.set(true)

                            /**
                             * Fail on any `JAVA_HOME` issues.
                             */
                            failOnError.set(booleanProperty("java.toolchains.strict", false))
                        }
                    }
                }
            }
        }
    }

    private fun Project.configureDependencyAnalysis() {
        extensions.configure(DependencyAnalysisExtension::class.java) { analysis ->
            analysis.useTypesafeProjectAccessors(true)

            analysis.issues { issues ->
                issues.all { project ->
                    project.onAny {
                        it.severity("fail")
                    }

                    project.onIncorrectConfiguration {
                        it.exclude(*incorrectConfiguration.toTypedArray())
                    }

                    project.onRedundantPlugins {
                        it.severity("fail")
                    }

                    project.onUnusedDependencies {
                        it.exclude(*unusedDependencies.toTypedArray())
                    }

                    project.onUsedTransitiveDependencies {
                        it.severity("warn")
                        it.exclude(*usedTransitive.toTypedArray())
                    }
                }
            }

            analysis.structure { structure ->
                structure.ignoreKtx(true)

                structure.bundle("androidx-compose-runtime") {
                    it.primary("androidx.compose.runtime:runtime")
                    it.includeGroup("androidx.compose.runtime")
                }
                structure.bundle("androidx-compose-ui") {
                    it.primary("androidx.compose.ui:ui")
                    it.includeGroup("androidx.compose.ui")
                    it.includeDependency("androidx.compose.runtime:runtime-saveable")
                }
                structure.bundle("androidx-compose-foundation") {
                    it.primary("androidx.compose.foundation:foundation")
                    it.includeGroup("androidx.compose.animation")
                    it.includeGroup("androidx.compose.foundation")
                }
                structure.bundle("androidx-compose-material") {
                    it.primary("androidx.compose.material:material")
                    it.includeGroup("androidx.compose.material")
                }
                structure.bundle("androidx-compose-material3") {
                    it.primary("androidx.compose.material3:material3")
                    it.includeGroup("androidx.compose.material3")
                }

                structure.bundle("coil") {
                    it.primary("io.coil-kt:coil-compose")
                    it.includeGroup("io.coil-kt")
                }
            }
        }
    }
}
