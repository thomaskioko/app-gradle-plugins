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
- Registers the test tasks `linuxTest`, `iosTest` and `ciTest`. The App, Android, JVM and
  Multiplatform plugins attach the module's tests to them. `./gradlew linuxTest` runs the task in
  every module that has it
- Registers `graphDump` and `graphUpdate`, described under [Module graph](#module-graph)

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
| `ignoreAll()` | Silences every dependency analysis category for this module |

## Module graph

`graphUpdate` draws every module this one depends on, directly or through another module, as a
Mermaid diagram in the module's `README.md`, between `<!--region graph-->` and
`<!--endregion-->`. It creates the file when the module has none. `graphDump` writes the same
diagram to `build/mermaid/graph.txt`. Commit the readme files, so a change to the dependency tree
shows up as a diff in review.

The diagram counts `api` and `implementation` dependencies, the `commonMain` ones included. It
leaves out modules whose path ends in `:testing`, which hold the fakes a module publishes for
tests. Every module the diagram reaches has to apply one of the suite's plugins.

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
