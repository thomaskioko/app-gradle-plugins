# Installing

Everything on this page was pasted into an empty project and built before being written down, in
the order it appears. Follow it top to bottom and the last step compiles.

## Before you start

Gradle has to run on Java 21 or newer. On an older one the build fails while resolving the plugin
itself, with a message about the JVM runtime version rather than about the plugin.

## Repositories

The plugins live on Maven Central, which the Gradle plugin portal already reads, so the portal on
its own is enough to find them. It is not enough to build with them. They depend on the Android
Gradle plugin, which is published only to Google's repository, so without `google()` the build
fails while resolving `com.android.tools.build:gradle`. That names a plugin you never asked for,
which is a confusing place to end up.

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

The plugins read versions from a catalog named `libs`, so `gradle/libs.versions.toml` has to
exist even in a project that would not otherwise have one.

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

Options inside `scaffold {}` read further entries as they are switched on. `useMetro()` reads
`metro-runtime`, `useCodegen()` reads `codegen-annotations` and `codegen-processor`, and so on.
Each option's documentation names what it looks for.

## Root project

The root project does two jobs. It applies the root plugin, which every other plugin in the suite
checks for and fails without. It also names every plugin any module will use, so that a module can
apply one without repeating the version.

Everything a module applies is declared here with `apply false`. That puts it on the build
classpath without applying it to the root project. Leave one out and the module that applies it
fails with a message about the plugin already being on the classpath with an unknown version.

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

## What gets applied for you

Three declarations cover the whole suite because these plugins ship together. Naming
`com.android.library` puts every Android plugin on the classpath, and naming one Kotlin plugin
puts the rest there too.

Applied to every module: Spotless and dependency analysis.

Applied by the plugin you chose: `com.android.application` for `app`, `com.android.library` and
`com.android.lint` for `android`, `org.jetbrains.kotlin.jvm` for `jvm`, and
`org.jetbrains.kotlin.multiplatform` for `multiplatform`.

Applied only when you ask for them, through `scaffold {}`: KSP, Metro, Compose, Kotlin
serialization, Roborazzi, dependency guard, baseline profiles, Google Services and Crashlytics.
Each option's documentation says what it applies and what it reads from the catalog.

## Android namespace

Android modules build their namespace from the module path and one property. Add it to
`gradle.properties`:

```properties
package.name=com.example.myapp
```

## A module

Apply one plugin per module and describe the module through `scaffold {}`.

```kotlin
plugins {
    alias(libs.plugins.app.kmp)
}

scaffold {
    useMetro()
}
```

The four platform plugins are `app` for an Android application, `android` for an Android library,
`jvm` for a plain Kotlin library, and `multiplatform` for a Kotlin Multiplatform library. Pick one
per module.

## A note on formatting

The suite runs Spotless over your build files as well as your source, using four spaces for
indentation. Every sample here is written that way, so pasting one leaves the build green. Paste
something indented with two spaces and the first build reports a formatting violation in the file
you just wrote.

## What to read next

Every plugin, every option in `scaffold {}` and every annotation is covered in the
[API reference](api/plugins/index.html), generated from the source itself.
