# Codegen

This directory contains KSP processors that generate code for Kotlin Multiplatform projects using
[Metro](https://zacsweers.github.io/metro/) for dependency injection. There are two independent tiers:

- **Navigation codegen** targets [Decompose](https://arkivanov.github.io/Decompose/). One annotation
  (`@NavDestination`) covers screens on the navigation stack, modal overlays, and bottom navigation
  tab roots. Three more (`@ScreenUi`, `@SheetUi`, `@TabUi`) connect Android composables to the
  navigation host. `@ChildPresenter` covers child presenters that a parent presenter owns, such as
  tab pager pages. `@AppRoot` and `@AppRootUi` cover the application's root presenter and the host
  composable that wraps every other screen. From these, the processor generates:
  - the Metro `@GraphExtension` graph,
  - the `NavDestination` binding for the Decompose host (plus the `NavRoot` singleton contribution
    for tabs),
  - the UI renderer binding on the Android side,
  - the binding container that puts the root presenter in the activity scope,
  - the provider interface and extension that let the activity render the root with one call.
- **Feature flag codegen** targets typed feature flags backed by a `FeatureFlagFactory`. One
  annotation (`@FeatureFlag`) decorates a `@Qualifier`-annotated annotation class. For each
  qualifier, the processor emits one `<QualifierBaseName>Binding.kt`. It holds the `@Provides
  @SingleIn @<Qualifier>` factory call and the `@Provides @IntoSet` rebind into the
  `Set<FeatureFlag<Boolean>>` multibinding.

Neither tier works with Jetpack Navigation, Voyager, Appyx, Dagger/Hilt, or any other library. The
generated code references Decompose's `ChildStack`/`ChildSlot`/`ComponentContext` primitives and
Metro's `@ContributesTo`/`@Provides`/`@IntoSet` directly.

## What you need

Your project must already use Metro. Each tier adds its own requirements:

**Navigation tier:**

- Decompose components hosted as presenters, with each destination identified by a `NavRoute` or
  `NavRoot` class.
- Metro dependency graphs that the generated `@GraphExtension` can plug into.
- A Decompose-based navigation host (typically a `ChildStack` for screens plus a `ChildSlot` for
  overlays) that consumes the `Set<NavDestination<*>>` multibinding the codegen contributes to.

**Feature flag tier:**

- A `FeatureFlag<T>` interface and a `FeatureFlagFactory` with a `boolean(key, title, description,
  defaultValue, dateAdded)` method.
- Metro graphs scoped at `AppScope` that the generated `@ContributesTo(AppScope::class)` interface
  can plug into.
- `kotlinx-datetime` on every module that declares `@FeatureFlag` qualifiers.

Tv Maniac is the reference implementation for both tiers. The full runtime contract, including the
exact consumer types the generated code references, is in
[the consumer contract](https://thomaskioko.github.io/app-gradle-plugins/internals/consumer-contract/).

## Docs

- [Get started (navigation)](https://thomaskioko.github.io/app-gradle-plugins/navigation/get-started/): what the navigation codegen does and how to wire
  it into a consumer project.
- [Feature flag codegen](https://thomaskioko.github.io/app-gradle-plugins/feature-flags/): the `@FeatureFlag` annotation, generated output, DSL
  call, and validation rules.
- [Annotation reference](https://thomaskioko.github.io/app-gradle-plugins/navigation/annotations/): every navigation annotation parameter and the
  validation rules the processor enforces.
- [Examples](https://thomaskioko.github.io/app-gradle-plugins/navigation/examples/): input and generated output for every navigation variant.
- [Architecture](https://thomaskioko.github.io/app-gradle-plugins/internals/): how the processors are built, for contributors.

## References

- Decompose: <https://arkivanov.github.io/Decompose/>
- Metro: <https://zacsweers.github.io/metro/>
- KSP: <https://kotlinlang.org/docs/ksp-overview.html>
- kctfork: <https://github.com/ZacSweers/kotlin-compile-testing>
- KotlinPoet: <https://square.github.io/kotlinpoet/>
