# Consumer contract

The consumer contract is the set of type names from the consumer project that the generated code refers to. The processor emits Kotlin source that uses these fully
qualified names, and they are not configurable. They are `ClassName` constants in `codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/util/External.kt`,
and every generator imports them from there.

## Hardcoded names

The hardcoded names fall into eight groups, sorted by the role each one plays at runtime.

**Decompose.** `com.arkivanov.decompose.ComponentContext` is the only Decompose type the codegen uses. It is the `@Provides` parameter on every generated
graph factory function, so every presenter on the graph can request it. `AppRootBindingGenerator` also uses it as a parameter on the generated
binding container function.

**Kotlin standard library.** `kotlin.OptIn`, `kotlin.native.HiddenFromObjC`, and `kotlin.experimental.ExperimentalObjCRefinement`. The graph and destination binding
generators use them to hide generated types from Objective-C when the compilation has a non JVM target. See
[generators.md](generators.md#hiding-from-objective-c).

**Metro.** Every generated annotation is one of `dev.zacsweers.metro.{ContributesTo, GraphExtension, Provides, IntoSet, BindingContainer, SingleIn}`.
`UiBindingGenerator` and `AppRootBindingGenerator` use `BindingContainer`. Only `AppRootBindingGenerator` uses `SingleIn`, to scope the generated provider
to the parent scope. We explain the binding container choice in
[generators.md](generators.md#bindingcontainer-object-for-ui-bindings-interface-companion-for-destination-bindings).

**Consumer navigation primitives** under `com.thomaskioko.tvmaniac.navigation`:

- `NavRoute`, `NavRoot`, `BaseRoute`. Marker interfaces for targets you can navigate to. The processor does not check inheritance at parse time. It relies on the consumer's compiler
  to fail if the route class does not implement the expected supertype.
- `NavDestination` and its nested `NavDestination.Screen`, `NavDestination.Overlay`, `NavDestination.TabRoot`. These are the multibinding entry types that the generated `@Provides
  @IntoSet` functions return. The kind branches in `NavDestinationBindingGenerator` and `TabDestinationBindingGenerator` choose between these subclasses.
- `NavRouteBinding`, `NavRootBinding`. The route serializer multibinding entries that feed polymorphic state restoration. See
  [State save and restore](#state-save-and-restore) below.
- `RootChild`, `ScreenDestination`, `SheetChild`, `SheetDestination`. The slot and stack child wrappers that the generated factory lambdas produce.

**Consumer UI primitives** under `com.thomaskioko.tvmaniac.navigation.ui`: `ScreenContent` and `SheetContent`. These are the multibinding entry types `UiBindingGenerator`
returns. Their constructor signatures (`matches: (RootChild) -> Boolean`, `content: @Composable (RootChild, Modifier) -> Unit` for `ScreenContent`; `(SheetChild) -> Unit`
for `SheetContent`) are part of the contract. If the consumer changes them, every generated UI binding breaks.

**Consumer home navigation** under `com.thomaskioko.tvmaniac.home.nav`: `TabChild`. The tab root factory lambda always wraps its presenter as `TabChild(...)`.

**Compose.** Only `AppRootUiBindingGenerator` names `androidx.compose.ui.Modifier` and `androidx.compose.runtime.Composable`. It puts `@Composable` directly on the
generated `AppRootContent` extension and declares its `modifier: Modifier` parameter. `UiBindingGenerator` never names either type. For screen and tab renderers it
forwards the `modifier` argument that the consumer's `ScreenContent` lambda passes in. Overlay renderers forward no modifier.

**App root primitives.** These live in whatever package the consumer chose. The `@AppRoot` and `@AppRootUi` generators do not use any consumer specific name. The bound
interface (`RootPresenter` in Tv Maniac), the implementation type, and the host composable all come from the annotated symbol through the parser. Consumers can
rename or move these without touching the codegen.

## Feature flag primitives

The feature flag codegen in `codegen/featureflag-processor` adds two more hardcoded consumer names. They are listed in
`codegen/featureflag-processor/src/main/kotlin/io/github/thomaskioko/codegen/featureflag/processor/util/External.kt`.

**Consumer feature flag primitives** under `com.thomaskioko.tvmaniac.featureflags`:

- `FeatureFlag<T>`. The generic interface consumers inject. `FeatureFlagBindingGenerator` uses it as `FeatureFlag<Boolean>` in every generated `@Provides` function and
  `@IntoSet` rebind. Moving or renaming the interface breaks every generated binding.
- `FeatureFlagFactory`. The type that builds flags. The generator emits `factory.boolean(key, title, description, defaultValue, dateAdded)` calls on it.
  The factory must declare a `boolean(...)` method with exactly that signature. The generator does not handle other shapes.

**Feature flag Metro primitives.** These come from the same `dev.zacsweers.metro` package as the navigation contract. The feature flag generator also uses `AppScope` directly as
the scope marker for `@ContributesTo(AppScope::class)` and `@SingleIn(AppScope::class)`. It uses `Qualifier` to mark the generated `<BaseName>Qualifier` annotation
(`FeatureFlagQualifierGenerator`). If you contribute feature flags into a different scope, you need to fork the processor and edit the matching `External.kt` constant.

**kotlinx.datetime.** `kotlinx.datetime.LocalDate` is the only datetime type used. The generator parses the annotation's `dateAdded` ISO String at codegen time and emits
a `LocalDate(year, month, day)` constructor call. Every module that declares `@FeatureFlag` anchors needs `kotlinx-datetime` on its classpath.

The full feature flag codegen reference is in [featureflag.md](../feature-flags.md), including the validation rules and error markers.

## Runtime flow

The pieces above work together when the user navigates to a destination. Following one navigation request from start to render makes the contract concrete.

1. A presenter calls `navigator.navigateTo(ShowDetailsRoute(showId = 42))`.
2. The consumer's `Navigator` looks up the route in the activity scope's `Set<NavDestination<*>>` multibinding. Each entry is a generated `NavDestination.Screen`,
   `NavDestination.Overlay`, or `NavDestination.TabRoot`. The navigator picks the entry whose `routeClass` matches the route's runtime class.
3. The navigator checks the entry's subclass. `NavDestination.Screen` goes onto the back stack. `NavDestination.Overlay` goes into the overlay slot.
   `NavDestination.TabRoot` switches the active tab.
4. The navigator calls the entry's factory lambda with `(route, componentContext)`. That lambda is the body the codegen emitted inside
   `provide<BaseName>NavDestination`. For a parameterized presenter, it casts the route, reads the property the parser recorded, and calls `factory.create(route.<routeProperty>)`. For a
   presenter with no runtime parameters, it just reads the presenter off the graph. Either way, the lambda wraps the presenter in a `ScreenDestination` (for stack
   and overlay) or a `TabChild` (for tabs).
5. The slot or stack host now holds a `RootChild`. To render it, the host goes through the activity scope's `Set<ScreenContent>` (or `Set<SheetContent>`)
   multibinding. Each entry is a generated `ScreenContent` (or `SheetContent`). The host calls each entry's `matches` predicate on the active child and picks the
   entry that returns `true`.
6. The host calls the matched entry's `content` lambda with the active child and, for screens, a `Modifier`. The lambda casts the child, casts its presenter, and calls
   the annotated composable. The composable renders.

The codegen produces steps 4 and 6: the destination factory lambda and the UI content lambda. Steps 1, 2, 3, and 5 live in the consumer project.

## State save and restore

When the operating system kills a backgrounded process, the navigation state has to survive, so the app can rebuild the same back stack on relaunch. Decompose serialises the
back stack as a list of route instances. The consumer picks a serializer for each route type. That is where `NavRouteBinding` and `NavRootBinding`
come in.

Every generated destination binding contributes one `NavRouteBinding<*>` (for `SCREEN` and `OVERLAY`) or `NavRootBinding<*>` (for `TAB_ROOT`) into the activity scope
multibinding. Each entry pairs a `KClass` with the route's `KSerializer`:

```kotlin
@Provides
@IntoSet
public fun provideShowsRouteBinding(): NavRouteBinding<*> =
    NavRouteBinding(ShowsRoute::class, ShowsRoute.serializer())
```

The consumer's serialization layer reads this multibinding to build a `SerializersModule` keyed by route class. Decompose uses that module to encode each route when it
saves the back stack on process death, and to decode it on relaunch. The codegen never serialises anything itself. It only contributes the entries the
consumer's serialization layer needs.

Tabs use the parallel `NavRootBinding`, so `NavRoot` instances take part in the same polymorphic save and restore as `NavRoute` instances. Tab bindings also contribute the `NavRoot` singleton itself into `Set<NavRoot>`. Navigators read that set to list the available tabs without going through destination factories.

## Constants over configuration

We hardcode the consumer type names on purpose, instead of reading them from KSP processor options. There are three reasons.

1. **Single source of truth.** Every generator looks up the same `ClassName`. A typo or a rename in the consumer project shows up as one compile error in the generated
   output, not as a search through every feature.
2. **No KSP options to plumb.** Configuration would mean reading KSP processor options at every call site, plus a matching Gradle DSL setting in `useCodegen()`. That
   cost is real, and the only known consumer is Tv Maniac.
3. **Easy to fork, but you do have to fork.** A consumer other than Tv Maniac is expected to fork the processor and edit `External.kt` directly. The constants are
   `internal`. They are easy to change in source and impossible to override without a fork. That keeps the upstream processor opinionated and small.

## Forking

For a project with a different set of navigation primitives, you edit `External.kt` and the matching fakes in
`codegen/processor-test/src/test/kotlin/io/github/thomaskioko/codegen/processor/TestStubs.kt`. The generators stay as they are, as long as the consumer's
primitives keep the same constructor signatures (for example `NavDestination.Screen(routeClass, factory)`, `ScreenContent(matches, content)`).

If a fork's primitives have a different structure (for example one `Destination` type instead of the `Screen` / `Overlay` / `TabRoot` split), the generators do need
changes. The place to make them is the `ScreenKind` enum in `NavData` together with the `subclass` switch in `NavDestinationBindingGenerator.destinationFun`.
