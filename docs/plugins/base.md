# Base

`io.github.thomaskioko.gradle.plugins.base`

This plugin creates the `scaffold {}` block that every module configures itself through. You don't
apply it yourself. The App, Android, JVM and Multiplatform plugins apply it for you.

## Features

- Creates `scaffold {}`
- Applies Spotless with the project's formatting rules
- Applies dependency analysis to the module so it contributes to `buildHealth`
- Sets the Java toolchain and target from the version catalog
- Sets the shared Kotlin compiler options, including the unused return value check on production
  code but not on tests

## Options

These options are available in `scaffold {}` on any module.

| Option | What it does |
|---|---|
| `useMetro()` | Applies the Metro plugin and turns on contribution providers |
| `useKotlinInject()` | Applies the kotlin-inject and kotlin-inject-anvil compiler plugins |
| `useSerialization()` | Applies the kotlinx.serialization plugin and adds the runtime |
| `useCodegen()` | Applies KSP with the navigation code generator and its annotations |
| `useFeatureFlagCodegen()` | Applies KSP with the feature flag code generator and its annotations |
| `useDependencyGuard(vararg configurations)` | Holds the named resolvable configurations to a recorded baseline |
| `optIn(vararg classes)` | Adds compiler opt-in entries to every Kotlin compilation |
| `ignoreUnusedDependencies(vararg dependencyPaths)` | Excludes named project dependencies from the unused dependency check |
| `ignoreAll(vararg projectPaths)` | Silences every dependency analysis category for the named projects, or for this one when called empty |

## Targets

These options set the targets of a Kotlin Multiplatform module. See the
[Multiplatform](multiplatform.md) page.

| Option | What it does |
|---|---|
| `addJvmTarget()` | Adds the JVM target |
| `addIosTargets(includeX64 = false)` | Adds `iosArm64` and `iosSimulatorArm64`, and `iosX64` on request |
| `addIosTargetsWithXcFramework(frameworkName, includeX64 = false, configure = {})` | The same, bundled into one static XCFramework |
| `addAndroidTarget(...)` | Adds an Android library target, see the [Android](android.md) page |
| `configureNativeTargets(bundleId = null, configure = {})` | Applies shared compiler and linker options to every native target |
| `android { }` | Configures the Android options after `addAndroidTarget()` has registered them |

Full detail in the
[API reference](../api/plugins/plugins/io.github.thomaskioko.gradle.plugins.extensions/-base-extension/index.html).
