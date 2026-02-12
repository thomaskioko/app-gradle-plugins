# App Gradle Plugins

A collection of opinionated Gradle plugins for building Kotlin Multiplatform and Android projects with sensible defaults and minimal configuration. Compatible with AGP 9.0+.

## Installation

### Using Version Catalogs (Recommended)

Add to your `gradle/libs.versions.toml`:

```toml
[versions]
app-gradle-plugins = "0.1.1"

[plugins]
app-android = { id = "io.github.thomaskioko.gradle.plugins.android", version.ref = "app-gradle-plugins" }
app-app = { id = "io.github.thomaskioko.gradle.plugins.app", version.ref = "app-gradle-plugins" }
app-jvm = { id = "io.github.thomaskioko.gradle.plugins.jvm", version.ref = "app-gradle-plugins" }
app-multiplatform = { id = "io.github.thomaskioko.gradle.plugins.multiplatform", version.ref = "app-gradle-plugins" }
app-root = { id = "io.github.thomaskioko.gradle.plugins.root", version.ref = "app-gradle-plugins" }
app-spotless = { id = "io.github.thomaskioko.gradle.plugins.spotless", version.ref = "app-gradle-plugins" }
```

Then use in your `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.app.multiplatform)
}
```

### Direct Usage

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.multiplatform") version "0.1.1"
}
```

## Plugin Types

### Kotlin Multiplatform Plugin

Configures a Kotlin Multiplatform module with default targets (Android, JVM, iOS) and sensible defaults. Uses `com.android.kotlin.multiplatform.library` for the Android target (AGP 9.0+).

**Default targets provided:** Android (via KMP Android library), JVM, iosArm64, iosSimulatorArm64.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.multiplatform")
}

scaffold {
    // Enable explicit API mode
    explicitApi()

    // Opt-in to experimental APIs
    optIn("kotlin.ExperimentalStdlibApi", "kotlinx.coroutines.ExperimentalCoroutinesApi")

    // Configure the Android target
    addAndroidTarget(
        enableAndroidResources = true,
        withHostTestBuilder = true,
        includeAndroidResources = true,
        configure = {
            useCompose()
            minSdkVersion(24)
        },
        lintConfiguration = {
            baseline = file("lint-baseline.xml")
            disable += "UnusedResources"
        },
    )

    // Add iOS targets with XCFramework
    addIosTargetsWithXcFramework("SharedKit")

    // Configure native target options
    configureNativeTargets(bundleId = "com.example.app")

    // Enable serialization support
    useSerialization()

    // Enable Kotlin Inject for dependency injection
    useKotlinInject()

    // Enable SKIE for Swift interop
    useSkie()

    // Enable Metro for DI
    useMetro()
}
```

#### `addAndroidTarget` Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `enableAndroidResources` | `Boolean` | `false` | Enable Android resources for the KMP Android target |
| `withHostTestBuilder` | `Boolean` | `false` | Enable host-side (JVM) unit tests for Android. Not compatible with some plugins (e.g., moko-resources) |
| `includeAndroidResources` | `Boolean` | `false` | Include Android resources in unit tests (`isIncludeAndroidResources`) |
| `withDeviceTestBuilder` | `Boolean` | `false` | Enable on-device instrumented tests |
| `withJava` | `Boolean` | `false` | Enable Java interop for the Android target |
| `configure` | `AndroidExtension.() -> Unit` | `{}` | Configure Android-specific options (Compose, minSdk, etc.) |
| `lintConfiguration` | `Lint.() -> Unit` | `{}` | Configure lint rules for the Android target |

### Android Library Plugin

For Android library modules with automatic namespace configuration and optimized defaults. Applies `com.android.library` and configures build features, compile options, and test setup.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.android")
}

scaffold {
    android {
        // Enable Compose support
        useCompose()

        // Enable build config generation
        enableBuildConfig()

        // Configure ProGuard consumer rules
        consumerProguardFiles("consumer-rules.pro")

        // Override minimum SDK version
        minSdkVersion(26)

        // Enable baseline profiles for performance optimization
        useBaselineProfile()

        // Configure screenshot testing with Roborazzi
        useRoborazzi()

        // Configure managed virtual devices for testing
        useManagedDevices(
            deviceName = "pixel6Api34",
            device = "Pixel 6",
            apiLevel = 34
        )

        // Custom library extension configuration
        libraryConfiguration {
            // Access LibraryExtension directly
        }
    }
}
```

### Android Application Plugin

For Android application modules with signing configuration and optimization. Automatically enables `buildConfig`.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.app")
}

scaffold {
    app {
        // Set the application ID
        applicationId("com.example.app")

        // Configure build type suffixes
        applicationIdSuffix("debug", ".debug")

        // Enable minification for release builds
        minify(
            file("proguard-rules.pro"),
            file("proguard-android-optimize.txt")
        )
    }

    android {
        // Enable Compose support
        useCompose()

        // Managed devices work for app projects too
        useManagedDevices()
    }
}
```

### Baseline Profile Plugin

For benchmark modules that generate baseline profiles. Uses `com.android.test` plugin.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.baseline-profile")
}

scaffold {
    benchmark {
        // Configure managed devices for baseline profile generation
        useManagedDevices(
            deviceName = "pixel6Api34",
            device = "Pixel 6",
            apiLevel = 34
        )
    }
}
```

### JVM Library Plugin

For pure Kotlin/JVM modules.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.jvm")
}

scaffold {
    jvm {
        // Enable Android Lint for JVM modules
        useAndroidLint()
    }

    // Common configurations work here too
    explicitApi()
    useSerialization()
}
```

### Root Project Plugin

Configures the root project with dependency analysis, version checks, and common tasks.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.root")
}
```

### Spotless Plugin

Automatic code formatting with Spotless using Kotlin and KTX configurations.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.spotless")
}
```

## Configuration Requirements

### Required Gradle Properties

Add to `gradle.properties`:

```properties
# Used to generate Android namespace based on module path
package.name=com.example

# Build Flags
app.debugOnly=true
app.enableIos=false
```

### Version Catalog Setup

Required entries in `libs.versions.toml`:

```toml
[versions]
# Compilation targets
java-target = "17"
java-toolchain = "17"
android-compile = "35"
android-min = "24"
android-target = "35"

[libraries]
# For Compose support
androidx-compose-compiler = { module = "androidx.compose.compiler:compiler", version = "..." }

# For serialization
kotlin-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version = "..." }

# For baseline profiles
androidx-profileinstaller = { module = "androidx.profileinstaller:profileinstaller", version = "..." }

# For desugaring (optional)
android-desugarJdkLibs = { module = "com.android.tools:desugar_jdk_libs", version = "..." }
```

## Features

### Automatic Configuration

- **Namespace Generation**: Android namespace is automatically set based on module path (e.g., `:data:api` becomes `com.example.data.api`)
- **Kotlin Options**: Configures optimal Kotlin compiler settings and JVM target
- **Test Configuration**: Unit tests are configured with proper reporting paths. Release unit tests are disabled by default
- **Lint Setup**: Android lint is configured with strict rules and centralized reporting
- **Build Optimization**: Release builds are optimized, debug builds prioritize build speed
- **Desugaring**: Core library desugaring is automatically enabled when `android-desugarJdkLibs` is in the version catalog

### Compose Support

The `useCompose()` configuration:
- Enables Compose build feature
- Configures Compose compiler with appropriate version
- Supports both Android and Multiplatform Compose
- Available via `scaffold { android { useCompose() } }` for Android/App plugins
- Available via `addAndroidTarget(configure = { useCompose() })` for KMP plugin

### Dependency Injection

Built-in support for Kotlin Inject and Metro:
- `useKotlinInject()` - Applies KSP processor and configures for all targets
- `useMetro()` - Applies Metro plugin and adds runtime dependency

### Swift Interop

Built-in SKIE support via `useSkie()` with configurable interop options for suspend functions, flows, enums, and sealed classes.


## License

```
Copyright 2025 Thomas Kioko

Licensed under the Apache License, Version 2.0
```