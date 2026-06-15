# Feature Flag Codegen

A KSP processor that eliminates the Metro binding pair every typed feature flag would otherwise
require by hand. The processor reads `@FeatureFlag`-decorated qualifier annotations and emits one
`<QualifierBaseName>Binding.kt` per qualifier into the qualifier's package, removing the
`FlagBindings` boilerplate consumers used to maintain.

This tier is independent of the navigation codegen documented in
[get-started.md](get-started.md). Modules that need both call both DSL functions; modules that need
only one apply only that one.

## Why it exists

Each typed feature flag needs three artifacts that the consumer would otherwise write by hand:

1. A `FooQualifier` annotation, marked with Metro's `@Qualifier`. Stays manual; it is the per-flag
   handle consumers inject.
2. A `@Provides @SingleIn(AppScope::class) @FooQualifier` function returning `FeatureFlag<Boolean>`
   by calling `factory.boolean(key, title, description, defaultValue, dateAdded)`. Mechanical:
   every flag has the same body shape, only the metadata changes.
3. A `@Provides @IntoSet` function that takes the qualified `FeatureFlag<Boolean>` and returns it,
   so the same instance enters the `Set<FeatureFlag<Boolean>>` multibinding the debug screen
   iterates. Pure delegation; every flag's version is identical except the qualifier annotation.

Items 2 and 3 derive entirely from the qualifier name and the metadata declared on `@FeatureFlag`,
so the processor emits them from one annotation. The qualifier itself stays manual because it is
the symbol consumers reference at the injection site.

## What you need

The consumer project must already use Metro and declare a `FeatureFlag<T>` interface plus a
`FeatureFlagFactory` that builds them. Specifically:

- `com.thomaskioko.tvmaniac.featureflags.FeatureFlag<T>` interface. The generic type the processor
  parameterises with `Boolean` in every generated binding.
- `com.thomaskioko.tvmaniac.featureflags.FeatureFlagFactory` with a `boolean(key, title,
  description, defaultValue, dateAdded)` method returning `FeatureFlag<Boolean>`. The generated
  `@Provides` function calls this.
- Metro graphs with `AppScope` that the generated `@ContributesTo(AppScope::class)` interface can
  plug into.
- `kotlinx.datetime.LocalDate` on the consumer's classpath; the generated `dateAdded =
  LocalDate(year, month, day)` literal resolves against it.

Type names are hardcoded in
[architecture/consumer-contract.md](architecture/consumer-contract.md#feature-flag-primitives).

The [Tv Maniac](https://github.com/c0de-wizard/tv-maniac) project is the reference consumer.

## Wire it up

Add the codegen dependency aliases to the consumer's `gradle/libs.versions.toml`:

```toml
codegen-featureflag-annotations = { module = "io.github.thomaskioko.gradle.plugins:codegen-featureflag-annotations", version.ref = "app-gradle-plugins" }
codegen-featureflag-processor = { module = "io.github.thomaskioko.gradle.plugins:codegen-featureflag-processor", version.ref = "app-gradle-plugins" }
```

Then enable the DSL in each module that declares feature flag qualifiers:

```kotlin
scaffold {
    useMetro()
    useFeatureFlagCodegen()
}
```

`useFeatureFlagCodegen()` applies KSP (and Metro, if absent), adds the annotation jar to
`commonMainImplementation`, and registers the processor against `kspCommonMain`. The DSL is
independent of `useCodegen()`; modules that consume both navigation and feature flag codegen call
both functions.

## Annotation reference

`@FeatureFlag` decorates a `@Qualifier`-annotated annotation class. Parameters:

| Parameter      | Type      | Description                                                           |
|----------------|-----------|-----------------------------------------------------------------------|
| `key`          | `String`  | Firebase Remote Config key. Drives lookups and debug-store overrides. |
| `title`        | `String`  | Human-readable name shown on the debug screen row.                    |
| `description`  | `String`  | One-line summary shown beneath the title.                             |
| `defaultValue` | `Boolean` | Fallback returned until Firebase serves an explicit value.            |
| `dateAdded`    | `String`  | ISO `YYYY-MM-DD` date the flag entered the codebase.                  |

`dateAdded` is a `String` because Kotlin annotations cannot accept `LocalDate`. The processor
parses to `kotlinx.datetime.LocalDate` at codegen time and emits a `LocalDate(year, month, day)`
constructor call in the generated code.

## Example

Input (hand-written):

```kotlin
package com.thomaskioko.tvmaniac.featureflags.flags

import dev.zacsweers.metro.Qualifier
import io.github.thomaskioko.codegen.annotations.FeatureFlag

@Qualifier
@FeatureFlag(
    key = "enable_continue_watching_nitro",
    title = "Progress Endpoint",
    description = "Use Trakt's internal /sync/progress/up_next_nitro call instead of the documented multi-step progress fetch.",
    defaultValue = false,
    dateAdded = "2026-05-20",
)
public annotation class ContinueWatchingNitroFlagQualifier
```

Output (generated into the same package, file `ContinueWatchingNitroFlagBinding.kt`):

```kotlin
package com.thomaskioko.tvmaniac.featureflags.flags

import com.thomaskioko.tvmaniac.featureflags.FeatureFlag
import com.thomaskioko.tvmaniac.featureflags.FeatureFlagFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.Boolean
import kotlinx.datetime.LocalDate

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
      dateAdded = LocalDate(2_026, 5, 20),
  )

  @Provides
  @IntoSet
  public fun bindContinueWatchingNitroFlag(@ContinueWatchingNitroFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<Boolean> = flag
}
```

The base name (`ContinueWatchingNitroFlag`) comes from the qualifier's simple name with a trailing
`Qualifier` suffix stripped. Qualifiers without the suffix keep their name as the base name.

## Validation

The processor reports a compile error pinned to the offending symbol whenever any of these hold:

| Marker                           | Rule                                                                              |
|----------------------------------|-----------------------------------------------------------------------------------|
| `[FeatureFlag/InvalidTarget]`    | The annotated symbol is not an `annotation class`.                                |
| `[FeatureFlag/MissingQualifier]` | The annotation class is not also annotated with `@dev.zacsweers.metro.Qualifier`. |
| `[FeatureFlag/EmptyKey]`         | `key` is blank.                                                                   |
| `[FeatureFlag/EmptyTitle]`       | `title` is blank.                                                                 |
| `[FeatureFlag/InvalidDate]`      | `dateAdded` does not parse as a valid ISO `YYYY-MM-DD` date.                      |

Each error message names the qualifier so the IDE error log identifies the failing flag.

## Out of scope

- Non-Boolean flag types (`enum`, `integer`, `string`). The processor emits `factory.boolean(...)`
  only. Additional methods land when the consumer adds the first non-Boolean flag.
- Spec-file-driven codegen. The annotation-on-qualifier approach is intentional; spec-file mode is
  a separate future option.
- Consolidated `GeneratedFlagBindings.kt` per consumer. Per-qualifier files are independent and
  need no cross-round bookkeeping.

## References

- [Consumer contract](architecture/consumer-contract.md#feature-flag-primitives): the full list of
  hardcoded type names the generated code references.
- [Navigation codegen](get-started.md): the sibling codegen tier for Decompose-based navigation.
- KSP: <https://kotlinlang.org/docs/ksp-overview.html>
- KotlinPoet: <https://square.github.io/kotlinpoet/>
