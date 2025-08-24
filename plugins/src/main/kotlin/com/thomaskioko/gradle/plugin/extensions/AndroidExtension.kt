package com.thomaskioko.gradle.plugin.extensions

import androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ManagedVirtualDevice
import com.thomaskioko.gradle.plugin.utils.android
import com.thomaskioko.gradle.plugin.utils.getDependency
import com.thomaskioko.gradle.plugin.utils.setupCompose
import org.gradle.api.Project

public abstract class AndroidExtension(private val project: Project) {

    public fun minSdkVersion(minSdkVersion: Int?) {
        if (minSdkVersion != null) {
            project.android {
                defaultConfig.minSdk = minSdkVersion
            }
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

    public fun useBaselineProfile() {
        project.plugins.apply("androidx.baselineprofile")

        project.dependencies.apply {
            add("runtimeOnly", project.getDependency("androidx-profileinstaller"))
        }

        project.extensions.configure(BaselineProfileConsumerExtension::class.java) {
            it.mergeIntoMain = true
            it.saveInSrc = true
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
