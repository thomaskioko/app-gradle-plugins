package io.github.thomaskioko.gradle.plugins

import androidx.baselineprofile.gradle.producer.BaselineProfileProducerExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.api.variant.TestAndroidComponentsExtension
import io.github.thomaskioko.gradle.plugins.extensions.AndroidExtension
import io.github.thomaskioko.gradle.plugins.utils.baseExtension
import io.github.thomaskioko.gradle.plugins.utils.configureCommonAndroid
import io.github.thomaskioko.gradle.plugins.utils.isDebugOnlyBuild
import org.gradle.api.Plugin
import org.gradle.api.Project

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
