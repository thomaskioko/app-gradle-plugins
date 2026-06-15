# Codegen

KSP based code generation for Kotlin Multiplatform consumers using
[Metro](https://zacsweers.github.io/metro/) for dependency injection. Two independent tiers:

- **Navigation codegen** targets [Decompose](https://arkivanov.github.io/Decompose/). One annotation
  (`@NavDestination`) covers stack screens, modal overlays, and bottom navigation tab roots. Three
  more (`@ScreenUi`, `@SheetUi`, `@TabUi`) cover the renderer bindings that join Android composables
  to the navigation host. `@ChildPresenter` covers parent-owned child presenters such as tab pager
  pages. Two more (`@AppRoot`, `@AppRootUi`) cover the application's root presenter and the host
  composable that wraps every other screen. The processor emits the Metro `@GraphExtension` graph,
  the `NavDestination` binding for the Decompose host (including the `NavRoot` singleton
  contribution for tabs), the UI renderer binding for the Android side, the activity-scope binding
  container for the root presenter, and the provider interface plus extension that lets the
  activity render the root with one call.
- **Feature flag codegen** targets typed feature flags backed by a `FeatureFlagFactory`. One
  annotation (`@FeatureFlag`) decorates a `@Qualifier`-annotated annotation class. The processor
  emits one `<QualifierBaseName>Binding.kt` per qualifier containing the `@Provides @SingleIn
  @<Qualifier>` factory call plus the `@Provides @IntoSet` rebind into the
  `Set<FeatureFlag<Boolean>>` multibinding.

Both tiers do not work with Jetpack Navigation, Voyager, Appyx, Dagger/Hilt, or any other library;
the generated output references Decompose's `ChildStack`/`ChildSlot`/`ComponentContext` primitives
and Metro's `@ContributesTo`/`@Provides`/`@IntoSet` directly.

## What you need

The consumer project must already use Metro. Each tier adds its own requirements:

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

The Tv Maniac project is the reference implementation for both tiers. The full runtime contract,
including the exact consumer types the generated code references, lives in
[architecture/consumer-contract.md](docs/architecture/consumer-contract.md).

## Docs

- [Get started (navigation)](docs/get-started.md): what the navigation codegen does and how to wire
  it into a consumer project.
- [Feature flag codegen](docs/featureflag.md): the `@FeatureFlag` annotation, generated output, DSL
  call, and validation rules.
- [Annotation reference](docs/annotations.md): every navigation annotation parameter and the
  validation rules the processor enforces.
- [Examples](docs/examples.md): input and generated output for every navigation variant.
- [Architecture](docs/architecture/index.md): how the processors are built, for contributors.

## References

- Decompose: <https://arkivanov.github.io/Decompose/>
- Metro: <https://zacsweers.github.io/metro/>
- KSP: <https://kotlinlang.org/docs/ksp-overview.html>
- kctfork: <https://github.com/ZacSweers/kotlin-compile-testing>
- KotlinPoet: <https://square.github.io/kotlinpoet/>
