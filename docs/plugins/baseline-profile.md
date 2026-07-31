# Baseline profile

`io.github.thomaskioko.gradle.plugins.baseline.profile`

Apply this on the benchmark module that produces a baseline profile for your application.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.baseline.profile")
}
```

The identifier is `baseline.profile` with a dot, not a hyphen.

## What it does

- Applies the Android test plugin and Base
- Points the test module at the application module and sets the instrumentation runner
- Adds a `benchmark {}` block inside `scaffold {}` carrying the same options as the
  [Android](android.md) page
- On non-debug builds, applies the AndroidX baseline profile producer, runs it on the registered
  managed device rather than a connected one, and passes the target application identifier through
  so the profile is produced for the right package

In debug-only mode the profile setup is skipped entirely, so local builds stay fast.

## The other half

The application module consuming the profile calls `useBaselineProfile()` in its `android {}`
block, naming this module. See the [Android](android.md) page.
