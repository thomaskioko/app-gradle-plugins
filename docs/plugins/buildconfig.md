# Build config

`io.github.thomaskioko.gradle.plugins.buildconfig`

Apply this on a module that needs constants fixed at compile time, such as an API key. It works on
any module, including a Kotlin Multiplatform one, which the Android `BuildConfig` does not.

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

Note that this block is `buildConfig {}` at the top level, not part of `scaffold {}`.

## Options

| Option | What it does |
|---|---|
| `packageName` | The package the generated object is written into. Required |
| `buildConfigField(name)` | Reads the value from `local.properties` or an environment variable of the same name, so a secret stays out of version control |
| `stringField(name, value)` | A literal text constant |
| `booleanField(name, value)` | A literal true or false constant |
| `intField(name, value)` | A literal whole number constant |

The three literal helpers write into `stringFields`, `booleanFields` and `intFields`, which are
also readable and settable directly if you are generating constants in a loop rather than naming
them one at a time.

The generated file is added to `commonMain` and every Kotlin compilation waits for it, so nothing
has to be ordered by hand.

Full detail in the
[API reference](../api/plugins/plugins/io.github.thomaskioko.gradle.plugins.extensions/-build-config-extension/index.html).
