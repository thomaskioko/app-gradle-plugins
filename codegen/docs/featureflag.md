# Feature Flag Codegen

A KSP processor that eliminates the per-flag boilerplate every typed feature flag would otherwise
require by hand. The processor reads a single `@FeatureFlag`-decorated anchor declaration and emits
two files per flag into the anchor's package and source set: the `<BaseName>Qualifier` annotation
and the `<BaseName>Binding` Metro binding. Consumers no longer hand-write the qualifier or the
`FlagBindings` boilerplate.

This tier is independent of the navigation codegen documented in
[get-started.md](get-started.md). Modules that need both call both DSL functions; modules that need
only one apply only that one.

## Why it exists

Each typed feature flag needs three artifacts that the consumer would otherwise write by hand, all
of which derive entirely from the flag name plus the metadata on `@FeatureFlag`:

1. A `<BaseName>Qualifier` annotation, marked with Metro's `@Qualifier` — the per-flag handle
   consumers inject.
2. A `@Provides @SingleIn(AppScope::class) @<BaseName>Qualifier` function returning
   `FeatureFlag<Boolean>` by calling `factory.boolean(key, title, description, defaultValue,
   dateAdded)`.
3. A `@Provides @IntoSet` function that takes the qualified `FeatureFlag<Boolean>` and returns it,
   so the same instance enters the `Set<FeatureFlag<Boolean>>` multibinding the debug screen
   iterates.

All three are mechanical, so the processor emits them from one annotated anchor. The anchor is the
only thing the developer writes.

## What you need

The consumer project must already use Metro and declare a `FeatureFlag<T>` interface plus a
`FeatureFlagFactory` that builds them. Specifically:

- `com.thomaskioko.tvmaniac.featureflags.FeatureFlag<T>` interface, parameterised with `Boolean` in
  every generated binding.
- `com.thomaskioko.tvmaniac.featureflags.FeatureFlagFactory` with a `boolean(key, title,
  description, defaultValue, dateAdded)` method returning `FeatureFlag<Boolean>`.
- Metro graphs with `AppScope` that the generated `@ContributesTo(AppScope::class)` interface plugs
  into.
- `kotlinx.datetime.LocalDate` on the classpath; the generated `dateAdded = LocalDate(year, month,
  day)` literal resolves against it.

Type names are hardcoded in
[architecture/consumer-contract.md](architecture/consumer-contract.md#feature-flag-primitives).
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
`kspCommonMainMetadata`). Registering every target is what makes platform-scoped flags work — see
[Platform isolation](#platform-isolation).

## Annotation reference

`@FeatureFlag` decorates a class-like anchor; a public `object` is recommended. Parameters:

| Parameter      | Type      | Description                                                           |
|----------------|-----------|-----------------------------------------------------------------------|
| `key`          | `String`  | Firebase Remote Config key. Drives lookups and debug-store overrides. |
| `title`        | `String`  | Human-readable name shown on the debug screen row.                    |
| `description`  | `String`  | One-line summary shown beneath the title.                             |
| `defaultValue` | `Boolean`  | Fallback returned until Firebase serves an explicit value.           |
| `dateAdded`    | `String`   | ISO `YYYY-MM-DD` date the flag entered the codebase.                 |
| `platform`     | `Platform` | Platform the flag is generated into. Defaults to `Platform.ALL`.     |

`dateAdded` is a `String` because Kotlin annotations cannot accept `LocalDate`. The processor parses
it to `kotlinx.datetime.LocalDate` at codegen time and emits a `LocalDate(year, month, day)`
constructor call in the generated binding.

`platform` is the `Platform` enum — `ALL` (default), `IOS`, or `JVM`. Leave it unset for a normal
flag; set it only to scope a flag to one platform (see [Platform isolation](#platform-isolation)).
The generated output is identical regardless of platform — the field only decides which compilation
emits it.

The base name comes from the anchor's simple name verbatim. Name the anchor `XxxFlag`, never
`XxxFlagQualifier` — the generator appends `Qualifier`/`Binding`, so a `Qualifier` suffix would
produce a doubled `XxxFlagQualifierQualifier`.

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

The `platform` field scopes a flag to one platform at compile time — not a runtime filter. The
anchor always stays in `commonMain`; the field is the only control:

- `platform = Platform.ALL` (default) → generated once for every graph (Android and iOS).
- `platform = Platform.IOS` → generated only into the iOS targets; absent from the Android binary.
- `platform = Platform.JVM` → generated only into the Android/JVM targets; absent from iOS. `JVM`
  covers the Android target and any plain `jvm` target — KSP reports the two identically, so there
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

The DSL attaches the processor to the `commonMain` metadata run and every per-target run. The
processor reads the `platform` field together with `SymbolProcessorEnvironment.platforms` (more than
one platform marks the metadata run; one native platform an iOS run; one JVM platform an
Android/JVM run) and emits each anchor exactly once: an `ALL` flag from the metadata run, an
`IOS`/`JVM` flag only from its matching per-target run. A `commonMain` anchor is therefore never
redeclared across per-target runs, and no source-set juggling is required.

Once generated, a platform-scoped flag reaches the app through the same path as any other, but only
on its platform:

- **Graph contribution.** The generated `<BaseName>Binding` carries `@ContributesTo(AppScope::class)`
  and exists only in that platform's compilation, so it contributes to only that platform's Metro
  `Set<FeatureFlag<Boolean>>` multibinding. Android and iOS are separate `AppScope` graphs, and the
  other platform's binary never contains the code — absence is a compile-time guarantee, not a
  runtime filter.
- **Debug screen.** The shared consumer-side interactor and presenter inject the whole
  `Set<FeatureFlag<Boolean>>`, not any individual qualifier, so a platform flag surfaces on that
  platform's debug screen with no shared-code change.
- **Boundary.** The generated `<BaseName>Qualifier` lives only in that platform's source set, so only
  that platform's source can inject the flag by qualifier. Shared/common code sees it only
  anonymously through the multibinding — enough to list and toggle it on the debug screen, but code
  that reads the flag by its qualifier must live in the same platform source set as the anchor.

## Validation

The processor reports a compile error pinned to the offending symbol whenever any of these hold:

| Marker                        | Rule                                                                       |
|-------------------------------|----------------------------------------------------------------------------|
| `[FeatureFlag/InvalidTarget]` | The annotated symbol is an annotation class, or is not a class/object/interface. |
| `[FeatureFlag/EmptyKey]`      | `key` is blank.                                                            |
| `[FeatureFlag/EmptyTitle]`    | `title` is blank.                                                          |
| `[FeatureFlag/InvalidDate]`   | `dateAdded` does not parse as a valid ISO `YYYY-MM-DD` date.               |

Each message names the anchor so the IDE error log identifies the failing flag.

## Out of scope

- Non-Boolean flag types (`enum`, `integer`, `string`). The processor emits `factory.boolean(...)`
  only; other methods land when the consumer adds the first non-Boolean flag.
- Spec-file-driven codegen. The annotation-on-anchor approach is intentional.
- Consolidated `GeneratedFlagBindings.kt` per consumer. Per-flag files are independent and need no
  cross-round bookkeeping.

## References

- [Consumer contract](architecture/consumer-contract.md#feature-flag-primitives): the full list of
  hardcoded type names the generated code references.
- [Navigation codegen](get-started.md): the sibling codegen tier for Decompose-based navigation.
- KSP: <https://kotlinlang.org/docs/ksp-overview.html>
- KotlinPoet: <https://square.github.io/kotlinpoet/>
