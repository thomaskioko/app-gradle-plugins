package io.github.thomaskioko.gradle.plugins

import com.autonomousapps.DependencyAnalysisExtension
import com.osacky.doctor.DoctorExtension
import io.github.thomaskioko.gradle.plugins.analysis.AnalysisExclusions
import io.github.thomaskioko.gradle.plugins.properties.scaffoldProperties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.buildconfiguration.tasks.UpdateDaemonJvm
import org.gradle.jvm.toolchain.JvmVendorSpec

/**
 * `RootPlugin` is a base Gradle plugin that configures common settings for all subprojects.
 */
public abstract class RootPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        require(project == project.rootProject) {
            "Root plugin should only be applied on the root project!"
        }

        plugins.apply("com.autonomousapps.dependency-analysis")

        scaffoldProperties()
        configureAggregateTestTasks()
        configureDaemonToolchainTask()
        configureDependencyAnalysis()
        configureGradleDoctor()
    }

    private fun Project.configureAggregateTestTasks() {
        val linuxTest = tasks.register(BasePlugin.LINUX_TEST)
        val macTest = tasks.register(BasePlugin.IOS_TEST)

        tasks.register(BasePlugin.ALL_TEST) {
            it.dependsOn(linuxTest)
            it.dependsOn(macTest)
        }
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
                            failOnError.set(scaffoldProperties().javaToolchainsStrict)
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
                        it.exclude(*AnalysisExclusions.incorrectConfiguration.toTypedArray())
                    }

                    project.onRedundantPlugins {
                        it.severity("fail")
                    }

                    project.onUnusedDependencies {
                        it.exclude(*AnalysisExclusions.unusedDependencies.toTypedArray())
                    }

                    project.onUsedTransitiveDependencies {
                        it.severity("warn")
                        it.exclude(*AnalysisExclusions.usedTransitive.toTypedArray())
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
                structure.bundle("androidx-compose-material-icons") {
                    it.primary("androidx.compose.material:material-icons-core")
                    it.includeDependency("androidx.compose.material:material-icons-extended")
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
