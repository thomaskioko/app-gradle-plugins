# App Gradle Plugins

A collection of opinionated Gradle plugins for building Kotlin Multiplatform and Android projects with sensible defaults and minimal configuration.

## Installation

### Using Version Catalogs (Recommended)

Add to your `gradle/libs.versions.toml`:

```toml
[versions]
app-plugins = "0.1.1"

[plugins]
app-android = { id = "io.github.thomaskioko.gradle.plugins.android", version.ref = "app-plugins" }
app-app = { id = "io.github.thomaskioko.gradle.plugins.app", version.ref = "app-plugins" }
app-jvm = { id = "io.github.thomaskioko.gradle.plugins.jvm", version.ref = "app-plugins" }
app-multiplatform = { id = "io.github.thomaskioko.gradle.plugins.multiplatform", version.ref = "app-plugins" }
app-root = { id = "io.github.thomaskioko.gradle.plugins.root", version.ref = "app-plugins" }
app-spotless = { id = "io.github.thomaskioko.gradle.plugins.spotless", version.ref = "app-plugins" }
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

Configures a Kotlin Multiplatform module with iOS and JVM targets, with optional Android support.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.multiplatform")
}

scaffold {
    // Enable explicit API mode
    explicitApi()
    
    // Opt-in to experimental APIs
    optIn("kotlin.ExperimentalStdlibApi", "kotlinx.coroutines.ExperimentalCoroutinesApi")
    
    // Add Android target with Compose support
    addAndroidMultiplatformTarget(
        enableAndroidResources = true
    )
    
    // Configure Android-specific settings
    android {
        useCompose()
        minSdkVersion(24)
    }
    
    // Add iOS targets with XCFramework
    addIosTargetsWithXcFramework("SharedKit")
    
    // Enable serialization support
    useSerialization()
    
    // Enable Kotlin Inject for dependency injection
    useKotlinInject()
}
```

### Android Library Plugin

For Android library modules with automatic namespace configuration and optimized defaults.

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
    }
}
```

### Android Application Plugin

For Android application modules with signing configuration and optimization.

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
        useCompose()
        enableBuildConfig()
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
fgp.android.namespacePrefix=com.example

# Version information for apps
app-version-code=1
app-version-name=1.0.0
```

### Version Catalog Setup

Required entries in `libs.versions.toml`:

```toml
[versions]
# Compilation targets
java-target = "17"
java-toolchain = "17"
android-compile = "34"
android-min = "24"
android-target = "34"

[libraries]
# For Compose support
androidx-compose-compiler = { module = "androidx.compose.compiler:compiler", version = "..." }

# For serialization
kotlin-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version = "..." }

# For Kotlin Inject
kotlinInject-compiler = { module = "me.tatarka.inject:kotlin-inject-compiler-ksp", version = "..." }
kotlinInject-runtime = { module = "me.tatarka.inject:kotlin-inject-runtime", version = "..." }

# For Roborazzi testing
androidx-compose-ui-test = { module = "androidx.compose.ui:ui-test-junit4", version = "..." }
robolectric = { module = "org.robolectric:robolectric", version = "..." }
roborazzi = { module = "io.github.takahirom.roborazzi:roborazzi", version = "..." }

# For baseline profiles
androidx-profileinstaller = { module = "androidx.profileinstaller:profileinstaller", version = "..." }
```

## Features

### Automatic Configuration

- **Namespace Generation**: Android namespace is automatically set based on module path
- **Kotlin Options**: Configures optimal Kotlin compiler settings and JVM target
- **Test Configuration**: Unit tests are configured with proper reporting paths
- **Lint Setup**: Android lint is configured with strict rules and centralized reporting
- **Build Optimization**: Release builds are optimized, debug builds prioritize build speed

### Compose Support

The `useCompose()` configuration:
- Enables Compose build feature
- Configures Compose compiler with appropriate version
- Supports both Android and Multiplatform Compose
- Handles Kotlin version compatibility

### Dependency Injection

Built-in support for Kotlin Inject:
- Applies KSP processor
- Configures for all targets in multiplatform
- Adds necessary runtime dependencies

### Testing

- **Roborazzi**: Screenshot testing with automatic configuration
- **Managed Devices**: Configure virtual devices for consistent testing
- **Test Reporting**: Centralized test reports in `build/reports/tests`

## Project Structure

After applying these plugins, your project structure might look like:

```
your-project/
├── gradle.properties         # Plugin configuration
├── gradle/
│   └── libs.versions.toml  # Version catalog
├── app/
│   └── build.gradle.kts    # Uses app plugin
├── feature/
│   ├── api/
│   │   └── build.gradle.kts # Uses multiplatform plugin
│   └── android/
│       └── build.gradle.kts # Uses android plugin
└── core/
    └── build.gradle.kts     # Uses jvm or multiplatform plugin
```

## Migration from Manual Configuration

These plugins replace hundreds of lines of boilerplate configuration. For example:

**Before** (manual configuration):
```kotlin
android {
    namespace = "com.example.feature"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }
    // ... more configuration
}
```

**After** (with plugins):
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

## Publishing

### Prerequisites

1. Register namespace `io.github.thomaskioko` on [Maven Central](https://central.sonatype.com)
2. Configure GPG signing in `~/.gradle/gradle.properties`:

```properties
mavenCentralUsername=your-username
mavenCentralPassword=your-password
signing.keyId=your-key-id
signing.password=your-password
signing.secretKeyRingFile=/path/to/secring.gpg
```

### Publishing Commands

```bash
# Local testing
./gradlew publishToMavenLocal

# Publish to Maven Central
./gradlew publishToMavenCentral
```

## Troubleshooting

### Common Issues

**Plugin not found**: Ensure Maven Central is in your plugin repositories:
```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
```

**Namespace not generated**: Check that `fgp.android.namespacePrefix` is set in `gradle.properties`

**Compose version conflicts**: Ensure `androidx-compose-compiler` version matches your Kotlin version

## License

```
Copyright 2025 Thomas Kioko

Licensed under the Apache License, Version 2.0
```