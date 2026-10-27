# Root

`io.github.thomaskioko.gradle.plugins.root`

This plugin configures the root project. Apply it there before anything else.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.root")
}
```

It refuses to be applied to a module. A misplaced `id(...)` fails instead of half working.

## Features

- Sets the Java vendor used for the Gradle daemon toolchain
- Applies dependency analysis at the root, so `buildHealth` covers the whole build. It also sets
  the severity of each issue category
- Configures Gradle Doctor, if the project applies it
