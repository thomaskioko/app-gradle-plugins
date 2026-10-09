# Baseline profile

`io.github.thomaskioko.gradle.plugins.baseline.profile`

This plugin configures the benchmark module that produces a baseline profile for your
application. Apply it on that module.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.baseline.profile")
}
```

The identifier is `baseline.profile` with a dot, not a hyphen.

## What it does

- Applies the Android test plugin and Base
- Points the test module at the application module and sets the instrumentation runner
- Adds a `benchmark {}` block inside `scaffold {}`, with the same options as the
  [Android](android.md) page
- On builds other than debug, applies the AndroidX baseline profile producer. It runs on the
  registered managed device, not a connected one. It also passes the target application
  identifier through, so the profile is produced for the right package

In debug-only mode we skip the profile setup entirely, so local builds stay fast.

## The other half

The application module that consumes the profile calls `useBaselineProfile()` in its
`android {}` block and names this module. See the [Android](android.md) page.
