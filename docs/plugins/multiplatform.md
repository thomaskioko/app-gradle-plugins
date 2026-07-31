# Multiplatform

`io.github.thomaskioko.gradle.plugins.multiplatform`

For a Kotlin Multiplatform module. Applies the Kotlin Multiplatform plugin and Base. Unlike the
other three, this one adds no targets on its own, because which platforms a module builds for is
the decision it exists to make.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.multiplatform")
}

scaffold {
    addJvmTarget()
    addIosTargets()
    addAndroidTarget()

    useMetro()
}
```

## Targets

| Option | What it does |
|---|---|
| `addJvmTarget()` | Adds the JVM target |
| `addIosTargets(includeX64 = false)` | Adds `iosArm64` and `iosSimulatorArm64`, and `iosX64` on request |
| `addIosTargetsWithXcFramework(frameworkName, includeX64 = false, configure = {})` | The same, bundled into one static XCFramework |
| `configureNativeTargets(bundleId = null, configure = {})` | Applies shared compiler and linker options to every native target |

## The Android target

`addAndroidTarget()` takes five parameters, all with defaults, so calling it bare is the common
case.

```kotlin
scaffold {
    addAndroidTarget(
        enableAndroidResources = false,
        withDeviceTestBuilder = false,
        withJava = false,
        configure = { },
        lintConfiguration = { },
    )
}
```

| Parameter | What it does |
|---|---|
| `enableAndroidResources` | Turns on resource processing for the target |
| `withDeviceTestBuilder` | Adds the instrumentation test source set and its runner |
| `withJava` | Adds Java sources to the target |
| `configure` | The same options as the [Android](android.md) page |
| `lintConfiguration` | Reaches the lint settings for this target |

The `configure` block and the separate `android { }` block reach the same options. Use `android { }`
when the call would otherwise be long enough to bury the target list.

Full detail in the
[API reference](../api/plugins/plugins/io.github.thomaskioko.gradle.plugins.extensions/-base-extension/index.html).
