# Build config

`io.github.thomaskioko.gradle.plugins.buildconfig`

This plugin generates constants that are fixed at compile time, such as an API key. It works on
any module, including a Kotlin Multiplatform one. The Android `BuildConfig` doesn't.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.buildconfig")
}

buildConfig {
    packageName.set("com.example.myapp.base")
    buildConfigField("TMDB_API_KEY")
    booleanField("IS_INTERNAL_BUILD", true)
}
```

This block is `buildConfig {}` at the top level. It isn't part of `scaffold {}`.

## Options

| Option | What it does |
|---|---|
| `packageName` | The package the generated object is written into. Required |
| `buildConfigField(name)` | Reads the value from `local.properties` or an environment variable of the same name, so a secret stays out of version control |
| `stringField(name, value)` | A literal text constant |
| `booleanField(name, value)` | A literal true or false constant |
| `intField(name, value)` | A literal whole number constant |

The three literal helpers write into `stringFields`, `booleanFields` and `intFields`. You can also
read and set those directly, which helps if you generate constants in a loop instead of naming
them one at a time.

The generated file is added to `commonMain`, and every Kotlin compilation waits for it. You don't
have to order anything by hand.

Full detail in the
[API reference](../api/plugins/plugins/io.github.thomaskioko.gradle.plugins.extensions/-build-config-extension/index.html).
