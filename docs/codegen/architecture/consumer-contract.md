# Consumer contract

The processor emits Kotlin source files that reference fully qualified type names from the consumer project. Those names are not configurable. They are `ClassName`
constants in `codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/util/External.kt`, and every generator imports from there.

## What is hardcoded

The hardcoded names fall into six groups, organised by the role each plays at runtime.

**Decompose.** `com.arkivanov.decompose.ComponentContext` is the single Decompose type the codegen references. It appears as the `@Provides` parameter on every generated
graph factory function so every presenter on the graph can request it. `AppRootBindingGenerator` also references it as a constructor parameter on the generated
binding container function.

**Metro.** Every generated annotation references one of `dev.zacsweers.metro.{ContributesTo, GraphExtension, Provides, IntoSet, BindingContainer, SingleIn}`.
`BindingContainer` is used by `UiBindingGenerator` and `AppRootBindingGenerator`. `SingleIn` is used only by `AppRootBindingGenerator` to scope the generated provider
to the parent scope. The rationale for the binding container generators is in
[generators.md](generators.md#binding-container-object-for-ui-bindings-interface-companion-for-destination-bindings).

**Consumer navigation primitives** under `com.thomaskioko.tvmaniac.navigation`:

- `NavRoute`, `NavRoot`, `BaseRoute`. Marker interfaces for routable targets. The processor does not check inheritance at parse time; it relies on the consumer's compiler
  to fail if the route class does not implement the expected supertype.
- `NavDestination` and the nested `NavDestination.Screen`, `NavDestination.Overlay`, `NavDestination.TabRoot`. The multibinding entry types the generated `@Provides
  @IntoSet` functions return. The kind branches in `NavDestinationBindingGenerator` and `TabDestinationBindingGenerator` switch between these subclasses.
- `NavRouteBinding`, `NavRootBinding`. The route serializer multibinding entries that feed polymorphic state restoration. See
  [State save and restore](#state-save-and-restore) below.
- `RootChild`, `ScreenDestination`, `SheetChild`, `SheetDestination`. The slot and stack child wrappers the generated factory lambdas produce.

**Consumer UI primitives** under `com.thomaskioko.tvmaniac.navigation.ui`: `ScreenContent` and `SheetContent`. These are the multibinding entry types `UiBindingGenerator`
returns. Their constructor signatures (`matches: (RootChild) -> Boolean`, `content: @Composable (RootChild, Modifier) -> Unit` for `ScreenContent`; `(SheetChild) -> Unit`
for `SheetContent`) are part of this contract. Changing them in the consumer breaks every generated UI binding.

**Consumer home navigation** under `com.thomaskioko.tvmaniac.home.nav`: `TabChild`. The tab root factory lambda always wraps its produced presenter as `TabChild(...)`.

**Compose.** `androidx.compose.ui.Modifier` is referenced by `UiBindingGenerator` on the screen path (overlay renderers do not forward a modifier) and by
`AppRootUiBindingGenerator` on the generated extension. `androidx.compose.runtime.Composable` is referenced only by `AppRootUiBindingGenerator`, which emits the
annotation directly on the generated `AppRootContent` extension.

**App root primitives.** Whatever package the consumer chose. The `@AppRoot` and `@AppRootUi` generators do not reference any consumer-specific name; the bound
interface (`RootPresenter` in Tv Maniac), the implementation type, and the host composable all flow through from the annotated symbol via the parser. Consumers can
rename or repackage these without touching the codegen.

## Feature flag primitives

The feature flag codegen tier in `codegen/featureflag-processor` adds two consumer-side hardcoded names, listed in
`codegen/featureflag-processor/src/main/kotlin/io/github/thomaskioko/codegen/featureflag/processor/util/External.kt`.

**Consumer feature flag primitives** under `com.thomaskioko.tvmaniac.featureflags`:

- `FeatureFlag<T>`. The generic interface consumers inject. `FeatureFlagBindingGenerator` parameterises it with `Boolean` in every generated `@Provides` function and
  `@IntoSet` rebind. Changing the package or renaming the interface breaks every generated binding.
- `FeatureFlagFactory`. The construction surface. The generator emits `factory.boolean(key, title, description, defaultValue, dateAdded)` calls referencing this type.
  The factory must declare a `boolean(...)` method with that exact signature; the generator does not branch on alternative shapes.

**Feature flag Metro primitives.** Same `dev.zacsweers.metro` package as the navigation contract, but the feature flag generator also references `AppScope` directly as
the scope marker for `@ContributesTo(AppScope::class)` and `@SingleIn(AppScope::class)`, and `Qualifier` to stamp the generated `<BaseName>Qualifier` annotation
(`FeatureFlagQualifierGenerator`). Consumers that contribute their feature flags into a different scope must fork the processor and edit the matching `External.kt` constant.

**kotlinx.datetime.** `kotlinx.datetime.LocalDate` is the only datetime reference. The generator parses the annotation's `dateAdded` ISO String at codegen time and emits
a `LocalDate(year, month, day)` constructor call. Consumers must keep `kotlinx-datetime` on the classpath of every module that declares `@FeatureFlag` anchors.

The full feature flag codegen reference lives in [featureflag.md](../featureflag.md). The validation rules and error markers are documented there.

## Runtime flow

The pieces above interact at runtime when the user navigates to a destination. Tracing one navigation request from start to render makes the contract concrete.

1. The user calls `navigator.navigateTo(ShowDetailsRoute(showId = 42))` somewhere in a presenter.
2. The consumer's `Navigator` looks the route up in the activity scope's `Set<NavDestination<*>>` multibinding. Each entry is a generated `NavDestination.Screen`,
   `NavDestination.Overlay`, or `NavDestination.TabRoot`. The navigator picks the entry whose `routeClass` matches the route's runtime class.
3. The navigator inspects the entry's subclass. `NavDestination.Screen` goes onto the back stack. `NavDestination.Overlay` goes into the overlay slot.
   `NavDestination.TabRoot` switches the active tab.
4. The navigator calls the entry's factory lambda with `(route, componentContext)`. The factory lambda is the body inside `provide<BaseName>NavDestination` that the
   codegen emitted. For a parameterized presenter it casts the route, reads the property the parser recorded, and calls `factory.create(route.<routeProperty>)`. For a
   presenter with no runtime parameters it just reads the presenter off the graph. Either way, the lambda wraps the produced presenter in a `ScreenDestination` (for stack
   and overlay) or a `TabChild` (for tabs).
5. The slot or stack host now holds a `RootChild`. To render it as a Compose UI, the host iterates the activity scope's `Set<ScreenContent>` (or `Set<SheetContent>`)
   multibinding. Each entry is a generated `ScreenContent` (or `SheetContent`). The host calls each entry's `matches` predicate against the active child and picks the
   entry that returns `true`.
6. The host invokes the matched entry's `content` lambda with the active child and (for screens) a `Modifier`. The lambda casts the child, casts its presenter, and calls
   the annotated composable. The composable renders.

The codegen's job is to produce steps 4 and 6: the destination factory lambda and the UI content lambda. Steps 1, 2, 3, 5 live in the consumer project.

## State save and restore

When the operating system kills a backgrounded process, the navigation state must survive so the app can rebuild the same back stack on relaunch. Decompose serialises the
back stack as a list of route instances; the consumer is responsible for choosing a serializer for each route type. This is where `NavRouteBinding` and `NavRootBinding`
come in.

Every generated destination binding contributes one `NavRouteBinding<*>` (for `SCREEN` and `OVERLAY`) or `NavRootBinding<*>` (for `TAB_ROOT`) into the activity scope
multibinding. Each entry pairs a `KClass` with the route's `KSerializer`:

```kotlin
@Provides
@IntoSet
public fun provideShowsRouteBinding(): NavRouteBinding<*> =
    NavRouteBinding(ShowsRoute::class, ShowsRoute.serializer())
```

The consumer's serialization layer iterates this multibinding to build a `SerializersModule` keyed by route class. When Decompose persists the back stack on process death
and restores it on relaunch, it uses that module to encode and decode each route. The codegen never serialises anything itself; it only contributes the entries the
consumer's serialization layer needs.

Tabs use the parallel `NavRootBinding` so that `NavRoot` instances participate in the same polymorphic save and restore alongside `NavRoute` instances. Tab bindings additionally contribute the `NavRoot` singleton itself into `Set<NavRoot>`, which navigators iterate to enumerate the available tabs without consulting destination factories.

## Why constants rather than configuration

Hardcoding the consumer type names rather than reading them from KSP processor options is a deliberate decision, for three reasons.

1. **Single source of truth.** Every generator looks up the same `ClassName`. A typo or a rename in the consumer project produces one compile error in the generator
   output, not a per feature search.
2. **No KSP options to plumb.** Configurability would mean reading KSP processor options at every call site, plus a corresponding Gradle DSL knob in `useCodegen()`. The
   cost is real and the only known consumer is Tv Maniac.
3. **Fork friendly without being fork tolerant.** A non Tv Maniac consumer is expected to fork the processor and edit `External.kt` directly. The constants are
   `internal`, deliberately low friction to change in source, and impossible to override without a fork. That keeps the upstream processor opinionated and small.

## What a fork would change

For a project with a different navigation primitive set, the surface to edit is `External.kt` plus the corresponding fakes in
`codegen/processor-test/src/test/kotlin/io/github/thomaskioko/codegen/processor/TestStubs.kt`. The generators themselves do not need touching as long as the consumer's
primitives keep the same constructor signatures (for example `NavDestination.Screen(routeClass, factory)`, `ScreenContent(matches, content)`).

If a fork's primitives have a different structure (for example a single `Destination` type instead of the `Screen` / `Overlay` / `TabRoot` split), the generators do need
editing. The `ScreenKind` enum in `NavData` plus the `subclass` switch in `NavDestinationBindingGenerator.destinationFun` is the seam for that change.
