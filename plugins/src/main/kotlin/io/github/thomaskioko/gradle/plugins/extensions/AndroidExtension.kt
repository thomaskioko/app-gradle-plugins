package io.github.thomaskioko.gradle.plugins.extensions

import androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerExtension
import com.android.build.api.dsl.AndroidResources
import com.android.build.api.dsl.ApplicationAndroidResources
import com.android.build.api.dsl.LibraryAndroidResources
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.api.dsl.TestOptions
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.HasHostTests
import io.github.thomaskioko.gradle.plugins.setup.configureTestOptions
import io.github.thomaskioko.gradle.plugins.setup.setupCompose
import io.github.thomaskioko.gradle.plugins.utils.android
import io.github.thomaskioko.gradle.plugins.utils.androidComponents
import io.github.thomaskioko.gradle.plugins.utils.androidLibrary
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import io.github.thomaskioko.gradle.plugins.utils.isDebugOnlyBuild
import org.gradle.api.Project

/**
 * Configures Android-specific options on a module that has registered an Android target.
 *
 * Reachable through `scaffold { android { ... } }` after either the `android` plugin has applied
 * itself or [BaseExtension.addAndroidTarget] has registered the sub-DSL on a multiplatform
 * module. Each method on this class either configures the Android Gradle Plugin's `android {}`
 * block or registers an opinionated dependency or plugin (Compose, Roborazzi, baseline profiles).
 *
 * ```kotlin
 * scaffold {
 *   addAndroidTarget()
 *   android {
 *     useCompose()
 *     enableAndroidResources()
 *     minSdkVersion(24)
 *   }
 * }
 * ```
 *
 * @property project The Gradle [Project] this extension is attached to.
 */
@ScaffoldDsl
public abstract class AndroidExtension(protected val project: Project) {

    internal var androidTestsEnabled: Boolean = false
        private set

    /**
     * Opts a module into instrumentation tests and applies the project's test orchestrator
     * conventions.
     *
     * Disables animations on the Android emulator, runs tests under the
     * `ANDROIDX_TEST_ORCHESTRATOR` execution mode, and adds an `androidTestUtil` dependency on
     * `androidx-test-orchestrator` resolved from the consumer's version catalog.
     *
     * ```kotlin
     * scaffold {
     *   addAndroidTarget(withDeviceTestBuilder = true)
     *   android {
     *     enableAndroidTests(
     *       testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner",
     *     )
     *   }
     * }
     * ```
     *
     * @param testInstrumentationRunner Fully qualified class name written to
     *   `defaultConfig.testInstrumentationRunner`. When `null`, any existing value is left in
     *   place.
     * @param clearPackageData Pass-through to the orchestrator's `clearPackageData` runner
     *   argument. When `true`, the orchestrator runs `pm clear <pkg>` between tests so each test
     *   starts from a clean app state.
     */
    public fun enableAndroidTests(
        testInstrumentationRunner: String? = null,
        clearPackageData: Boolean = true,
    ) {
        androidTestsEnabled = true

        project.dependencies.add(
            "androidTestUtil",
            project.getDependency("androidx-test-orchestrator"),
        )

        project.android {
            if (testInstrumentationRunner != null) {
                defaultConfig.testInstrumentationRunner = testInstrumentationRunner
            }
            defaultConfig.testInstrumentationRunnerArguments["clearPackageData"] = clearPackageData.toString()
        }

        project.configureTestOptions {
            animationsDisabled = true
            execution = "ANDROIDX_TEST_ORCHESTRATOR"
        }
    }

    /**
     * Sets `defaultConfig.minSdk` on the Android target.
     *
     * Skips the change when [minSdkVersion] is `null`, which lets a consumer guard the call with
     * an optional version catalog entry without splitting the DSL into two branches.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     minSdkVersion(24)
     *   }
     * }
     * ```
     *
     * @param minSdkVersion Minimum SDK version to write to `defaultConfig.minSdk`. When `null`,
     *   the call is a no-op.
     */
    public fun minSdkVersion(minSdkVersion: Int?) {
        if (minSdkVersion != null) {
            project.android {
                defaultConfig.minSdk = minSdkVersion
            }
        }
    }

    /**
     * Enables Android resource processing on the target.
     *
     * Use this in modules that ship resources alongside their code, such as feature modules with
     * drawables, strings, or layout XML. For Kotlin Multiplatform Android library targets, the
     * same flag is exposed as `addAndroidTarget(enableAndroidResources = true)`.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     enableAndroidResources()
     *   }
     * }
     * ```
     */
    public fun enableAndroidResources() {
        project.android {
            androidResources.enable = true
        }
    }

    /**
     * Turns on the Android `BuildConfig` build feature for the library target.
     *
     * Required when consumers use `com.android.build.gradle.api.BuildConfig` constants generated
     * from `defaultConfig.buildConfigField` entries. Off by default to avoid generating an empty
     * `BuildConfig` class for modules that do not need one.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     enableBuildConfig()
     *   }
     * }
     * ```
     */
    public fun enableBuildConfig() {
        project.androidLibrary {
            buildFeatures {
                buildConfig = true
            }
        }
    }

    /**
     * Adds ProGuard or R8 configuration files that are bundled into consumers of this library.
     *
     * Use this for libraries that ship ProGuard rules required by their public API (for example,
     * keep rules for reflection-based serialization). The files are merged into the consumer's
     * release build classpath.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     consumerProguardFiles("consumer-proguard-rules.pro")
     *   }
     * }
     * ```
     *
     * @param files Paths to ProGuard or R8 configuration files relative to the module's project
     *   directory.
     */
    public fun consumerProguardFiles(vararg files: String) {
        project.android {
            (this as LibraryExtension).defaultConfig.consumerProguardFiles(*files)
        }
    }

    /**
     * Applies the Compose Compiler Gradle plugin and adds the Compose runtime dependencies.
     *
     * Configures Compose metrics and reports output for release builds when the consumer enables
     * them, and enables Live Edit support in debug builds. The runtime artifacts are resolved
     * from the consumer's version catalog under the `androidx-compose-bom` and related aliases.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     useCompose()
     *   }
     * }
     * ```
     */
    public fun useCompose() {
        project.setupCompose()
    }

    /**
     * Sets manifest placeholders on every variant of the module.
     *
     * Works on both pure Android library modules and Kotlin Multiplatform Android library
     * targets. Goes through `AndroidComponentsExtension.onVariants` rather than the legacy
     * `defaultConfig.manifestPlaceholders` DSL because the legacy DSL is not present on
     * `KotlinMultiplatformAndroidLibraryExtension`.
     *
     * Apply this when a third-party manifest declares a placeholder that needs a value. AppAuth's
     * `${appAuthRedirectScheme}` is the typical example; the value is fed into the placeholder
     * map below.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     manifestPlaceholders(
     *       mapOf("appAuthRedirectScheme" to "com.example.test"),
     *     )
     *   }
     * }
     * ```
     *
     * @param placeholders Map of placeholder name to value. Each entry is applied to the main
     *   variant, every host test variant, and every device test variant so the placeholder
     *   resolves in unit and instrumentation builds.
     */
    public fun manifestPlaceholders(placeholders: Map<String, String>) {
        project.androidComponents {
            onVariants { variant ->
                placeholders.forEach { (key, value) ->
                    variant.manifestPlaceholders.put(key, value)
                }
                // Host tests (KMP `withHostTestBuilder` or AGP unit test) merge their own
                // manifests independently of the main variant, so the placeholders on the main
                // variant do not propagate. Apply them explicitly to every host test component.
                (variant as? HasHostTests)?.hostTests?.values?.forEach { hostTest ->
                    placeholders.forEach { (key, value) ->
                        hostTest.manifestPlaceholders.put(key, value)
                    }
                }
                // Device tests (KMP `withDeviceTestBuilder` or AGP androidTest) also merge their
                // own manifests independently of the main variant. Apply placeholders so
                // dependencies such as AppAuth resolve in instrumentation builds.
                (variant as? HasDeviceTests)?.deviceTests?.values?.forEach { deviceTest ->
                    placeholders.forEach { (key, value) ->
                        deviceTest.manifestPlaceholders.put(key, value)
                    }
                }
            }
        }
    }

    /**
     * Applies the Roborazzi plugin and adds Compose UI test, Robolectric, and Roborazzi
     * dependencies for screenshot testing.
     *
     * The dependencies land in `testImplementation` and `testRuntimeOnly`, so screenshot tests
     * run on the JVM under Robolectric without needing a device or emulator.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     useRoborazzi()
     *   }
     * }
     * ```
     */
    public fun useRoborazzi() {
        project.plugins.apply("io.github.takahirom.roborazzi")

        project.dependencies.apply {
            add("testImplementation", project.getDependency("androidx-compose-ui-test"))
            add("testImplementation", project.getDependency("robolectric"))
            add("testRuntimeOnly", project.getDependency("roborazzi"))
        }
    }

    /**
     * Adds Compose UI test and Robolectric dependencies for running Compose UI tests on the JVM.
     *
     * Pairs `androidx-compose-ui-test` with the Compose UI test manifest runtime so tests can
     * launch composables under Robolectric. Use this when the module has Compose unit tests but
     * does not need Roborazzi screenshots.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     useComposeTests()
     *   }
     * }
     * ```
     */
    public fun useComposeTests() {
        project.dependencies.apply {
            add("testImplementation", project.getDependency("androidx-compose-ui-test"))
            add("testImplementation", project.getDependency("robolectric"))
            add("testRuntimeOnly", project.getDependency("androidx-compose-ui-test-manifest"))
        }
    }

    /**
     * Applies the AndroidX baseline profile consumer plugin and wires the benchmark project that
     * generates the profile.
     *
     * Skips application on debug-only builds because baseline profiles only optimize release
     * builds. Adds `androidx-profileinstaller` as a runtime dependency so the profile installs at
     * app startup, and configures the consumer extension to merge the profile into `main` and
     * save it in source control.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     useBaselineProfile(benchmarkProject = project(":benchmark"))
     *   }
     * }
     * ```
     *
     * @param benchmarkProject The Gradle project that runs the benchmark and produces the
     *   baseline profile. Pass a `Project` instance from `project(":path")` or `null` to skip
     *   adding the `baselineProfile` configuration dependency.
     */
    public fun useBaselineProfile(benchmarkProject: Any? = null) {
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

    /**
     * Registers an Android Gradle Plugin managed virtual device for instrumentation tests.
     *
     * The default values match a Pixel 6 running API 34 with a system image picked by host
     * architecture: `aosp_atd` on x86_64 hosts (Linux CI runners) and `aosp` on arm64 hosts
     * (Apple Silicon), since Google does not publish ATD images for arm64.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     useManagedDevices(
     *       deviceName = "pixel6Api34",
     *       device = "Pixel 6",
     *       apiLevel = 34,
     *     )
     *   }
     * }
     * ```
     *
     * @param deviceName Identifier under which the device is registered. Reuse the same name
     *   across modules so Gradle deduplicates the device.
     * @param device Display name of the device profile (for example `Pixel 6`). Must match an
     *   AGP-supported device name.
     * @param apiLevel Android API level the device runs.
     * @param systemImageSource System image source identifier (for example `aosp_atd` or
     *   `google`). Defaults to `aosp_atd` on x86_64 and `aosp` on arm64.
     */
    @Suppress("UnstableApiUsage")
    public fun useManagedDevices(
        deviceName: String = "pixel6Api34",
        device: String = "Pixel 6",
        apiLevel: Int = 34,
        systemImageSource: String = defaultSystemImageSource(),
    ) {
        project.configureTestOptions {
            managedDevices {
                allDevices.register(deviceName, ManagedVirtualDevice::class.java) {
                    it.device = device
                    it.apiLevel = apiLevel
                    it.systemImageSource = systemImageSource
                }
            }
        }
    }

    /**
     * Configures the underlying [LibraryExtension] directly for options not exposed by this
     * sub-DSL.
     *
     * Use this as an escape hatch when AGP exposes an option that this extension does not wrap.
     * The lambda receives the same `LibraryExtension` that AGP exposes through the
     * `android {}` block.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     libraryConfiguration {
     *       buildTypes.getByName("debug").enableUnitTestCoverage = true
     *     }
     *   }
     * }
     * ```
     *
     * @param configuration Configuration block applied to the project's [LibraryExtension].
     */
    public fun libraryConfiguration(configuration: LibraryExtension.() -> Unit) {
        project.extensions.configure(LibraryExtension::class.java) { extension ->
            extension.configuration()
        }
    }

    private companion object {
        /**
         * Picks the headless AOSP Automated Test Device (ATD) image on x86_64 hosts (Linux CI
         * runners) and falls back to plain AOSP on arm64 hosts (Apple Silicon), since Google
         * does not publish ATD images for arm64. Override per call site when a project needs a
         * different image source.
         */
        private fun defaultSystemImageSource(): String =
            when (System.getProperty("os.arch")) {
                "aarch64", "arm64" -> "aosp"
                else -> "aosp_atd"
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
