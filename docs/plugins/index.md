# Plugins

Eleven plugins are published. A project applies `root` once and then one plugin per module. The
rest are applied for you or opted into.

| Plugin | Identifier | Apply it |
|---|---|---|
| [Root](root.md) | `io.github.thomaskioko.gradle.plugins.root` | On the root project, once |
| [App](app.md) | `io.github.thomaskioko.gradle.plugins.app` | On an Android application module |
| [Android](android.md) | `io.github.thomaskioko.gradle.plugins.android` | On an Android library module |
| [JVM](jvm.md) | `io.github.thomaskioko.gradle.plugins.jvm` | On a plain Kotlin library module |
| [Multiplatform](multiplatform.md) | `io.github.thomaskioko.gradle.plugins.multiplatform` | On a Kotlin Multiplatform module |
| [Base](base.md) | `io.github.thomaskioko.gradle.plugins.base` | Applied for you by the four above |
| [Spotless](spotless.md) | `io.github.thomaskioko.gradle.plugins.spotless` | Applied for you by Base |
| [Lint](lint.md) | `io.github.thomaskioko.gradle.plugins.lint` | On the root project, to add the custom rules |
| [Baseline profile](baseline-profile.md) | `io.github.thomaskioko.gradle.plugins.baseline.profile` | On a benchmark module |
| [Resource generator](resource-generator.md) | `io.github.thomaskioko.gradle.plugins.resource.generator` | On the module holding the strings |
| [Build config](buildconfig.md) | `io.github.thomaskioko.gradle.plugins.buildconfig` | On a module needing compile-time constants |

Pick exactly one of App, Android, JVM and Multiplatform per module. Each applies Base, which is
what creates the `scaffold {}` block.

## Where the options live

`scaffold {}` is one block with nested blocks that appear only when the matching plugin is
applied.

```kotlin
scaffold {
    // always here, see the Base page
    useMetro()

    // only on an Android or Multiplatform module, see the Android page
    android {
        useCompose()
    }

    // only on an Android application module, see the App page
    app {
        applicationId("com.example.myapp")
    }

    // only on a JVM module, see the JVM page
    jvm {
        useAndroidLint()
    }
}
```
