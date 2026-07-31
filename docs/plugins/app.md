# App

`io.github.thomaskioko.gradle.plugins.app`

For an Android application module. Applies the Android application plugin and the Android plugin
from this suite, so everything on the [Android](android.md) page is available here too, and adds
an `app {}` block for the things only an application has.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.app")
}

scaffold {
    app {
        applicationId("com.example.myapp")
        minify()
    }
    android {
        useCompose()
    }
}
```

## Options

| Option | What it does |
|---|---|
| `applicationId(applicationId)` | Sets the application identifier |
| `applicationIdSuffix(buildType, suffix)` | Adds a suffix on the named build type, so two variants can be installed side by side |
| `minify(vararg files)` | Turns on R8 for the release build type and adds the named rules |
| `useFirebase()` | Connects Firebase when a `google-services.json` is present, and skips it when there is not, so a checkout without one still builds |

## Version and release tasks

An application module also gets `bumpVersion` and `release`, which read and rewrite a `version.txt`
at the root of the project holding `VERSION_NUMBER` and `BUILD_NUMBER`.

```bash
./gradlew bumpVersion -Ptype=minor
```

`type` takes `major`, `minor`, `patch` or `beta`.

Full detail in the
[API reference](../api/plugins/plugins/io.github.thomaskioko.gradle.plugins.extensions/-app-extension/index.html).
