# Annotation Reference

The annotations live in the `codegen-annotations` artifact under the package `io.github.thomaskioko.codegen.annotations`. All three have `SOURCE` retention and target classes only. This reference walks through each annotation, what it marks, what the processor emits, and the invariants the processor checks.


## `@NavScreen`

Marks a presenter class as a root level navigation destination. A destination is something that sits on the back stack. It is reached with `navigator.navigateTo(route)` and popped with `navigator.navigateBack()`.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class NavScreen(
    val route: KClass<*>,
    val parentScope: KClass<*>,
)
```

The `route` parameter points at the feature's route class. This is the `@Serializable` type that implements the consumer project's `NavRoute` interface. The processor verifies the reference at generation time.

The `parentScope` parameter names the parent DI scope whose factory provides a `ComponentContext`. In Tv Maniac this is almost always `ActivityScope::class`.

### Processor behavior

The processor picks between two output shapes based on how the presenter is injected.

1. If the class is annotated with `@Inject` (plain constructor injection), the processor emits the simple shape. The generated graph exposes the presenter instance directly.
2. If the class is annotated with `@AssistedInject` and has a nested `@AssistedFactory`, the processor emits the parameterized shape. The generated graph exposes the factory type. The generated `NavDestination.createChild` casts the incoming route, extracts the single property whose type matches the presenter's single `@Assisted` constructor parameter, and calls `factory.create(param)`.

### Generated files

For a presenter `com.example.feature.presenter.FooPresenter`, the processor emits two files into `com.example.feature.presenter.di`.

`FooScreenGraph.kt` is a Metro `@GraphExtension(FooRoute::class)` interface. The route class itself is used as the graph's scope marker, so no separate scope class is generated.

`FooNavDestinationBinding.kt` is a Metro `@ContributesTo(parentScope)` interface with a companion that contributes `@IntoSet NavDestination` and `@IntoSet NavRouteBinding<*>`.

Using the route as the scope keeps it visible from every consumer (including `commonMain` and sibling modules) without the cross module visibility problems that a KSP generated scope class would introduce. KSP output lands in per target source sets, so a separately generated `FooScreenScope` would be invisible from `commonMain`.

See [examples.md](examples.md) for concrete output shapes.


## `@TabScreen`

Marks a presenter class as a tab inside a host screen, for example a bottom navigation tab hanging off a home screen. Tabs don't participate in the root back stack; they contribute into a host's tab multibinding instead.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class TabScreen(
    val config: KClass<*>,
    val parentScope: KClass<*>,
)
```

The `config` parameter names the config subtype (usually a data object) this tab matches. In Tv Maniac this is a subtype of `HomeConfig`. The config class itself doubles as the generated tab graph's scope marker.

The `parentScope` parameter is typically the host route class (e.g. `HomeRoute::class`) rather than `ActivityScope::class`, because tabs contribute into the host graph whose scope is the host route.

### Processor behavior

`@TabScreen` is incompatible with `@AssistedInject`. Tabs must use plain `@Inject`. The processor reports a compile error if a tab presenter declares a nested `@AssistedFactory`.

### Generated files

For `FooPresenter` matching `HomeConfig.Foo`, the processor emits two files.

`FooTabGraph.kt` is a `@GraphExtension(HomeConfig.Foo::class)` exposing the presenter. The config class itself is the scope marker, so no separate tab scope class is generated.

`FooTabDestinationBinding.kt` contributes `@IntoSet TabDestination`. No `NavRouteBinding` is generated because the host feature owns its own config serialization.


## `@NavSheet`

Marks a presenter class as a modal sheet destination. This is a bottom sheet, dialog, or other overlay presented on top of the current destination through Decompose's `childSlot`.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class NavSheet(
    val route: KClass<*>,
    val parentScope: KClass<*>,
)
```

The `route` parameter names the sheet's config class. The config must implement the consumer project's `SheetConfig` marker. Like the other annotations, this class also serves as the generated graph's scope marker.

The `parentScope` parameter names the DI scope the generated binding is contributed to. This is typically `ActivityScope::class`.

### Processor behavior

`@NavSheet` requires `@AssistedInject`. The presenter must declare a nested `@AssistedFactory`. The processor maps each `@Assisted` constructor parameter to a property of the route class by type, in order, and emits an error if the shapes do not match.

### Generated files

For `FooSheetPresenter` annotated with `route = FooSheetConfig::class`, the processor emits two files.

`FooSheetScreenGraph.kt` is a `@GraphExtension(FooSheetConfig::class)` exposing the presenter's nested `@AssistedFactory`.

`FooSheetDestinationBinding.kt` is a `@ContributesTo(parentScope)` interface whose companion contributes both `@IntoSet SheetChildFactory` and `@IntoSet SheetConfigBinding<*>`.

The sheet binding mirrors the `@NavScreen` pair. The child factory matches on the generic `SheetConfig` marker, casts to the feature's config type, and dispatches through the graph's assisted factory. The config binding feeds the polymorphic `KSerializer<SheetConfig>` used by Decompose's `childSlot`, so the sheet slot survives process death without a central registry.


## Required consumer primitives

The generated code references fully qualified names that the consumer project must provide. These are hardcoded in the processor's `util/External.kt`, grouped below by the role they play.

The root destination shape pulls from `com.thomaskioko.tvmaniac.navigation`: `NavRoute` is the route supertype, `NavRouteBinding` is the Metro multibinding entry for routes, `NavDestination` is the root destination factory interface, `RootChild` is the Decompose child marker, and `ScreenDestination` is the generic root wrapper.

The sheet shape reuses that package for `SheetConfig` (the sheet config supertype), `SheetChild` (Decompose sheet child marker), `SheetDestination` (generic sheet wrapper), `SheetChildFactory` (sheet destination factory interface), and `SheetConfigBinding` (Metro multibinding entry for sheet configs).

Tabs live under `com.thomaskioko.tvmaniac.home.nav`: `TabDestination` is the tab factory interface and `TabChild` is the generic tab wrapper. Finally, `com.thomaskioko.tvmaniac.core.base.ActivityScope` is the default parent scope referenced by the generated binding contributions.

The processor is opinionated about these names. Consumers other than Tv Maniac would need to adjust `util/External.kt` to match their navigation primitives.
