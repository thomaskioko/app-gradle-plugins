Change Log
==========

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

