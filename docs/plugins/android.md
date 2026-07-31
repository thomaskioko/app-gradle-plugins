# Android

`io.github.thomaskioko.gradle.plugins.android`

For an Android library module. Applies the Android library plugin, the Kotlin plugin and Base, and
adds the `android {}` block inside `scaffold {}`.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.android")
}

scaffold {
    android {
        useCompose()
    }
}
```

The namespace comes from the module path and the `package.name` property, so no module sets one.

A Kotlin Multiplatform module reaches the same options by calling `addAndroidTarget()` first. See
the [Multiplatform](multiplatform.md) page.

## Options

| Option | What it does |
|---|---|
| `useCompose()` | Applies the Compose compiler plugin and adds the Compose runtime |
| `useComposeTests()` | Adds Compose UI test and Robolectric dependencies for running UI tests on the JVM |
| `useRoborazzi()` | Applies Roborazzi and adds the Compose UI test, Robolectric and Roborazzi dependencies |
| `enableAndroidTests(testInstrumentationRunner = null, clearPackageData = true)` | Opts into instrumentation tests and applies the test orchestrator |
| `useManagedDevices(deviceName = "pixel6Api34", device = "Pixel 6", apiLevel = 34, systemImageSource = ...)` | Registers a managed virtual device for instrumentation tests |
| `useBaselineProfile(benchmarkProject = null)` | Applies the baseline profile consumer plugin and connects the benchmark module |
| `minSdkVersion(minSdkVersion)` | Overrides the minimum SDK for this module |
| `enableAndroidResources()` | Turns on resource processing |
| `enableBuildConfig()` | Turns on the Android `BuildConfig` feature |
| `consumerProguardFiles(vararg files)` | Bundles the named rules into consumers of this library |
| `manifestPlaceholders(placeholders)` | Sets manifest placeholders on every variant |
| `libraryConfiguration(configuration)` | Reaches the underlying Android extension for anything not listed here |

## Core library desugaring

Declare `android-desugarJdkLibs` in your version catalog and the plugin turns on core library
desugaring and adds the dependency for you. Leave it out and desugaring stays off, which is the
right default for a module that does not need it.

```toml
[libraries]
android-desugarJdkLibs = { module = "com.android.tools:desugar_jdk_libs", version = "2.1.5" }
```

Full detail in the
[API reference](../api/plugins/plugins/io.github.thomaskioko.gradle.plugins.extensions/-android-extension/index.html).
