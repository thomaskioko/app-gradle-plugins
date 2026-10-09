# Annotation reference

This page is the reference for the navigation codegen annotations. They live in the `codegen-annotations` artifact under the package
`io.github.thomaskioko.codegen.annotations`. All have `SOURCE` retention.

There are seven:

- `@NavDestination` targets presenter classes in the shared Kotlin Multiplatform layer. The processor generates a Metro `@GraphExtension` plus a navigation binding for
  each annotated presenter.
- `@ScreenUi`, `@SheetUi`, and `@TabUi` target `@Composable` functions in Android `ui` modules. The processor generates the Metro binding that adds the composable to
  the consumer's `Set<ScreenContent>` or `Set<SheetContent>` multibinding. `@TabUi` is for tab pager pages, where the active child is a `TabChild` rather than a
  `ScreenDestination`.
- `@ChildPresenter` targets a presenter class that another presenter creates, instead of one you navigate to through a route. Tab pager pages are the typical case.
  The processor generates a `<Presenter>ChildGraph` graph extension plus a factory contributed to the parent host's scope.
- `@AppRoot` targets the application's `@AssistedInject` root presenter implementation. The processor generates the activity-scope binding container that wires the
  nested `@AssistedFactory` to the presenter's bound interface.
- `@AppRootUi` targets the host `@Composable` that wraps every other screen. The processor generates an `AppRootProvider` interface plus a Composable extension. The
  activity then calls the host once (`graph.AppRootContent()`) instead of forwarding each dependency by hand.

For each annotation, this page covers what it marks, what the processor emits, the validation rules, and the common pitfalls. The terms used throughout (graph
extension, multibinding, binding, slot, scope) are defined in the glossary in [architecture/index.md](../internals/index.md#glossary).


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

Use `@NavDestination` on a presenter when the navigator needs to push it onto the back stack (`SCREEN`), show it as a modal overlay (`OVERLAY`), or render it as a
top level tab (`TAB_ROOT`). Without it, you write the Metro graph extension, the `NavDestination` factory, and the route binding by hand for every presenter.

### Parameters

The `route` parameter points at the feature's route class.

- For `SCREEN` and `OVERLAY` it implements the consumer project's `NavRoute` interface. `OVERLAY` routes also implement the consumer's overlay marker interface (for
  example `OverlayRoute` in Tv Maniac). The navigator checks that marker at runtime to decide whether to push the route onto the stack or show it as an overlay.
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

- `FooScreenGraph.kt` is a Metro `@GraphExtension(FooRoute::class)` interface scoped to the route. The route class itself is the scope marker, so we don't generate a
  separate scope class. The reasoning is in [architecture/generators.md](../internals/generators.md#route-class-as-graph-scope).
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

For `SCREEN` and `OVERLAY`, the processor detects whether your presenter takes a runtime parameter from the route. A plain `@Inject` constructor gives a graph that
exposes the presenter directly. An `@AssistedInject` constructor with a nested `@AssistedFactory` gives a graph that exposes the factory. The generated binding then
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
- **Routing a tab through a `data class`.** `TAB_ROOT` requires a `data object`. A `data class` route implies a payload, but the tab multibinding is keyed by route
  type, not by instance.
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

Pair `@ScreenUi` with `@NavDestination(kind = SCREEN)` on the matching presenter. The presenter lives in the shared Kotlin Multiplatform layer. The composable lives
in an Android only `ui` module.

The annotation exists because the navigation host can't call the composable directly. At runtime the host holds an active `RootChild`, and it doesn't know the
concrete presenter type inside. So it walks a `Set<ScreenContent>` multibinding, finds the entry whose predicate matches the active child, and renders that entry's
`content` lambda. `@ScreenUi` generates that entry. It pairs the `(it as? ScreenDestination<*>)?.presenter is FooPresenter` predicate with a content lambda that casts
the child and calls the annotated composable.

### Parameters

The `presenter` parameter names the presenter type this screen renders. The processor uses it in two places. It builds the `matches` predicate, which checks that the
active `RootChild` is a `ScreenDestination<*>` wrapping that type. It also casts the presenter before passing it to the composable.

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

We use a `@BindingContainer object` on purpose. The full reasoning is in
[architecture/generators.md](../internals/generators.md#bindingcontainer-object-for-ui-bindings-interface-companion-for-destination-bindings). The short version: in an
Android only `ui` module, an `interface + companion object` form silently produces an empty multibinding unless the right Metro flag is set.

### Composable signature requirement

The annotated function must match the signature `@Composable fun <Name>(presenter: <PresenterType>, modifier: Modifier = Modifier)`. The processor doesn't check the
parameter names or the `Modifier` default while parsing. But the generated code calls the composable with `presenter = ..., modifier = modifier`. So a function with
parameters in a different order or with different names fails to compile after generation, not during processing.

### Common pitfalls

- **Annotating a composable in a transitive `implementation` dependency.** The generated binding lives in the same module as the composable. If the app only gets that
  module transitively, the binding never reaches the app's compile classpath. Add the module as a direct `implementation` dependency.
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

Pair `@SheetUi` with `@NavDestination(kind = OVERLAY)` on the matching presenter. The presenter lives in the shared Kotlin Multiplatform layer. The composable lives
in an Android only `ui` module.

The annotation exists for the same reason as `@ScreenUi`. The consumer's overlay slot can't call the composable directly. The slot host walks a `Set<SheetContent>`
multibinding, finds the entry whose predicate matches the active overlay child, and renders that entry's `content` lambda. `@SheetUi` generates that entry. It pairs
the `(it as? SheetDestination<*>)?.presenter is FooPresenter` predicate with a content lambda that casts the child and calls the annotated composable. This set is
separate from `Set<ScreenContent>` because the overlay slot and the navigation stack are independent registries.

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

For `EpisodeSheet` annotated `@SheetUi(presenter = EpisodeSheetPresenter::class, parentScope = ActivityScope::class)`, the processor emits `EpisodeSheetUiBinding.kt`. It
has the same structure as a `@ScreenUi` binding, with two differences: the return type is `SheetContent` instead of `ScreenContent`, and the `content` lambda doesn't
forward a `modifier`.

### Composable signature requirement

The signature requirement matches `@ScreenUi`: `@Composable fun <Name>(presenter: <PresenterType>, modifier: Modifier = Modifier)`. The generated code forwards only
`presenter`. The `modifier` parameter is there so you can still call the composable from preview code.

The generated wrapper doesn't pass a modifier because `SheetContent.content` is typed as `(SheetChild) -> Unit`. A modal overlay decides its own layout (usually inside
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

Pair `@TabUi` with a tab pager presenter that exposes its children as `TabChild<*>` instances, not `ScreenDestination<*>` instances. Tv Maniac's home shell works this
way. The home pager hosts a fixed set of tab pages (Discover, Library, Progress, Profile). Each is backed by a tab presenter wrapped in a `TabChild`, and the pager
renders them through a `Set<ScreenContent>` multibinding.

The annotation exists because the predicate `@ScreenUi` generates (`(it as? ScreenDestination<*>)?.presenter is FooPresenter`) doesn't match a `TabChild<*>`. `@TabUi`
generates the same `ScreenContent` multibinding entry, but casts the child to `TabChild<*>`. The host sees one set. Each predicate in it knows how to recognise its own
child type.

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

For `DiscoverScreen` annotated `@TabUi(presenter = DiscoverShowsPresenter::class, parentScope = ActivityScope::class)`, the processor emits one file,
`DiscoverScreenUiBinding.kt`. It has the same structure as a `@ScreenUi` binding, with two differences: the cast inside the `matches` predicate
(`(it as? TabChild<*>)?.presenter is DiscoverShowsPresenter`) and the cast inside the `content` lambda (`(child as TabChild<*>).presenter as DiscoverShowsPresenter`).

### Composable signature requirement

The signature requirement matches `@ScreenUi`: `@Composable fun <Name>(presenter: <PresenterType>, modifier: Modifier = Modifier)`. The generator forwards both `presenter`
and `modifier` to the composable.


## `@ChildPresenter`

Marks a presenter class as a child presenter that a parent host presenter creates and owns, such as a tab pager's pages. The processor generates a
`<Presenter>ChildGraph` graph extension plus a factory contributed to the parent host's scope.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class ChildPresenter(
    val scope: KClass<*>,
    val parentScope: KClass<*>,
)
```

### When to use `@ChildPresenter`

Use `@ChildPresenter` when another presenter constructs this one, instead of the navigator reaching it through a route. Tab pager pages, sub-screens inside an
expanding card, and similar sub-components fit. Routed destinations use `@NavDestination` instead.

Without the annotation, a parent presenter with child presenters needs a hand-written `@GraphExtension` interface that lists each child as a property. It also needs a
nested `@ContributesTo(parentScope) @GraphExtension.Factory` for the parent to consume. With the annotation, the processor emits one graph extension for each
annotated child, and the parent takes one factory per child.

### Parameters

The `scope` parameter is the graph scope, used as the marker on the generated `@GraphExtension`. Several `@ChildPresenter` classes may share a scope. Each still gets
its own graph extension.

The `parentScope` parameter names the parent dependency injection scope that hosts the generated factory. Usually this is the route class of the parent host (for
example `ProgressRoot::class`).

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

Each presenter gets its own factory function name (`create<BaseName>Graph`), so two child graphs contributed to the same parent scope don't collide. The base name is
the presenter's simple name without the `Presenter` suffix.

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

`parentScope` decides which hosts can embed the child. Point it at a parent route (as in the example
above) and the child belongs to that one host. Point it at a shared ancestor scope and every host
below that scope can embed it. That is how a component can live in its own module and be reused.

Metro graph extensions resolve bindings from any ancestor scope. The scope chain runs
`AppScope -> ActivityScope -> {tab roots} -> {child scopes}`. Each level is a `@GraphExtension` whose
factory `@ContributesTo` the level above. So a factory contributed to `ActivityScope` is visible to
every tab root, stack screen, and child below it.

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

Nesting one embeddable component inside another just works. The outer component's graph descends
from `ActivityScope`, so it can inject the inner component's `ActivityScope`-contributed factory.

There is one constraint. An embeddable component may inject only bindings reachable at `ActivityScope`
or `AppScope`. If it depends on a binding scoped to one tab root, it compiles in that root and fails
in every other host. You get an ordinary Metro missing-binding error at the embedding site, not a
silent failure. The generator doesn't care about scopes, so you need no extra annotation or generator
change. The `parentScope` you choose is the whole difference.

### Validation

The processor reports a compile error if the annotated symbol is not a class.

### Common pitfalls

- **Forgetting `@Inject`.** `@ChildPresenter` only tells the processor which graph to generate. The presenter still needs Metro's injection annotation so Metro creates
  instances of it.
- **Reusing a factory function name.** Two child graphs contributed to the same `parentScope` can't expose factory functions with the same name. The generator derives
  the name from the presenter's simple name. So two child presenters in the same parent scope need different simple names.


## `@AppRoot`

Marks an `@AssistedInject` presenter implementation as the application's root host. The processor emits the `@BindingContainer` that connects the nested
`@AssistedFactory` to the bound interface at the parent scope.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class AppRoot(
    val parentScope: KClass<*>,
)
```

### When to use `@AppRoot`

Use `@AppRoot` on the root presenter implementation in the activity scope. The activity creates the root presenter once and hands it to the host composable. It is
not a destination on the navigation stack. Without the annotation, you write a `@BindingContainer @ContributesTo(parentScope) object` by hand. It takes the assisted
factory and a `ComponentContext` and returns the bound interface. With the annotation, the processor emits that file.

`@AppRoot` differs from `@NavDestination` in two ways. First, the root has no route, so the annotation has no `route` parameter. Second, the root is bound to its
public interface at the parent scope (usually `ActivityScope`) instead of exposed through a `@GraphExtension`. So the generated file is a binding container, not a
graph plus a destination binding.

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

This replaces the binding container you would otherwise write by hand and keep in sync with the factory function name and the bound interface name.

### Bound interface inference

The processor reads the implementation's supertypes in declaration order and picks the first non-marker interface as the bound type. It skips Decompose's
`ComponentContext` (usually used as a delegate via `ComponentContext by componentContext`) and Kotlin's implicit `Any`. An implementation that extends more than one
non-marker interface is rejected at compile time.

### Validation

The processor reports a compile error if any of the following hold:

- The annotated symbol is not a class.
- The class does not carry `@AssistedInject`.
- The class does not declare a nested `@AssistedFactory` interface.
- The nested factory does not declare exactly one function.
- The class extends zero or more than one non-marker interface.

### Common pitfalls

- **Forgetting the nested factory.** `@AppRoot` requires `@AssistedInject` plus a nested `@AssistedFactory`. The factory's single function must take a
  `ComponentContext` and return the implementation type, the same way you would call the assisted factory by hand.
- **Multiple non-marker supertypes.** The bound type is inferred from the supertype list. If the implementation extends two interfaces (for example a presenter contract
  and an extra interface), the processor can't pick one and reports a compile error. Remove the second interface so the bound type is clear, or merge the two
  contracts into one.


## `@AppRootUi`

Marks the host `@Composable` function as the application's root UI. The processor generates a provider interface with one property for each non-modifier parameter of
the composable. It also generates a Composable extension that calls the composable with the receiver's properties.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class AppRootUi(
    val presenter: KClass<*>,
    val parentScope: KClass<*>,
)
```

### When to use `@AppRootUi`

Pair `@AppRootUi` with `@AppRoot` on the matching presenter implementation. The composable is the activity's top level Compose entry point. It receives the root
presenter plus any multibinding sets the host needs (`Set<ScreenContent>`, `Set<SheetContent>`, and so on). It renders every screen the navigation system pushes
beneath it.

The composable is not part of the `Set<ScreenContent>` multibinding the navigation system walks. It is the host that provides that set to its descendants, so
`@ScreenUi` doesn't apply. `@AppRootUi` lets the codegen emit a provider interface built from the composable's parameter list. The activity graph extends that
interface, and the activity renders the host with one call.

### Parameters

The `presenter` parameter names the root presenter type. The processor reads the composable's parameters in declaration order and skips a final `modifier: Modifier`
parameter. The first remaining parameter type must equal `presenter`. A mismatch is a compile error.

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

Make your activity-scope `@DependencyGraph` extend `AppRootProvider`, then call `graph.AppRootContent()` from the activity. The call site shrinks from one argument per
dependency to one extension call.

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

- **Activity graph does not extend `AppRootProvider`.** The generated extension is on `AppRootProvider`, not on your graph type. Declare `: AppRootProvider` on your
  `@DependencyGraph` interface so the extension resolves at the call site.
- **Adding a parameter without updating the graph.** Each non-modifier parameter becomes a `val` on the generated interface. If the activity graph already exposes a
  property with the same name and type, the new field is covered. Otherwise you add the property, usually as a Metro multibinding or injection point.


## `DestinationKind`

`DestinationKind` is the enum passed to `@NavDestination(kind = ...)`. It picks the role the destination plays at runtime.

- `SCREEN`: a screen pushed onto the navigation stack. Tapping the system back button pops it off and returns to the previous destination.
- `OVERLAY`: a modal overlay (sheet, dialog, or menu) that appears on top of the current screen without affecting the back stack. Dismissing the overlay returns to the
  screen that was visible underneath.
- `TAB_ROOT`: the destination shown when the user selects a top level tab. Each tab anchors its own back stack and persists across tab switches.

The `SCREEN` and `OVERLAY` outputs have the same structure. They differ only in the `NavDestination` subclass the binding contributes. The navigator checks that
subclass at runtime to decide between pushing onto the stack and showing an overlay.


## Required consumer primitives

The generated code references fully qualified names that your project must provide. They are hardcoded in the processor's `util/External.kt`. Below they are grouped by
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
- `SheetChild`, `SheetDestination`: used when the slot host renders overlays. The consumer usually rewraps the `RootChild` from an `Overlay` factory into a
  `SheetDestination` at the slot host.

Tabs additionally pull `TabChild` from `com.thomaskioko.tvmaniac.home.nav`. `TabChild` is the generic tab wrapper used by `TabRoot` factory lambdas.

`com.thomaskioko.tvmaniac.core.base.ActivityScope` is the default parent scope referenced by the generated binding contributions.

The Android UI renderer types live under `com.thomaskioko.tvmaniac.navigation.ui`. `ScreenContent` carries a `matches` predicate and a `@Composable (RootChild, Modifier)
-> Unit` content lambda. `SheetContent` does the same for `SheetChild`. The bindings generated for `@ScreenUi` and `@SheetUi` also reference
`androidx.compose.ui.Modifier`, because the screen variant forwards a modifier into the composable.

`@AppRoot` references one more Metro primitive, `dev.zacsweers.metro.SingleIn`, to scope the generated provider to the parent scope. `@AppRootUi` emits a top level
`@Composable` extension, so it references `androidx.compose.runtime.Composable` and `androidx.compose.ui.Modifier` directly. These three names are also hardcoded
constants in `util/External.kt`, with matching test stubs.

The processor is opinionated about these names. A project other than Tv Maniac would need to change `util/External.kt` to match its own navigation primitives. See
[architecture/consumer-contract.md](../internals/consumer-contract.md) for why these are constants and what a fork would change.


## Migration from earlier versions

Earlier releases shipped `@NavScreen`, `@TabScreen`, and `@NavSheet` as separate annotations. The single `@NavDestination(kind = ...)` API replaces them:

- `@NavScreen` becomes `@NavDestination(kind = DestinationKind.SCREEN)`.
- `@TabScreen` becomes `@NavDestination(kind = DestinationKind.TAB_ROOT)`.
- `@NavSheet` becomes `@NavDestination(kind = DestinationKind.OVERLAY)`.

See the [CHANGELOG](../changelog.md) for the version that introduced the change.
