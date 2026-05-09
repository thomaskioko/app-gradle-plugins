package io.github.thomaskioko.gradle.plugins

import androidx.baselineprofile.gradle.producer.BaselineProfileProducerExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.api.variant.TestAndroidComponentsExtension
import io.github.thomaskioko.gradle.plugins.extensions.AndroidExtension
import io.github.thomaskioko.gradle.plugins.setup.configureCommonAndroid
import io.github.thomaskioko.gradle.plugins.utils.baseExtension
import io.github.thomaskioko.gradle.plugins.utils.isDebugOnlyBuild
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Configures a benchmark module that produces an Android baseline profile for an app.
 *
 * Apply this plugin on a `com.android.test` benchmark module paired with the consumer app
 * module. The plugin applies `com.android.test`, chains [BasePlugin], and registers the
 * `benchmark` sub-DSL ([io.github.thomaskioko.gradle.plugins.extensions.AndroidExtension]) on
 * `scaffold {}`. It points the test module at the app module via `targetProjectPath = ":app"`,
 * sets `androidx.test.runner.AndroidJUnitRunner` as the instrumentation runner, and applies the
 * project's common Android settings.
 *
 * On non-debug builds the plugin also applies `androidx.baselineprofile`, configures the producer
 * extension to run on the registered Pixel 6 managed device, disables connected devices, and
 * forwards the target app ID into the instrumentation runner arguments so the produced profile
 * targets the right package. Skipped entirely when the project is in debug-only mode.
 *
 * ```kotlin
 * plugins {
 *   id("io.github.thomaskioko.gradle.plugins.baseline.profile")
 * }
 * ```
 */
public class BaselineProfilePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("com.android.test")
        target.plugins.apply(BasePlugin::class.java)

        target.baseExtension.extensions.create("benchmark", AndroidExtension::class.java)

        target.configureCommonAndroid()
        target.basicTestConfiguration()

        // Only configure baseline profiling when not in debug-only mode
        if (!target.isDebugOnlyBuild()) {
            target.plugins.apply("androidx.baselineprofile")
            target.fullComponentsConfiguration()
        }
    }

    private fun Project.basicTestConfiguration() {
        extensions.configure(TestExtension::class.java) { extension ->
            extension.defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            extension.targetProjectPath = ":app"
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Project.fullComponentsConfiguration() {
        extensions.configure(BaselineProfileProducerExtension::class.java) {
            it.managedDevices += "pixel6Api34"
            it.useConnectedDevices = false
            it.enableEmulatorDisplay = false
        }

        extensions.configure(TestAndroidComponentsExtension::class.java) { components ->
            components.onVariants { variant ->
                val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
                variant.instrumentationRunnerArguments.put(
                    "targetAppId",
                    variant.testedApks.map { artifactsLoader.load(it)?.applicationId },
                )
            }
        }
    }
}
