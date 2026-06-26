# Annotation reference

The annotations live in the `codegen-annotations` artifact under the package `io.github.thomaskioko.codegen.annotations`. All have `SOURCE` retention.

Seven annotations:

- `@NavDestination` targets presenter classes in the shared Kotlin Multiplatform layer. The processor generates a Metro `@GraphExtension` plus a navigation binding for
  each annotated presenter.
- `@ScreenUi`, `@SheetUi`, and `@TabUi` target `@Composable` functions in Android `ui` modules. The processor generates the Metro binding that contributes the composable
  to the consumer's `Set<ScreenContent>` or `Set<SheetContent>` multibinding. `@TabUi` is the variant for tab pager pages, where the active child is a `TabChild` rather
  than a `ScreenDestination`.
- `@ChildPresenter` targets a presenter class owned by another presenter rather than navigated to through a route, such as a tab pager's pages. The processor generates a
  `<Presenter>ChildGraph` graph extension plus a factory contributing to the parent host's scope.
- `@AppRoot` targets the application's `@AssistedInject` root presenter implementation. The processor generates the activity-scope binding container that wires the
  nested `@AssistedFactory` to the presenter's bound interface.
- `@AppRootUi` targets the host `@Composable` that wraps every other screen. The processor generates an `AppRootProvider` interface plus a Composable extension so the
  activity invokes the host with one call (`graph.AppRootContent()`) instead of forwarding each dependency by hand.

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
    - `@IntoSet NavRoot` for `TAB_ROOT` only: the route singleton itself, contributed to `Set<NavRoot>` so consumers do not have to keep a parallel binding next to each tab.

See [examples.md](examples.md) for concrete output for each kind.

### Behavior by kind

| Kind | Injection | Contributes |
|---|---|---|
| `SCREEN` | `@Inject` (no runtime parameters) or `@AssistedInject` (parameterized) | `NavDestination.Screen` plus `NavRouteBinding<*>` |
| `OVERLAY` | Same as `SCREEN` | `NavDestination.Overlay` plus `NavRouteBinding<*>` |
| `TAB_ROOT` | `@Inject` only (no `@AssistedInject`) | `NavDestination.TabRoot` plus `NavRootBinding<*>` plus the `NavRoot` singleton into `Set<NavRoot>` |

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


## `@TabUi`

Marks a `@Composable` function as the Android renderer for one tab pager page. Parallel to `@ScreenUi`, but the predicate matches a `TabChild<*>` instead of a
`ScreenDestination<*>`.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class TabUi(
    val presenter: KClass<*>,
    val parentScope: KClass<*>,
)
```

### When to use `@TabUi`

Pair `@TabUi` with a tab pager presenter that exposes children as `TabChild<*>` instances rather than as `ScreenDestination<*>` instances. Tv Maniac's home shell uses this
shape: the home pager hosts a fixed set of tab pages (Discover, Library, Progress, Profile), each backed by a `TabChild`-wrapped tab presenter, and renders them through a
`Set<ScreenContent>` multibinding.

The annotation exists because `@ScreenUi`'s generated predicate (`(it as? ScreenDestination<*>)?.presenter is FooPresenter`) does not match a `TabChild<*>`. `@TabUi`
generates the same `ScreenContent` multibinding entry but casts the child to `TabChild<*>` instead. From the host's point of view the registry is one set; the predicates
inside the set know how to recognise their own child type.

### Parameters

The parameters have the same meaning as on [`@ScreenUi`](#screenui).

### Minimal example

```kotlin
@Composable
@TabUi(presenter = DiscoverShowsPresenter::class, parentScope = ActivityScope::class)
public fun DiscoverScreen(
    presenter: DiscoverShowsPresenter,
    modifier: Modifier = Modifier,
) {
    // ... compose UI here
}
```

### Generated artifacts

For `DiscoverScreen` annotated `@TabUi(presenter = DiscoverShowsPresenter::class, parentScope = ActivityScope::class)`, the processor emits one file
`DiscoverScreenUiBinding.kt`. The file is structurally identical to a `@ScreenUi` binding. The two differences are the cast inside the `matches` predicate
(`(it as? TabChild<*>)?.presenter is DiscoverShowsPresenter`) and the cast inside the `content` lambda (`(child as TabChild<*>).presenter as DiscoverShowsPresenter`).

### Composable signature requirement

The signature requirement matches `@ScreenUi`: `@Composable fun <Name>(presenter: <PresenterType>, modifier: Modifier = Modifier)`. The generator forwards both `presenter`
and `modifier` to the composable.


## `@ChildPresenter`

Marks a presenter class as a child presenter created and owned by a parent host presenter, such as a tab pager's pages. The processor generates a
`<Presenter>ChildGraph` graph extension plus a factory contributing to the parent host's scope.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class ChildPresenter(
    val scope: KClass<*>,
    val parentScope: KClass<*>,
)
```

### When to use `@ChildPresenter`

Use `@ChildPresenter` when a presenter is constructed by another presenter rather than navigated to through a route. Tab pager pages, expanding-card sub-screens, and
similar sub-components fit this pattern. Routed destinations should use `@NavDestination` instead.

Without the annotation a parent presenter that exposes child presenters has to hand-write a `@GraphExtension` interface listing each child as a property plus a nested
`@ContributesTo(parentScope) @GraphExtension.Factory` that the parent presenter consumes. With the annotation the processor emits one graph extension per annotated child,
and the parent presenter consumes one factory per child.

### Parameters

The `scope` parameter is the graph scope, used as the marker on the generated `@GraphExtension`. Multiple `@ChildPresenter` classes may share a scope; each gets its own
graph extension.

The `parentScope` parameter names the parent dependency injection scope hosting the generated factory. Typically the route class of the parent host (for example
`ProgressRoot::class`).

### Minimal example

```kotlin
@Inject
@ChildPresenter(
    scope = ProgressChildScope::class,
    parentScope = ProgressRoot::class,
)
public class UpNextPresenter(
    componentContext: ComponentContext,
    // ... deps
) : ComponentContext by componentContext
```

### Generated artifacts

For a presenter `com.example.feature.upnext.UpNextPresenter` annotated `@ChildPresenter(scope = ProgressChildScope::class, parentScope = ProgressRoot::class)`, the
processor emits one file into `com.example.feature.upnext.di`:

- `UpNextChildGraph.kt`: a `@GraphExtension(ProgressChildScope::class) interface UpNextChildGraph` exposing `val upNextPresenter: UpNextPresenter` and a nested
  `@ContributesTo(ProgressRoot::class) @GraphExtension.Factory interface Factory` whose single function `createUpNextGraph(@Provides componentContext: ComponentContext)`
  returns the graph.

The factory function name is unique per presenter (`create<BaseName>Graph`) so two child graphs that contribute to the same parent scope do not collide. The base name is
the presenter's simple name with the `Presenter` suffix stripped.

### Parent presenter wiring

The parent presenter takes one factory parameter for each child:

```kotlin
@Inject
public class ProgressPresenter(
    componentContext: ComponentContext,
    upNextGraphFactory: UpNextChildGraph.Factory,
    calendarGraphFactory: CalendarChildGraph.Factory,
) : ComponentContext by componentContext {
    public val upNextPresenter: UpNextPresenter =
        upNextGraphFactory.createUpNextGraph(childContext(key = "UpNext")).upNextPresenter
    public val calendarPresenter: CalendarPresenter =
        calendarGraphFactory.createCalendarGraph(childContext(key = "Calendar")).calendarPresenter
}
```

### Embeddable / reusable components

`parentScope` decides which hosts can embed the child. Pointing it at a parent route (the example
above) ties the child to that one host. Pointing it at a shared ancestor scope makes the child
embeddable by every host below that scope, which is what lets a component live in its own module and
be reused.

Metro graph extensions resolve bindings from any ancestor scope. The scope chain runs
`AppScope -> ActivityScope -> {tab roots} -> {child scopes}`, each level a `@GraphExtension` whose
factory `@ContributesTo` the level above. A factory contributed to `ActivityScope` is therefore
visible to every tab root, stack screen, and child below it.

To make a component reusable, give it its own scope and set `parentScope` to `ActivityScope`:

```kotlin
@Inject
@ChildPresenter(
    scope = FeaturedShowsComponentScope::class,
    parentScope = ActivityScope::class,
)
public class FeaturedShowsPresenter(
    componentContext: ComponentContext,
    // ... deps available at ActivityScope or AppScope
) : ComponentContext by componentContext
```

Any host below `ActivityScope` then embeds it the same way a parent embeds a pager child:

```kotlin
@Inject
public class DiscoverPresenter(
    componentContext: ComponentContext,
    featuredGraphFactory: FeaturedShowsChildGraph.Factory,
) : ComponentContext by componentContext {
    public val featuredPresenter: FeaturedShowsPresenter =
        featuredGraphFactory.createFeaturedShowsGraph(childContext(key = "Featured")).featuredShowsPresenter
}
```

Nesting one embeddable component inside another works for free: the outer component's graph descends
from `ActivityScope`, so it can inject the inner component's `ActivityScope`-contributed factory.

The one constraint: an embeddable component may inject only bindings reachable at `ActivityScope` or
`AppScope`. Depending on a binding scoped to a specific tab root would compile in that root but fail
in any other host, which surfaces as an ordinary Metro missing-binding error at the embedding site,
not a silent failure. The generator is scope-agnostic, so the embeddable shape needs no annotation or
generator change: it is purely the `parentScope` you choose.

### Validation

The processor reports a compile error if the annotated symbol is not a class.

### Common pitfalls

- **Forgetting `@Inject`.** `@ChildPresenter` only tells the processor which graph to generate. The presenter still needs Metro's injection annotation so Metro creates
  instances of it.
- **Reusing a factory function name.** Two child graphs that contribute to the same `parentScope` cannot expose factory functions with the same name. The generator
  derives the function name from the presenter's simple name, so two child presenters in the same parent scope must have distinct simple names.


## `@AppRoot`

Marks an `@AssistedInject` presenter implementation as the application's root host. The processor emits the `@BindingContainer` that wires the nested `@AssistedFactory`
to the bound interface at the parent scope.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class AppRoot(
    val parentScope: KClass<*>,
)
```

### When to use `@AppRoot`

Use `@AppRoot` on the activity-scope root presenter implementation. The root presenter is the top-level component the activity instantiates once and hands to the host
composable; it is not a destination in the navigation stack. Without the annotation the consumer has to hand-write a `@BindingContainer @ContributesTo(parentScope)
object` that takes the assisted factory and a `ComponentContext` and returns the bound interface. With the annotation the processor emits that file.

`@AppRoot` differs from `@NavDestination` in two ways. The root has no route, so the annotation does not take a `route` parameter. The root is bound to its public
interface at the parent scope (typically `ActivityScope`) rather than exposed through a `@GraphExtension`, so the generated artifact is a binding container, not a graph
plus a destination binding.

### Parameters

The `parentScope` parameter names the dependency injection scope hosting the generated binding. Typically `ActivityScope::class` in the consumer project.

### Minimal example

```kotlin
@AppRoot(parentScope = ActivityScope::class)
@AssistedInject
public class DefaultRootPresenter(
    @Assisted componentContext: ComponentContext,
    private val navigator: Navigator,
    // ... more deps
) : RootPresenter, ComponentContext by componentContext {

    @AssistedFactory
    public fun interface Factory {
        public fun create(componentContext: ComponentContext): DefaultRootPresenter
    }
}
```

### Generated artifacts

For an implementation `com.example.app.presenter.DefaultRootPresenter` annotated `@AppRoot(parentScope = ActivityScope::class)` and implementing `RootPresenter`, the
processor emits one file into `com.example.app.presenter.di`:

- `RootPresenterBindingContainer.kt`: a `@BindingContainer @ContributesTo(parentScope) object` whose `@Provides @SingleIn(parentScope)` function takes a
  `ComponentContext` and the nested `Factory`, and returns the bound interface (`RootPresenter` in this case). The function body invokes the factory's single function
  with the supplied `ComponentContext`.

The output replaces the hand-written binding container the consumer would otherwise have to keep in sync with the implementation's factory function name and the bound
interface name.

### Bound interface inference

The processor reads the implementation's supertypes in declaration order and picks the first non-marker interface as the bound type. Decompose's `ComponentContext`
(commonly used as a delegate via `ComponentContext by componentContext`) and Kotlin's implicit `Any` are filtered out. Implementations that extend more than one
non-marker interface are rejected at compile time.

### Validation

The processor reports a compile error if any of the following hold:

- The annotated symbol is not a class.
- The class does not carry `@AssistedInject`.
- The class does not declare a nested `@AssistedFactory` interface.
- The nested factory does not declare exactly one function.
- The class extends zero or more than one non-marker interface.

### Common pitfalls

- **Forgetting the nested factory.** `@AppRoot` requires `@AssistedInject` plus a nested `@AssistedFactory`. The factory's single function must take a `ComponentContext`
  and return the implementation type, mirroring how the consumer would have invoked the assisted factory by hand.
- **Multiple non-marker supertypes.** The bound type is inferred from the supertype list. If the implementation extends two interfaces (for example a presenter contract
  and an extra interface), the processor cannot pick one and reports a compile error. Pick the bound type explicitly by removing the second interface, or merge the two
  contracts into one.


## `@AppRootUi`

Marks the host `@Composable` function as the application's root UI. The processor generates a provider interface declaring one property for each non-modifier parameter
on the composable plus a Composable extension that invokes the composable using the receiver's properties.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class AppRootUi(
    val presenter: KClass<*>,
    val parentScope: KClass<*>,
)
```

### When to use `@AppRootUi`

Pair `@AppRootUi` with `@AppRoot` on the matching presenter implementation. The composable is the activity's top-level Compose entry point: it receives the root
presenter plus any multibinding sets the host needs (`Set<ScreenContent>`, `Set<SheetContent>`, and so on), and renders every screen the navigation system pushes
underneath it.

The composable is not part of the `Set<ScreenContent>` multibinding the navigation system iterates. It is the host that provides that set to its descendants. `@ScreenUi`
does not apply for that reason. `@AppRootUi` exists so the codegen can emit a provider interface keyed off the composable's parameter list, which lets the activity
graph extend that interface and the activity render the host with one call.

### Parameters

The `presenter` parameter names the root presenter type. The processor reads the composable's parameters in declaration order, skips a final `modifier: Modifier`
parameter, and asserts that the first remaining parameter type equals `presenter`. Mismatch is a compile error.

The `parentScope` parameter names the dependency injection scope hosting the generated artifacts. Typically `ActivityScope::class`.

### Minimal example

```kotlin
@AppRootUi(presenter = RootPresenter::class, parentScope = ActivityScope::class)
@Composable
public fun RootScreen(
    rootPresenter: RootPresenter,
    screenContents: Set<ScreenContent>,
    sheetContents: Set<SheetContent>,
    modifier: Modifier = Modifier,
) {
    // ... compose UI here, including the navigation host
}
```

### Generated artifacts

For a composable `com.example.app.ui.RootScreen` annotated `@AppRootUi(presenter = RootPresenter::class, parentScope = ActivityScope::class)`, the processor emits one
file into `com.example.app.ui.di`:

- `RootScreenAppRootUiBinding.kt` contains two declarations:
  - An `AppRootProvider` interface declaring one `val` for each non-modifier parameter on the annotated composable.
  - A `@Composable AppRootProvider.AppRootContent(modifier: Modifier)` extension that invokes the composable using the receiver's properties.

The consumer makes its activity-scope `@DependencyGraph` extend `AppRootProvider`, then calls `graph.AppRootContent()` from the activity. The activity call site
collapses from one argument per dependency to one extension call.

### Composable signature requirement

The annotated function must:

- Be `@Composable`.
- Declare at least one non-modifier parameter.
- Declare its first non-modifier parameter as the `presenter` type.

The processor reads the parameter list in order and skips any parameter named `modifier` whose type is `androidx.compose.ui.Modifier`. Every other parameter becomes a
property on the generated `AppRootProvider` interface.

### Validation

The processor reports a compile error if any of the following hold:

- The annotated symbol is not a function.
- The function lives in the default (empty) package.
- The function has no non-modifier parameters.
- The first non-modifier parameter type does not equal `presenter`.
- More than one `@AppRootUi` is declared in the same compilation round.

### Common pitfalls

- **Activity graph does not extend `AppRootProvider`.** The generated extension is on `AppRootProvider`, not on the consumer's graph type directly. The consumer must
  declare `: AppRootProvider` on its `@DependencyGraph` interface so the extension resolves at the call site.
- **Adding a parameter without updating the graph.** Each non-modifier parameter becomes a `val` on the generated interface. If the activity graph already exposes a
  property of the same name and type, the new field is satisfied automatically. Otherwise the consumer needs to add the property, typically as a Metro multibinding or
  injection point.


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

`@AppRoot` references one additional Metro primitive, `dev.zacsweers.metro.SingleIn`, used to scope the generated provider to the parent scope. `@AppRootUi` emits a
top-level `@Composable` extension and so references `androidx.compose.runtime.Composable` and `androidx.compose.ui.Modifier` directly. These three names are also
hardcoded constants in `util/External.kt` and have matching test stubs.

The processor is opinionated about these names. Consumers other than Tv Maniac would need to adjust `util/External.kt` to match their navigation primitives. See
[architecture/consumer-contract.md](architecture/consumer-contract.md) for why these are constants and what a fork would change.


## Migration from earlier versions

Earlier releases of this module shipped `@NavScreen`, `@TabScreen`, and `@NavSheet` as separate annotations. They have been replaced by the unified `@NavDestination(kind
= ...)` API:

- `@NavScreen` becomes `@NavDestination(kind = DestinationKind.SCREEN)`.
- `@TabScreen` becomes `@NavDestination(kind = DestinationKind.TAB_ROOT)`.
- `@NavSheet` becomes `@NavDestination(kind = DestinationKind.OVERLAY)`.

See the [CHANGELOG](../../CHANGELOG.md) for the version that introduced the change.
