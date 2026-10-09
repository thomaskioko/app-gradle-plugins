# Generators

The generator layer turns the typed values from [data-model.md](data-model.md) into KotlinPoet `FileSpec` outputs. There are six generator files under
`codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/codegen/`:

- `ScreenGraphGenerator.kt` emits the `@GraphExtension` interface and its nested factory. It handles screens, tab roots, and child presenters, which all provide their
  names through the `GraphData` interface.
- `NavDestinationBindingGenerator.kt` emits the `NavDestination` and `NavRouteBinding` companion object bindings for screens and overlays.
- `TabDestinationBindingGenerator.kt` emits the `NavDestination.TabRoot`, the `NavRootBinding`, and the `NavRoot` singleton bindings for tab roots.
- `UiBindingGenerator.kt` emits the `@BindingContainer` object that contributes `ScreenContent` or `SheetContent` for composables annotated with `@ScreenUi`, `@SheetUi`,
  or `@TabUi`. The `kind` field on `UiBindingData` picks the variant.
- `AppRootBindingGenerator.kt` emits the `@BindingContainer` object that connects the nested `@AssistedFactory` to the bound interface, for classes annotated with `@AppRoot`.
- `AppRootUiBindingGenerator.kt` emits the `AppRootProvider` interface and the `@Composable AppRootProvider.AppRootContent(modifier)` extension, for composables
  annotated with `@AppRootUi`.

`BindingFiles.kt` holds a shared scaffold that both destination binding generators use.

Each of the five paths (presenter `@NavDestination`, composable `@ScreenUi`/`@SheetUi`/`@TabUi`, presenter `@AppRoot`, composable `@AppRootUi`, presenter
`@ChildPresenter`) starts from its own function on the processor entry. `processNavDestination` is the only one that chooses between two generators. It picks the
screen or the tab binding based on the parsed type:

```kotlin
val binding = when (data) {
    is ScreenData -> NavDestinationBindingGenerator.generate(data, hideFromObjC)
    is TabData -> TabDestinationBindingGenerator.generate(data, hideFromObjC)
}
writeFiles(declaration, listOf(ScreenGraphGenerator.generate(data, hideFromObjC), binding))
```

Both branches use `ScreenGraphGenerator`, because the `@GraphExtension` interface looks the same for screens, overlays, and tab roots. The route class is the
scope marker in every case. The branches differ only in which destination binding generator runs next to it.

## Hiding from Objective-C

Every graph and destination binding generator takes a `hideFromObjC` flag. When it is true, the
emitted types (outer interface, nested `Factory`, companion object, `@AppRoot` binding object)
get `@HiddenFromObjC`, and the file gets `@file:OptIn(ExperimentalObjCRefinement::class)`.
No Swift code names these types; they are dependency injection plumbing. Exported Objective-C
classes cannot be dead stripped. So we hide them, which keeps this whole layer out of the
framework header of any module a consumer exports.

`NavigationCodegenProcessorProvider` computes the flag once. It is true when
`environment.platforms` contains a platform other than JVM. `HiddenFromObjC` is an
`@OptionalExpectation` that the compiler only accepts in common module sources. A compilation that only targets JVM
(a plain JVM consumer, or the compile testing harness in `processor-test`) therefore emits no
refinement annotations, and its output is the same as before the flag existed. The golden files
cover the JVM path. `HideFromObjCTest` in the processor module asserts both paths on the
rendered `FileSpec` text. The examples on this page show the JVM output.

The UI binding generators never take the flag. Their output lands in Android only modules that no
framework exports.

## ScreenGraphGenerator

This generator produces one `FileSpec` with one public interface annotated `@GraphExtension(scope)`. The interface has one property and one nested `Factory` interface. The property exposes
either the presenter (for plain `@Inject` presenters and tabs) or the assisted factory type (for parameterized presenters). The factory interface is annotated
`@ContributesTo(parentScope) @GraphExtension.Factory` and declares the abstract `create<BaseName>Graph(@Provides componentContext: ComponentContext): <Graph>` factory
function.

Every name in the output (interface name, property name, factory function name) comes from the `GraphData`. The generator never derives them again, and that is on purpose.
See [data-model.md](data-model.md#naming-on-the-data-class) for why naming lives on the intermediate value.

## NavDestinationBindingGenerator

This generator produces a `@ContributesTo(parentScope)` interface with a companion that holds two `@Provides @IntoSet` functions:

- `provide<BaseName>NavDestination(graphFactory): NavDestination<*>`. It returns `NavDestination.Screen(...)` or `NavDestination.Overlay(...)`, depending on
  `ScreenData.kind`. The factory lambda receives `(route, componentContext)`. For a presenter with no runtime parameters, it ignores the route, calls
  `graphFactory.create<BaseName>Graph(componentContext).<presenter>` directly, and wraps the result in `ScreenDestination(...)`. For a parameterized presenter, it casts
  the route, reads the property the parser recorded, calls `graph.<factory>.create(route.<routeProperty>)`, and wraps the result in `ScreenDestination(...)`.
- `provide<BaseName>RouteBinding(): NavRouteBinding<*>`. It returns `NavRouteBinding(<Route>::class, <Route>.serializer())`. This entry feeds the polymorphic save and
  restore that the consumer's navigation state container runs when the process is killed and later restored.

The choice between the two destination bodies lives entirely inside `destinationBody`. A new presenter form (for example, presenters that take more than one
runtime parameter) would only extend that one method.

## TabDestinationBindingGenerator

This generator mirrors `NavDestinationBindingGenerator` for tab roots. It contributes `NavDestination.TabRoot(...)` and `NavRootBinding<*>` instead of `Screen` and `NavRouteBinding`. It also adds a third function, `provide<BaseName>NavRoot()`, which contributes the route singleton into `Set<NavRoot>`. That third entry replaces the hand written `<Feature>RootBinding` files consumers used to keep next to each tab.
The factory lambda always wraps the presenter in `TabChild(...)` instead of `ScreenDestination(...)`. There is no parameterized branch, because tabs always use
plain `@Inject`, and the parser enforces that earlier.

## UiBindingGenerator

This generator produces a `@BindingContainer @ContributesTo(parentScope) object <FunctionName>UiBinding` with one `@Provides @IntoSet` function that returns `ScreenContent` or
`SheetContent`. A private `Variant` data class holds the values that differ between the three kinds. A tab pager renderer reuses `ScreenContent` and casts to `TabChild`:

```kotlin
private fun variantFor(kind: UiBindingKind): Variant = when (kind) {
    UiBindingKind.Screen -> Variant(ScreenContent, ScreenDestination, forwardsModifier = true)
    UiBindingKind.Sheet -> Variant(SheetContent, SheetDestination, forwardsModifier = false)
    UiBindingKind.Tab -> Variant(ScreenContent, TabChild, forwardsModifier = true)
}
```

The `matches` lambda is `(it as? <DestinationType><*>)?.presenter is <PresenterType>`. The `content` lambda casts the child and calls the composable as a `MemberName`
(KotlinPoet's `%M` interpolation). It always forwards `presenter`, and forwards `modifier` only when `forwardsModifier` is `true`. Overlays get no modifier, because
`SheetContent.content` is typed as `(SheetChild) -> Unit`. Modal layout choices (a `ModalBottomSheet`, for example) belong inside the composable body, not at the call
site.

## BindingFiles

`contributingBindingFile(bindingName, parentScope, vararg providers)` builds the `@ContributesTo(parentScope) public interface <Name> { public companion object {
<providers> } }` scaffold. Both `NavDestinationBindingGenerator` and `TabDestinationBindingGenerator` use it. `UiBindingGenerator` does not, because it emits a
`@BindingContainer object` instead of an `interface + companion`. The last section explains why.

## AppRootBindingGenerator

This generator produces one `@BindingContainer @ContributesTo(parentScope) object <InterfaceName>BindingContainer`. Its only function is annotated `@Provides @SingleIn(parentScope)`,
takes a `ComponentContext` and the nested `@AssistedFactory`, and returns the bound interface. The body is one expression: `return factory.<factoryFunctionName>(componentContext)`.
The factory function name comes from `AppRootData.factoryFunctionName`. We capture it at parse time, so a non default name (`build`, `make`, etc.) works without
changing the generator.

We deliberately keep the output byte equivalent to the binding container a consumer would otherwise write and maintain by hand. The generator replaces that file.
Diffing the output against a checked in golden lets contributors confirm the two still match on every change.

## AppRootUiBindingGenerator

This generator produces one `FileSpec` with two declarations:

- An `AppRootProvider` interface with one `val` for each parameter on the annotated composable other than the modifier. The properties follow declaration order. Their
  names and types come from `AppRootUiData.parameters`.
- A `@Composable AppRootProvider.AppRootContent(modifier: Modifier = Modifier)` extension that calls the composable through `MemberName`. It passes each parameter from
  the matching property on the receiver. When `AppRootUiData.hasModifier` is `true`, the extension forwards `modifier`. Otherwise it leaves that argument out of the
  call.

The extension sits on the generated provider interface, not on a specific consumer graph type. Consumers make their `@DependencyGraph` extend `AppRootProvider`. This
keeps the codegen independent of the consumer's graph type. It also avoids a circular module dependency between `features/root/ui` (where the annotated composable lives)
and the consumer's `:app` module (where the graph lives).

## Output structure

These are two choices in the output that are easy to miss. It is worth knowing them before you edit a generator.

### `@BindingContainer object` for UI bindings, `interface + companion` for destination bindings

The bindings we emit for `@NavDestination` use Metro's `interface + companion object` structure. The bindings we emit for `@ScreenUi`, `@SheetUi`, and
`@TabUi` use Metro's `@BindingContainer object` structure instead. The reason is a Metro detail.

`@Provides @IntoSet` declarations inside an `interface + companion object` only become contributions when Metro's `generateContributionProviders` flag is on. The
scaffold turns that flag off in `MetroSetup.kt` (`generateContributionProviders.set(false)`). The destination bindings in the Kotlin Multiplatform presenter modules
still get picked up in the interface form. The Android only `ui` modules, where the UI bindings land, do not pick up companion declarations. Emitting
`interface + companion` there would quietly produce an empty multibinding at build time. Using `@BindingContainer object` makes the contributions discoverable without the flag.

### Route class as graph scope

`ScreenGraphGenerator` annotates each generated graph with `@GraphExtension(scope)`, where `scope` is the route class itself (for example `DebugRoute::class` or
`DiscoverRoot::class`). The processor never emits a separate `<Presenter>ScreenScope` class.

The reason is a KSP detail. KSP output lands in source sets keyed by Kotlin Multiplatform target. A generated `<Presenter>ScreenScope` in a presenter's `iosMain`
directory would not be visible from `commonMain`, from other modules, or from the consumer's app graph. The route class is already a hand written type in the feature's
`nav/api` module, and every consumer can see it. Using it as the scope marker keeps the graph reachable from everywhere. A generated scope type is exactly what would
cause the visibility problem.
