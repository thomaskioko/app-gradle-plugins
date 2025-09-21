package io.github.thomaskioko.gradle.plugins.extensions

import androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerExtension
import com.android.build.api.dsl.AndroidResources
import com.android.build.api.dsl.ApplicationAndroidResources
import com.android.build.api.dsl.LibraryAndroidResources
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ManagedVirtualDevice
import io.github.thomaskioko.gradle.plugins.utils.android
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import io.github.thomaskioko.gradle.plugins.utils.isDebugOnlyBuild
import io.github.thomaskioko.gradle.plugins.utils.setupCompose
import org.gradle.api.Project

public abstract class AndroidExtension(private val project: Project) {

    public fun minSdkVersion(minSdkVersion: Int?) {
        if (minSdkVersion != null) {
            project.android {
                defaultConfig.minSdk = minSdkVersion
            }
        }
    }

    public fun enableAndroidResources() {
        project.android {
            androidResources.enable = true
        }
    }

    public fun enableBuildConfig() {
        project.android {
            buildFeatures.buildConfig = true
        }
    }

    public fun consumerProguardFiles(vararg files: String) {
        project.android {
            (this as LibraryExtension).defaultConfig.consumerProguardFiles(*files)
        }
    }

    public fun useCompose() {
        project.setupCompose()
    }


    public fun useRoborazzi() {
        project.plugins.apply("io.github.takahirom.roborazzi")

        project.android {
            testOptions.unitTests.isIncludeAndroidResources = true
        }

        project.dependencies.apply {
            add("testImplementation", project.getDependency("androidx-compose-ui-test"))
            add("testImplementation", project.getDependency("robolectric"))
            add("testRuntimeOnly", project.getDependency("roborazzi"))
        }
    }

    /**
     * Configures baseline profiles for optimizing runtime performance. Only applied on release builds.
     *
     * @param benchmarkProject Benchmark project for generating baseline profiles
     */
    public fun useBaselineProfile(benchmarkProject: Any? = null) {
        // Only apply baseline profile plugin for release builds
        if (!project.isDebugOnlyBuild()) {
            project.plugins.apply("androidx.baselineprofile")

            project.dependencies.apply {
                add("runtimeOnly", project.getDependency("androidx-profileinstaller"))
                benchmarkProject?.let { benchmark ->
                    add("baselineProfile", benchmark)
                }
            }

            project.extensions.configure(BaselineProfileConsumerExtension::class.java) {
                it.mergeIntoMain = true
                it.saveInSrc = true
            }
        }
    }

    @Suppress("UnstableApiUsage")
    public fun useManagedDevices(
        deviceName: String = "pixel6Api34",
        device: String = "Pixel 6",
        apiLevel: Int = 34,
        systemImageSource: String = "aosp",
    ) {
        project.android {
            testOptions {
                managedDevices {
                    allDevices.register(deviceName, ManagedVirtualDevice::class.java) {
                        it.device = device
                        it.apiLevel = apiLevel
                        it.systemImageSource = systemImageSource
                    }
                }
            }
        }
    }
}

internal var AndroidResources.enable: Boolean
    get() = when (this) {
        is LibraryAndroidResources -> enable
        is ApplicationAndroidResources -> true
        else -> throw UnsupportedOperationException("")
    }
    set(value) = when (this) {
        is LibraryAndroidResources -> enable = value
        is ApplicationAndroidResources -> {}
        else -> throw UnsupportedOperationException("")
    }
