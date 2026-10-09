# Installing

This page lists the settings a project needs before its first module builds. We pasted each step
into an empty project and built it, in the order shown here. Follow it top to bottom and the last
step compiles.

## Requirements

Gradle has to run on Java 21 or newer. On an older JVM the build fails while resolving the plugin
itself. The error talks about the JVM runtime version, not about the plugin.

## Repositories

The plugins are published to Maven Central. The Gradle plugin portal already reads Maven Central,
so the portal alone is enough to find them. It isn't enough to build with them, though. They
depend on the Android Gradle plugin, which is only published to Google's repository. Without
`google()`, the build fails while resolving `com.android.tools.build:gradle`. That names a plugin
you never asked for, which is confusing.

Add both to `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
```

## Version catalog

The plugins read versions from a catalog named `libs`. So `gradle/libs.versions.toml` has to
exist, even in a project that wouldn't otherwise have one.

Three versions are always required. A module targeting Android needs three more.

```toml
[versions]
--8<-- "catalog-version.md"

java-target = "21"
java-toolchain = "21"
ktlint = "1.8.0"

# Android modules only
android-compile = "36"
android-min = "26"
android-target = "36"

# The plugins the suite applies on your behalf, see the next section
agp = "9.3.1"
kotlin = "2.4.10"
spotless = "8.9.0"

[plugins]
app-root = { id = "io.github.thomaskioko.gradle.plugins.root", version.ref = "app-gradle-plugins" }
app-android = { id = "io.github.thomaskioko.gradle.plugins.android", version.ref = "app-gradle-plugins" }
app-application = { id = "io.github.thomaskioko.gradle.plugins.app", version.ref = "app-gradle-plugins" }
app-jvm = { id = "io.github.thomaskioko.gradle.plugins.jvm", version.ref = "app-gradle-plugins" }
app-kmp = { id = "io.github.thomaskioko.gradle.plugins.multiplatform", version.ref = "app-gradle-plugins" }
app-baseline-profile = { id = "io.github.thomaskioko.gradle.plugins.baseline.profile", version.ref = "app-gradle-plugins" }
app-buildconfig = { id = "io.github.thomaskioko.gradle.plugins.buildconfig", version.ref = "app-gradle-plugins" }
app-lint = { id = "io.github.thomaskioko.gradle.plugins.lint", version.ref = "app-gradle-plugins" }
app-resource-generator = { id = "io.github.thomaskioko.gradle.plugins.resource.generator", version.ref = "app-gradle-plugins" }
app-spotless = { id = "io.github.thomaskioko.gradle.plugins.spotless", version.ref = "app-gradle-plugins" }

android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
```

Options inside `scaffold {}` read more entries when you switch them on. `useMetro()` reads
`metro-runtime`, `useCodegen()` reads `codegen-annotations` and `codegen-processor`, and so on.
Each option's documentation names the entries it looks for.

## Root project

The root project does two jobs. First, it applies the root plugin. Every other plugin in the
suite checks for it and fails without it. Second, it names every plugin any module will use. That
way a module can apply one without repeating the version.

You declare everything a module applies here, with `apply false`. That puts the plugin on the
build classpath without applying it to the root project. If you leave one out, the module that
applies it fails. The error says the plugin is already on the classpath with an unknown version.

```kotlin
plugins {
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false

    alias(libs.plugins.app.root)
    alias(libs.plugins.app.android) apply false
    alias(libs.plugins.app.jvm) apply false
    alias(libs.plugins.app.kmp) apply false
}
```

## Applied plugins

Three declarations cover the whole suite, because these plugins ship together. Naming
`com.android.library` puts every Android plugin on the classpath. Naming one Kotlin plugin puts
the rest there too.

Applied to every module: Spotless and dependency analysis.

Applied by the plugin you chose: `com.android.application` for `app`, `com.android.library` and
`com.android.lint` for `android`, `org.jetbrains.kotlin.jvm` for `jvm`, and
`org.jetbrains.kotlin.multiplatform` for `multiplatform`.

Applied only when you ask for them in `scaffold {}`: KSP, Metro, Compose, Kotlin serialization,
Roborazzi, dependency guard, baseline profiles, Google Services and Crashlytics. Each option's
documentation says what it applies and what it reads from the catalog.

## Android namespace

Android modules build their namespace from the module path plus one property. Add it to
`gradle.properties`:

```properties
package.name=com.example.myapp
```

## Module setup

Each module applies one plugin and describes itself through `scaffold {}`.

```kotlin
plugins {
    alias(libs.plugins.app.kmp)
}

scaffold {
    useMetro()
}
```

There are four platform plugins. `app` is for an Android application, `android` for an Android
library, `jvm` for a plain Kotlin library, and `multiplatform` for a Kotlin Multiplatform library.
Pick one per module.

## Formatting

The suite runs Spotless over your build files as well as your source, with four spaces for
indentation. Every sample here uses four spaces, so pasting one keeps the build green. If you
paste something indented with two spaces, the first build reports a formatting violation in the
file you just wrote.

## Next steps

The [API reference](api/plugins/index.html) covers every plugin, every option in `scaffold {}` and
every annotation. We generate it from the source.
