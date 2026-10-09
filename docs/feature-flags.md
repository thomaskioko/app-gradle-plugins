# Feature Flag Codegen

The feature flag codegen is a KSP processor that generates the Metro wiring for typed feature flags.
You write one anchor declaration annotated with `@FeatureFlag`. The processor emits two files for that
flag, in the anchor's package and source set: the `<BaseName>Qualifier` annotation and the
`<BaseName>Binding` Metro binding. You no longer write the qualifier or the `FlagBindings` code by
hand.

This tier is independent of the navigation codegen documented in
[get-started.md](navigation/get-started.md). A module that needs both calls both DSL functions. A
module that needs one applies only that one.

## Why it exists

Every typed feature flag needs three pieces of code. Without the codegen, you write all three by hand,
for every flag:

1. A `<BaseName>Qualifier` annotation, marked with Metro's `@Qualifier`. This is what consumers
   inject to get this one flag.
2. A `@Provides @SingleIn(AppScope::class) @<BaseName>Qualifier` function returning
   `FeatureFlag<Boolean>` by calling `factory.boolean(key, title, description, defaultValue,
   dateAdded)`.
3. A `@Provides @IntoSet` function that takes the qualified `FeatureFlag<Boolean>` and returns it.
   This puts the same instance into the `Set<FeatureFlag<Boolean>>` multibinding the debug screen
   iterates.

None of it needs thought. Everything comes from the flag name and the metadata on `@FeatureFlag`.
So the processor generates all three from one annotated anchor. The anchor is the only thing you
write.

## What you need

Your project must already use Metro. It also needs a `FeatureFlag<T>` interface and a
`FeatureFlagFactory` that builds them. Specifically:

- `com.thomaskioko.tvmaniac.featureflags.FeatureFlag<T>` interface, parameterised with `Boolean` in
  every generated binding.
- `com.thomaskioko.tvmaniac.featureflags.FeatureFlagFactory` with a `boolean(key, title,
  description, defaultValue, dateAdded)` method returning `FeatureFlag<Boolean>`.
- Metro graphs with `AppScope` that the generated `@ContributesTo(AppScope::class)` interface plugs
  into.
- `kotlinx.datetime.LocalDate` on the classpath. The generated `dateAdded = LocalDate(year, month,
  day)` literal resolves against it.

The type names are hardcoded. They are listed in
[architecture/consumer-contract.md](internals/consumer-contract.md#feature-flag-primitives).
The [Tv Maniac](https://github.com/c0de-wizard/tv-maniac) project is the reference consumer.

## Wire it up

Add the codegen dependency aliases to the consumer's `gradle/libs.versions.toml`:

```toml
codegen-featureflag-annotations = { module = "io.github.thomaskioko.gradle.plugins:codegen-featureflag-annotations", version.ref = "app-gradle-plugins" }
codegen-featureflag-processor = { module = "io.github.thomaskioko.gradle.plugins:codegen-featureflag-processor", version.ref = "app-gradle-plugins" }
```

Then enable the DSL in each module that declares feature flags:

```kotlin
scaffold {
    useMetro()
    useFeatureFlagCodegen()
}
```

`useFeatureFlagCodegen()` applies KSP (and Metro, if absent), adds the annotation jar to
`commonMainImplementation`, and registers the processor against **all targets** via
`addKspDependencyForAllTargets` (its target-name mapping rewrites `metadata` to
`kspCommonMainMetadata`). We register every target because that is what makes platform-scoped flags work (see
[Platform isolation](#platform-isolation)).

## Annotation reference

`@FeatureFlag` goes on a class-like anchor. We recommend a public `object`. Parameters:

| Parameter      | Type      | Description                                                           |
|----------------|-----------|-----------------------------------------------------------------------|
| `key`          | `String`  | Firebase Remote Config key. Drives lookups and debug-store overrides. |
| `title`        | `String`  | Human-readable name shown on the debug screen row.                    |
| `description`  | `String`  | One-line summary shown beneath the title.                             |
| `defaultValue` | `Boolean`  | Fallback returned until Firebase serves an explicit value.           |
| `dateAdded`    | `String`   | ISO `YYYY-MM-DD` date the flag entered the codebase.                 |
| `platform`     | `Platform` | Platform the flag is generated into. Defaults to `Platform.ALL`.     |

`dateAdded` is a `String` because Kotlin annotations can't take a `LocalDate`. The processor parses
it into a `kotlinx.datetime.LocalDate` at codegen time. The generated binding then contains a
`LocalDate(year, month, day)` constructor call.

`platform` is the `Platform` enum: `ALL` (default), `IOS`, or `JVM`. Leave it unset for a normal
flag. Set it only to scope a flag to one platform (see [Platform isolation](#platform-isolation)).
The generated output is the same on every platform. The field only decides which compilation
emits it.

The base name is the anchor's simple name, unchanged. Name the anchor `XxxFlag`, never
`XxxFlagQualifier`. The generator appends `Qualifier` and `Binding`, so a `Qualifier` suffix gives
you `XxxFlagQualifierQualifier`.

## Example

Input (the only hand-written file):

```kotlin
package com.thomaskioko.tvmaniac.featureflags.flags

import io.github.thomaskioko.codegen.annotations.FeatureFlag

@FeatureFlag(
    key = "enable_continue_watching_nitro",
    title = "Progress Endpoint",
    description = "Use Trakt's internal /sync/progress/up_next_nitro call instead of the documented multi-step progress fetch.",
    defaultValue = false,
    dateAdded = "2026-05-20",
)
public object ContinueWatchingNitroFlag
```

Generates `ContinueWatchingNitroFlagQualifier.kt`:

```kotlin
package com.thomaskioko.tvmaniac.featureflags.flags

import dev.zacsweers.metro.Qualifier

@Qualifier
public annotation class ContinueWatchingNitroFlagQualifier
```

…and `ContinueWatchingNitroFlagBinding.kt`:

```kotlin
@ContributesTo(AppScope::class)
public interface ContinueWatchingNitroFlagBinding {
  @Provides
  @SingleIn(AppScope::class)
  @ContinueWatchingNitroFlagQualifier
  public fun provideContinueWatchingNitroFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> = factory.boolean(
      key = "enable_continue_watching_nitro",
      title = "Progress Endpoint",
      description = "Use Trakt's internal /sync/progress/up_next_nitro call instead of the documented multi-step progress fetch.",
      defaultValue = false,
      dateAdded = LocalDate(2026, 5, 20),
  )

  @Provides
  @IntoSet
  public fun bindContinueWatchingNitroFlag(@ContinueWatchingNitroFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<Boolean> = flag
}
```

## Platform isolation

The `platform` field scopes a flag to one platform at compile time. It is not a runtime filter. The
anchor always stays in `commonMain`, and the field is the only control:

- `platform = Platform.ALL` (default) → generated once for every graph (Android and iOS).
- `platform = Platform.IOS` → generated only into the iOS targets. Absent from the Android binary.
- `platform = Platform.JVM` → generated only into the Android/JVM targets. Absent from iOS. `JVM`
  covers the Android target and any plain `jvm` target. KSP reports the two the same way, so there
  is no Android-only value.

```kotlin
@FeatureFlag(
    key = "enable_liquid_glass",
    title = "Liquid Glass",
    description = "Render the iOS debug screen with the Liquid Glass material.",
    defaultValue = false,
    dateAdded = "2026-06-18",
    platform = Platform.IOS,
)
public object EnableLiquidGlassFlag // declared in commonMain; reaches the iOS graph only
```

The DSL attaches the processor to the `commonMain` metadata run and to the KSP run of every target.
The processor reads the `platform` field together with `SymbolProcessorEnvironment.platforms`.
More than one platform means the metadata run. One native platform means an iOS run. One JVM
platform means an Android/JVM run. Each anchor is emitted exactly once: an `ALL` flag from the
metadata run, an `IOS` or `JVM` flag only from the matching target run. So a `commonMain` anchor is
never declared twice across target runs, and you don't have to move anything between source sets.

Once generated, a platform-scoped flag reaches the app the same way as any other flag, but only on
its platform:

- **Graph contribution.** The generated `<BaseName>Binding` carries `@ContributesTo(AppScope::class)`
  and exists only in that platform's compilation. So it contributes only to that platform's Metro
  `Set<FeatureFlag<Boolean>>` multibinding. Android and iOS are separate `AppScope` graphs, and the
  other platform's binary never contains the code. The compiler guarantees the flag is absent. No
  runtime filter is involved.
- **Debug screen.** The interactor and presenter in your shared code inject the whole
  `Set<FeatureFlag<Boolean>>`, not any single qualifier. So a platform flag shows up on that
  platform's debug screen without any change to shared code.
- **Boundary.** The generated `<BaseName>Qualifier` lives only in that platform's source set. So only
  that platform's code can inject the flag by qualifier. Common code sees it only anonymously,
  through the multibinding. That is enough to list and toggle it on the debug screen. Code that reads
  the flag by its qualifier must live in the same platform source set as the anchor.

## Validation

The processor reports a compile error on the offending symbol when any of these hold:

| Marker                        | Rule                                                                       |
|-------------------------------|----------------------------------------------------------------------------|
| `[FeatureFlag/InvalidTarget]` | The annotated symbol is an annotation class, or is not a class/object/interface. |
| `[FeatureFlag/EmptyKey]`      | `key` is blank.                                                            |
| `[FeatureFlag/EmptyTitle]`    | `title` is blank.                                                          |
| `[FeatureFlag/InvalidDate]`   | `dateAdded` does not parse as a valid ISO `YYYY-MM-DD` date.               |

Each message names the anchor, so the IDE error log tells you which flag failed.

## Out of scope

- Non-Boolean flag types (`enum`, `integer`, `string`). The processor emits only `factory.boolean(...)`.
  Other methods come when a consumer adds the first non-Boolean flag.
- Codegen driven by a spec file. We chose an annotation on an anchor on purpose.
- A single `GeneratedFlagBindings.kt` for each consumer. One file per flag keeps each file independent,
  so the processor tracks nothing across rounds.

## References

- [Consumer contract](internals/consumer-contract.md#feature-flag-primitives): the full list of
  hardcoded type names the generated code references.
- [Navigation codegen](navigation/get-started.md): the sibling codegen tier for Decompose-based navigation.
- KSP: <https://kotlinlang.org/docs/ksp-overview.html>
- KotlinPoet: <https://square.github.io/kotlinpoet/>
