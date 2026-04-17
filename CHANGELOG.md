Change Log
==========

## 0.7.1 *(2026-04-17)*
- Introduce a KSP-based navigation codegen that eliminates per-destination DI boilerplate in Metro + Decompose KMP projects.

## 0.6.7 *(2026-04-10)*
- Remove SKIE DSL

## 0.6.6 *(2026-04-09)*
- Fix iOS test crashes for modules using moko resources.
- Copy moko resource bundles to iOS test binaries so localization tests run on iOS.
- Optimize `MokoResourceGeneratorTask`: remove dead incremental logic, reduce duplication, scope compile dependency to `commonMain`.
- Add golden file tests for moko resource code generation.

## 0.6.4 *(2026-04-01)*
- Add Metro Gradle plugin and configure contribution providers.
- Add support for Compose UI tests.
- Dependency updates.

## 0.6.3 *(2026-03-26)*
- Remove Tag publishing from beta builds

## 0.6.2 *(2026-03-25)*
- Add support for beta version bumps.
- Update `AppPlugin` to use `BUILD_NUMBER` and enable release optimizations.

## 0.6.1 *(2026-03-24)*
- Push release tags to origin automatically during release.

## 0.6.0 *(2026-03-24)*
- Add release automation tasks (`bumpVersion`, `release`).
- Dependency updates.

## 0.5.1 *(2026-02-26)*
- Enable new compiler features: explicit backing fields, reified types in catch clauses, and `@all:` annotation target.
- Fix dependency analysis plugin configuration for KMP projects.
- Replace deprecated `-Xjvm-default` compiler flag with `-jvm-default`.
- Dependency updates.


## 0.5.0 *(2026-02-12)*
- Migrate to gradle 9.0
- Delete `AndroidMultiplatformPlugin` plugin. We now only need `KotlinMultiplatformPlugin`
- Remove `org.jetbrains.kotlin.android`
- Delete deprecated androidTarget support.

## 0.4.3 *(2025-12-30)*
- Enable return-value-checker.
- Enable `explicitApi` by default and remove it from base extension.
- Update java-toolchain version to 25.
- Dependency updates.

## 0.4.2
- Rename `useSkies()` to `useSkie()`
- Fix typo in release key alias property (`rReleaseKeyAlias` → `releaseKeyAlias`)
- Make `MokoResourceGeneratorTask` package name configurable via `resourcePackage` property
- Improve configuration cache compatibility and task disabling performance
- Add validation for release signing properties and BuildConfig package name
- Remove duplicate `-Xconsistent-data-class-copy-visibility` compiler argument

## 0.4.1
- Dependency updates.

## 0.4.0
- Add "io.github.thomaskioko.gradle.plugins.buildconfig" plugin. Helps us read keys from `local.properties`

**Usage**
```kotlin

buildConfig {

    packageName.set("com.thomaskioko.tvmaniac.core.base")
    booleanField("IS_DEBUG", true)
    buildConfigField("TMDB_API_KEY")
    buildConfigField("CLIENT_ID")
    buildConfigField("CLIENT_SECRET")
}
```

## 0.3.2
- Added `useSkie()` to DSL
- Group  Kotlin, KSP & Skie in Renovate
- Enable Kotlin/Native binary size optimization (Experimental) - Reduces release binary size
- Simplify DisableTasks and fix library lint configuration


## 0.3.0
- CI/CD automation
- Enable tests in multiplatform by default
- Configure ktlint & spotless.
  - Fix formatting on files

## 0.2.1
- Add debug flag and disable tasks during development.
- Update dependencies.

## 0.1.0

- Initial stable release
- Maven Central publishing support

### Plugin Inventory
- `com.thomaskioko.gradle.app` - Android application modules
- `com.thomaskioko.gradle.android` - Android library modules
- `com.thomaskioko.gradle.android.multiplatform` - Android targets in KMP
- `com.thomaskioko.gradle.jvm` - JVM modules
- `com.thomaskioko.gradle.multiplatform` - Kotlin Multiplatform modules
- `com.thomaskioko.gradle.base` - Base configurations
- `com.thomaskioko.gradle.root` - Root project setup
- `com.thomaskioko.gradle.baseline.profile` - Android baseline profiles
- `com.thomaskioko.gradle.spotless` - Code formatting with Spotless
- `com.thomaskioko.resource.generator` - Moko resource generation

