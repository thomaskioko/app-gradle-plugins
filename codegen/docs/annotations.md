# Annotation reference

The annotations live in the `codegen-annotations` artifact under the package `io.github.thomaskioko.codegen.annotations`. All have `SOURCE` retention.

Three annotations:

- `@NavDestination` targets presenter classes in the shared Kotlin Multiplatform layer. The processor generates a Metro `@GraphExtension` plus a navigation binding for
  each annotated presenter.
- `@ScreenUi` and `@SheetUi` target `@Composable` functions in Android `ui` modules. The processor generates the Metro binding that contributes the composable to the
  consumer's `Set<ScreenContent>` or `Set<SheetContent>` multibinding.

This reference walks through each annotation: what it marks, what the processor emits, the validation rules, and the common pitfalls. For the vocabulary used throughout
(graph extension, multibinding, binding, slot, scope), see the glossary in [architecture/index.md](architecture/index.md#glossary).


## `@NavDestination`

Marks a presenter class as a navigation destination. One annotation, three kinds.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class NavDestination(
    val route: KClass<*>,
    val parentScope: KClass<*>,
    val kind: DestinationKind,
)

public enum class DestinationKind {
    SCREEN,
    OVERLAY,
    TAB_ROOT,
}
```

### When to use `@NavDestination`

Annotate a presenter class with `@NavDestination` whenever the consumer's navigator needs to push it onto the back stack (`SCREEN`), present it as a modal overlay
(`OVERLAY`), or render it as a top-level tab anchor (`TAB_ROOT`). Without the annotation the consumer has to manually write the Metro graph extension, the
`NavDestination` factory, and the route binding for each presenter.

### Parameters

The `route` parameter points at the feature's route class.

- For `SCREEN` and `OVERLAY` it implements the consumer project's `NavRoute` interface. `OVERLAY` additionally implements the consumer's overlay marker interface (for
  example `OverlayRoute` in Tv Maniac), which is what the consumer's navigator inspects at runtime to decide between stack and overlay routing.
- For `TAB_ROOT` it implements the consumer's `NavRoot` interface and is typically a `data object`.

The `parentScope` parameter names the parent dependency injection scope whose factory provides a `ComponentContext`. Typically `ActivityScope::class`.

The `kind` parameter chooses one of three destination roles. See [DestinationKind](#destinationkind).

### Minimal example

```kotlin
@Inject
@NavDestination(
    route = ShowsRoute::class,
    parentScope = ActivityScope::class,
    kind = DestinationKind.SCREEN,
)
public class ShowsPresenter(
    componentContext: ComponentContext,
) : ComponentContext by componentContext
```

### Generated artifacts

For a presenter `com.example.feature.presenter.FooPresenter` annotated `@NavDestination(route = FooRoute::class, parentScope = ActivityScope::class, kind = SCREEN)`:

- `FooScreenGraph.kt` is a Metro `@GraphExtension(FooRoute::class)` interface scoped to the route. The route class itself is the scope marker, so no separate scope class
  is generated. The reasoning lives in [architecture/generators.md](architecture/generators.md#route-class-as-graph-scope).
- `FooNavDestinationBinding.kt` is a Metro `@ContributesTo(parentScope)` interface with a companion that contributes:
    - `@IntoSet NavDestination<*>`: the matching `NavDestination.Screen` (or `Overlay`, or `TabRoot`) instance.
    - `@IntoSet NavRouteBinding<*>` for `SCREEN` or `OVERLAY`, or `@IntoSet NavRootBinding<*>` for `TAB_ROOT`.

See [examples.md](examples.md) for concrete output for each kind.

### Behavior by kind

| Kind | Injection | Contributes |
|---|---|---|
| `SCREEN` | `@Inject` (no runtime parameters) or `@AssistedInject` (parameterized) | `NavDestination.Screen` plus `NavRouteBinding<*>` |
| `OVERLAY` | Same as `SCREEN` | `NavDestination.Overlay` plus `NavRouteBinding<*>` |
| `TAB_ROOT` | `@Inject` only (no `@AssistedInject`) | `NavDestination.TabRoot` plus `NavRootBinding<*>` |

For `SCREEN` and `OVERLAY` the processor auto-detects whether your presenter accepts a runtime parameter from the route. A plain `@Inject` constructor produces a graph
that exposes the presenter directly. An `@AssistedInject` constructor with a nested `@AssistedFactory` produces a graph that exposes the factory; the generated binding
casts the route, reads the property whose type matches the presenter's single `@Assisted` parameter, and calls `factory.create(param)`.

### Validation

The processor reports a compile error if any of the following hold:

- The annotated symbol is not a class.
- `kind` is not `SCREEN`, `OVERLAY`, or `TAB_ROOT`.
- The presenter is parameterized but does not have exactly one `@Assisted` constructor parameter.
- `kind` is `TAB_ROOT` and the presenter declares a nested `@AssistedFactory`. Tab roots must use plain `@Inject` because the route is a `data object` and carries no payload.

### Common pitfalls

- **Forgetting `@Inject` or `@AssistedInject`.** `@NavDestination` only tells the processor which graph and binding to generate. The presenter still needs Metro's
  injection annotation so Metro creates instances of it.
- **Routing a tab through a `data class`.** `TAB_ROOT` requires a `data object`. A `data class` route would imply a payload, but the tab multibinding is keyed by route
  type, not by an instance.
- **Mismatched route property type for parameterized presenters.** The `@Assisted` parameter type must match a property on the route class. The processor reads that
  property at navigation time and passes it through the assisted factory.


## `@ScreenUi`

Marks a `@Composable` function as the Android renderer for a screen presenter.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class ScreenUi(
    val presenter: KClass<*>,
    val parentScope: KClass<*>,
)
```

### When to use `@ScreenUi`

Pair `@ScreenUi` with `@NavDestination(kind = SCREEN)` on the matching presenter. The presenter lives in the shared Kotlin Multiplatform layer; the composable lives in an
Android only `ui` module.

The annotation exists because the navigation host cannot call the composable directly. At runtime the host holds an active `RootChild` whose concrete presenter type is
opaque to it, so it iterates a `Set<ScreenContent>` multibinding to find the entry whose predicate matches the active child and renders that entry's `content` lambda.
`@ScreenUi` generates the entry that pairs the `(it as? ScreenDestination<*>)?.presenter is FooPresenter` predicate with a content lambda that casts the child and invokes
the annotated composable.

### Parameters

The `presenter` parameter names the presenter type this screen renders. The processor uses it to build the `matches` predicate that tests whether the active `RootChild`
is a `ScreenDestination<*>` wrapping an instance of that type, and to cast the presenter before forwarding it to the composable.

The `parentScope` parameter names the dependency injection scope the generated binding is contributed to. Typically `ActivityScope::class`.

### Minimal example

```kotlin
@Composable
@ScreenUi(presenter = ShowsPresenter::class, parentScope = ActivityScope::class)
public fun ShowsScreen(
    presenter: ShowsPresenter,
    modifier: Modifier = Modifier,
) {
    // ... compose UI here
}
```

### Generated artifacts

For a composable `com.example.feature.ui.FooScreen` annotated `@ScreenUi(presenter = FooPresenter::class, parentScope = ActivityScope::class)`, the processor emits one
file into `com.example.feature.ui.di`:

- `FooScreenUiBinding.kt`: a `@BindingContainer @ContributesTo(ActivityScope::class) object` that `@Provides @IntoSet` a single `ScreenContent`. The `matches` lambda
  tests `(it as? ScreenDestination<*>)?.presenter is FooPresenter`. The `content` lambda casts the child and invokes `FooScreen(presenter = ..., modifier = modifier)`.

The `@BindingContainer object` structure is deliberate. The detailed reasoning lives in
[architecture/generators.md](architecture/generators.md#binding-container-object-for-ui-bindings-interface-companion-for-destination-bindings); the short version is that
an `interface + companion object` form would silently produce an empty multibinding inside an Android only `ui` module without the right Metro flag.

### Composable signature requirement

The annotated function must match the signature `@Composable fun <Name>(presenter: <PresenterType>, modifier: Modifier = Modifier)`. The processor does not enforce the
parameter names or the `Modifier` default at parse time, but the generated code calls the composable with `presenter = ..., modifier = modifier`. A function whose
parameters are out of order or named differently will fail to compile after generation, not during processing.

### Common pitfalls

- **Annotating a composable in a transitive `implementation` dependency.** The generated binding lives in the same module as the composable. If the consumer pulls in that
  module only as a transitive dependency, the binding never makes it onto the app's compile classpath. Add the module as a direct `implementation` dependency.
- **Returning a value from the composable.** `ScreenContent.content` is `(RootChild, Modifier) -> Unit`. The generated wrapper expects the composable to return `Unit`.


## `@SheetUi`

Marks a `@Composable` function as the Android renderer for a modal overlay presenter. Parallel to `@ScreenUi`, but contributes a `SheetContent` instead of a `ScreenContent`.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class SheetUi(
    val presenter: KClass<*>,
    val parentScope: KClass<*>,
)
```

### When to use `@SheetUi`

Pair `@SheetUi` with `@NavDestination(kind = OVERLAY)` on the matching presenter. The presenter lives in the shared Kotlin Multiplatform layer; the composable lives in an
Android only `ui` module.

The annotation exists for the same reason as `@ScreenUi`: the consumer's overlay slot cannot call the composable directly. The slot host iterates a `Set<SheetContent>`
multibinding to find the entry whose predicate matches the active overlay child, then renders that entry's `content` lambda. `@SheetUi` generates the entry that pairs the
`(it as? SheetDestination<*>)?.presenter is FooPresenter` predicate with a content lambda that casts the child and invokes the annotated composable. The set is separate
from `Set<ScreenContent>` because the overlay slot and the navigation stack are independent registries.

### Parameters

The parameters have the same meaning as on [`@ScreenUi`](#screenui).

### Minimal example

```kotlin
@Composable
@SheetUi(presenter = EpisodeSheetPresenter::class, parentScope = ActivityScope::class)
public fun EpisodeSheet(
    presenter: EpisodeSheetPresenter,
    modifier: Modifier = Modifier,
) {
    // ... compose your ModalBottomSheet here
}
```

### Generated artifacts

For `EpisodeSheet` annotated `@SheetUi(presenter = EpisodeSheetPresenter::class, parentScope = ActivityScope::class)`, the processor emits `EpisodeSheetUiBinding.kt`. The
file is structurally identical to a `@ScreenUi` binding. The two differences are the return type (`SheetContent` instead of `ScreenContent`) and the absence of `modifier`
forwarding in the `content` lambda.

### Composable signature requirement

The signature requirement matches `@ScreenUi`: `@Composable fun <Name>(presenter: <PresenterType>, modifier: Modifier = Modifier)`. Only `presenter` is forwarded by the
generated code. The `modifier` parameter exists so the composable can still be called from preview code.

The generated wrapper does not pass a modifier because `SheetContent.content` is typed as `(SheetChild) -> Unit`. A modal overlay decides its own layout (typically inside
a `ModalBottomSheet`) in the composable body, not at the call site.


## `DestinationKind`

`DestinationKind` is the enum passed to `@NavDestination(kind = ...)`. It picks the role the destination plays at runtime.

- `SCREEN`: a screen pushed onto the navigation stack. Tapping the system back button pops it off and returns to the previous destination.
- `OVERLAY`: a modal overlay (sheet, dialog, or menu) that appears on top of the current screen without affecting the back stack. Dismissing the overlay returns to the
  screen that was visible underneath.
- `TAB_ROOT`: the destination shown when the user selects a top level tab. Each tab anchors its own back stack and persists across tab switches.

The `SCREEN` and `OVERLAY` outputs are structurally identical; they differ only in the `NavDestination` subclass the binding contributes. The consumer's navigator
inspects that subclass at runtime to choose between stack and overlay routing.


## Required consumer primitives

The generated code references fully qualified names that the consumer project must provide. These are hardcoded in the processor's `util/External.kt`, grouped below by
the role each plays.

The destination types pull from `com.thomaskioko.tvmaniac.navigation`:

- `BaseRoute`: sealed parent of every routable target.
- `NavRoute`: supertype for routes that map to navigation stack screens or overlays.
- `NavRoot`: supertype for routes that map to top level tab anchors.
- `NavDestination`: sealed factory family with nested `Screen`, `Overlay`, `TabRoot` subclasses. Codegen emits one of these for each annotated presenter.
- `NavRouteBinding`: Metro multibinding entry for `NavRoute` polymorphic serialization.
- `NavRootBinding`: Metro multibinding entry for `NavRoot` polymorphic serialization.
- `RootChild`: Decompose child marker; the return type of every `NavDestination` factory lambda.
- `ScreenDestination`: generic root wrapper used by `Screen` and `Overlay` factories.
- `SheetChild`, `SheetDestination`: used by overlay rendering at the slot host. The consumer typically rewraps a `RootChild` from an `Overlay` factory into a
  `SheetDestination` at the slot host.

Tabs additionally pull `TabChild` from `com.thomaskioko.tvmaniac.home.nav`. `TabChild` is the generic tab wrapper used by `TabRoot` factory lambdas.

`com.thomaskioko.tvmaniac.core.base.ActivityScope` is the default parent scope referenced by the generated binding contributions.

The Android UI renderer types live under `com.thomaskioko.tvmaniac.navigation.ui`. `ScreenContent` carries a `matches` predicate and a `@Composable (RootChild, Modifier)
-> Unit` content lambda. `SheetContent` does the same for `SheetChild`. The bindings generated for `@ScreenUi` and `@SheetUi` also reference
`androidx.compose.ui.Modifier` because the screen variant forwards a modifier into the composable.

The processor is opinionated about these names. Consumers other than Tv Maniac would need to adjust `util/External.kt` to match their navigation primitives. See
[architecture/consumer-contract.md](architecture/consumer-contract.md) for why these are constants and what a fork would change.


## Migration from earlier versions

Earlier releases of this module shipped `@NavScreen`, `@TabScreen`, and `@NavSheet` as separate annotations. They have been replaced by the unified `@NavDestination(kind
= ...)` API:

- `@NavScreen` becomes `@NavDestination(kind = DestinationKind.SCREEN)`.
- `@TabScreen` becomes `@NavDestination(kind = DestinationKind.TAB_ROOT)`.
- `@NavSheet` becomes `@NavDestination(kind = DestinationKind.OVERLAY)`.

See the [CHANGELOG](../../CHANGELOG.md) for the version that introduced the change.
