package io.github.thomaskioko.gradle.plugins.extensions

import androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerExtension
import com.android.build.api.dsl.AndroidResources
import com.android.build.api.dsl.ApplicationAndroidResources
import com.android.build.api.dsl.LibraryAndroidResources
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.api.dsl.TestExtension
import com.android.build.api.dsl.TestOptions
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.HasHostTests
import io.github.thomaskioko.gradle.plugins.utils.android
import io.github.thomaskioko.gradle.plugins.utils.androidApp
import io.github.thomaskioko.gradle.plugins.utils.androidComponents
import io.github.thomaskioko.gradle.plugins.utils.androidLibrary
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import io.github.thomaskioko.gradle.plugins.utils.isDebugOnlyBuild
import io.github.thomaskioko.gradle.plugins.utils.setupCompose
import org.gradle.api.Project

public abstract class AndroidExtension(protected val project: Project) {

    internal var androidTestsEnabled: Boolean = false
        private set

    public fun enableAndroidTests() {
        androidTestsEnabled = true
    }

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
        project.androidLibrary {
            buildFeatures {
                buildConfig = true
            }
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

    /**
     * Sets manifest placeholders on every variant. Works for both pure Android
     * library modules and KMP Android library targets because it goes through
     * the common `AndroidComponentsExtension.onVariants` variant API rather
     * than the legacy `defaultConfig.manifestPlaceholders` DSL (which is
     * absent on `KotlinMultiplatformAndroidLibraryExtension`).
     *
     * Typical use: overriding placeholders required by third-party library
     * manifests (e.g. AppAuth's `${appAuthRedirectScheme}`) in a test module
     * where the real value is not meaningful.
     *
     * Example:
     * ```
     * scaffold {
     *     android {
     *         manifestPlaceholders(mapOf("appAuthRedirectScheme" to "com.example.test"))
     *     }
     * }
     * ```
     */
    public fun manifestPlaceholders(placeholders: Map<String, String>) {
        project.androidComponents {
            onVariants { variant ->
                placeholders.forEach { (key, value) ->
                    variant.manifestPlaceholders.put(key, value)
                }
                // Host test variants (KMP Android library `withHostTestBuilder`
                // or AGP unit test) have their own manifest merge — the
                // placeholders on the main variant do not propagate. Apply
                // them explicitly to every host test component.
                (variant as? HasHostTests)?.hostTests?.values?.forEach { hostTest ->
                    placeholders.forEach { (key, value) ->
                        hostTest.manifestPlaceholders.put(key, value)
                    }
                }
                // Device test variants (KMP `withDeviceTestBuilder` or AGP
                // androidTest) also merge their own manifests independently
                // of the main variant. Apply placeholders so dependencies
                // such as AppAuth resolve in instrumentation builds.
                (variant as? HasDeviceTests)?.deviceTests?.values?.forEach { deviceTest ->
                    placeholders.forEach { (key, value) ->
                        deviceTest.manifestPlaceholders.put(key, value)
                    }
                }
            }
        }
    }

    public fun useRoborazzi() {
        project.plugins.apply("io.github.takahirom.roborazzi")

        project.dependencies.apply {
            add("testImplementation", project.getDependency("androidx-compose-ui-test"))
            add("testImplementation", project.getDependency("robolectric"))
            add("testRuntimeOnly", project.getDependency("roborazzi"))
        }
    }

    public fun useComposeTests() {
        project.dependencies.apply {
            add("testImplementation", project.getDependency("androidx-compose-ui-test"))
            add("testImplementation", project.getDependency("robolectric"))
            add("testRuntimeOnly", project.getDependency("androidx-compose-ui-test-manifest"))
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
        val configureManagedDevices: TestOptions.() -> Unit = {
            managedDevices {
                allDevices.register(deviceName, ManagedVirtualDevice::class.java) {
                    it.device = device
                    it.apiLevel = apiLevel
                    it.systemImageSource = systemImageSource
                }
            }
        }

        when {
            project.plugins.hasPlugin("com.android.application") ->
                project.androidApp { testOptions(configureManagedDevices) }

            project.plugins.hasPlugin("com.android.library") ->
                project.androidLibrary { testOptions(configureManagedDevices) }

            project.plugins.hasPlugin("com.android.test") ->
                project.extensions.configure(TestExtension::class.java) {
                    it.testOptions(
                        configureManagedDevices,
                    )
                }
        }
    }

    public fun libraryConfiguration(configuration: LibraryExtension.() -> Unit) {
        project.extensions.configure(LibraryExtension::class.java) { extension ->
            extension.configuration()
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
