# JVM

`io.github.thomaskioko.gradle.plugins.jvm`

For a plain Kotlin library with no Android or native targets. Applies the Kotlin JVM plugin and
Base, and adds a `jvm {}` block.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.jvm")
}

scaffold {
    jvm {
        useAndroidLint()
    }
}
```

## Options

| Option | What it does |
|---|---|
| `useAndroidLint()` | Applies the standalone Android Lint plugin, so a JVM module is checked by the same rules as an Android one |

Everything in `scaffold {}` itself is on the [Base](base.md) page.

Full detail in the
[API reference](../api/plugins/plugins/io.github.thomaskioko.gradle.plugins.extensions/-jvm-extension/index.html).
