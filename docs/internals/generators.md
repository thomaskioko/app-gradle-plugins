# Generators

The generator layer turns the typed intermediate values from [data-model.md](data-model.md) into KotlinPoet `FileSpec` outputs. Six generator files live under
`codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/codegen/`:

- `ScreenGraphGenerator.kt` emits the `@GraphExtension` interface plus its nested factory. It covers screens, tab roots, and child presenters, which all supply their
  names through the `GraphData` interface.
- `NavDestinationBindingGenerator.kt` emits the `NavDestination` and `NavRouteBinding` companion object bindings for screens and overlays.
- `TabDestinationBindingGenerator.kt` emits the `NavDestination.TabRoot`, the `NavRootBinding`, and the `NavRoot` singleton bindings for tab roots.
- `UiBindingGenerator.kt` emits the `@BindingContainer` object that contributes `ScreenContent` or `SheetContent` for composables annotated with `@ScreenUi`, `@SheetUi`,
  or `@TabUi`. The `kind` field on `UiBindingData` selects the variant.
- `AppRootBindingGenerator.kt` emits the `@BindingContainer` object that wires the nested `@AssistedFactory` to the bound interface for classes annotated with `@AppRoot`.
- `AppRootUiBindingGenerator.kt` emits the `AppRootProvider` interface plus the `@Composable AppRootProvider.AppRootContent(modifier)` extension for composables
  annotated with `@AppRootUi`.

`BindingFiles.kt` holds a shared scaffold that both destination binding generators reuse.

Each of the five paths (presenter `@NavDestination`, composable `@ScreenUi`/`@SheetUi`/`@TabUi`, presenter `@AppRoot`, composable `@AppRootUi`, presenter
`@ChildPresenter`) is dispatched from its own function on the processor entry. `processNavDestination` is the only one that picks between two generators, choosing the
screen or the tab binding from the parsed type:

```kotlin
val binding = when (data) {
    is ScreenData -> NavDestinationBindingGenerator.generate(data, hideFromObjC)
    is TabData -> TabDestinationBindingGenerator.generate(data, hideFromObjC)
}
writeFiles(declaration, listOf(ScreenGraphGenerator.generate(data, hideFromObjC), binding))
```

## Hiding from Objective-C

Every graph and destination binding generator takes a `hideFromObjC` flag. When it is true, the
emitted types (outer interface, nested `Factory`, companion object, `@AppRoot` binding object)
carry `@HiddenFromObjC` and the file carries `@file:OptIn(ExperimentalObjCRefinement::class)`.
The generated types are dependency injection plumbing no Swift code names, and exported
Objective-C classes cannot be dead-stripped, so hiding them keeps the whole layer out of the
framework header of any module a consumer exports.

The flag is computed once in `NavigationCodegenProcessorProvider`: true when
`environment.platforms` contains a non-JVM platform. `HiddenFromObjC` is an
`@OptionalExpectation` the compiler only accepts in common module sources, so a JVM-only
compilation (a plain JVM consumer, or the compile testing harness in `processor-test`) emits no
refinement annotations and produces the same output as before the flag existed. The golden files
cover the JVM path; `HideFromObjCTest` in the processor module asserts both paths on the
rendered `FileSpec` text. The examples in this document show the JVM shape.

The UI binding generators never take the flag. Their output lands in Android-only modules that no
framework exports.

Both branches reuse `ScreenGraphGenerator` because the `@GraphExtension` interface looks the same for screens, overlays, and tab roots. The route class doubles as the
scope marker either way. The two branches diverge only in which destination binding generator runs alongside.

## ScreenGraphGenerator

Produces a single `FileSpec` containing one public interface annotated `@GraphExtension(scope)` with one property and one nested `Factory` interface. The property exposes
either the presenter (for plain `@Inject` presenters and tabs) or the assisted factory type (for parameterized presenters). The factory interface is annotated
`@ContributesTo(parentScope) @GraphExtension.Factory` and declares the abstract `create<BaseName>Graph(@Provides componentContext: ComponentContext): <Graph>` factory
function.

Every name in the output (interface name, property name, factory function name) is read off the `NavData` rather than re derived in the generator. This is intentional.
See [data-model.md](data-model.md#naming-on-the-data-class) for why naming lives on the intermediate value.

## NavDestinationBindingGenerator

Produces a `@ContributesTo(parentScope)` interface with a companion that holds two `@Provides @IntoSet` functions:

- `provide<BaseName>NavDestination(graphFactory): NavDestination<*>`. Returns either `NavDestination.Screen(...)` or `NavDestination.Overlay(...)` depending on
  `ScreenData.kind`. The factory lambda receives `(route, componentContext)`. For a presenter with no runtime parameters it ignores the route, calls
  `graphFactory.create<BaseName>Graph(componentContext).<presenter>` directly, and wraps the result in `ScreenDestination(...)`. For a parameterized presenter it casts
  the route, reads the property the parser recorded, calls `graph.<factory>.create(route.<routeProperty>)`, and wraps the result in `ScreenDestination(...)`.
- `provide<BaseName>RouteBinding(): NavRouteBinding<*>`. Returns `NavRouteBinding(<Route>::class, <Route>.serializer())`. This entry feeds the polymorphic save and
  restore that the consumer's navigation state container performs when the process is killed and later restored.

The branch between the two destination bodies lives entirely inside `destinationBody`. Adding a new presenter form (for example, presenters that accept more than one
runtime parameter) would extend that one method.

## TabDestinationBindingGenerator

Mirrors `NavDestinationBindingGenerator` for tab roots. It contributes `NavDestination.TabRoot(...)` and `NavRootBinding<*>` instead of `Screen` and `NavRouteBinding`, plus a third `provide<BaseName>NavRoot()` function that contributes the route singleton into `Set<NavRoot>`. The third entry replaces the hand-written `<Feature>RootBinding` files consumers used to keep alongside each tab.
The factory lambda always wraps the produced presenter in `TabChild(...)` rather than `ScreenDestination(...)`. There is no parameterized branch because tabs are always
plain `@Inject`, enforced upstream by the parser.

## UiBindingGenerator

Produces a `@BindingContainer @ContributesTo(parentScope) object <FunctionName>UiBinding` containing one `@Provides @IntoSet` function returning `ScreenContent` or
`SheetContent`. A private `Variant` data class holds the values that vary between the two kinds:

```kotlin
private fun variantFor(kind: UiBindingKind): Variant = when (kind) {
    UiBindingKind.Screen -> Variant(ScreenContent, ScreenDestination, forwardsModifier = true)
    UiBindingKind.Sheet -> Variant(SheetContent, SheetDestination, forwardsModifier = false)
}
```

The `matches` lambda is `(it as? <DestinationType><*>)?.presenter is <PresenterType>`. The `content` lambda casts the child and invokes the composable as a `MemberName`
(KotlinPoet's `%M` interpolation), forwarding `presenter` always and `modifier` only when `forwardsModifier` is `true`. Overlays do not receive a modifier because
`SheetContent.content` is typed as `(SheetChild) -> Unit`. Modal layout decisions (a `ModalBottomSheet`, for example) belong inside the composable body, not at the call
site.

## BindingFiles

`contributingBindingFile(bindingName, parentScope, vararg providers)` builds the `@ContributesTo(parentScope) public interface <Name> { public companion object {
<providers> } }` scaffold that both `NavDestinationBindingGenerator` and `TabDestinationBindingGenerator` reuse. `UiBindingGenerator` does not use it because it emits a
`@BindingContainer object` instead of an `interface + companion`. The next section explains the difference.

## AppRootBindingGenerator

Produces a single `@BindingContainer @ContributesTo(parentScope) object <InterfaceName>BindingContainer` whose only function is annotated `@Provides @SingleIn(parentScope)`,
takes a `ComponentContext` and the nested `@AssistedFactory`, and returns the bound interface. The function body is one expression: `return factory.<factoryFunctionName>(componentContext)`.
The factory function name comes from `AppRootData.factoryFunctionName`, captured at parse time so a non-default name (`build`, `make`, etc.) works without generator
changes.

The output is intentionally byte-equivalent to the hand-written binding container the consumer would otherwise have to keep in sync. The reasoning: the generator
replaces a manually written file. Diffing the generated output against a checked-in golden lets contributors verify the equivalence at every change.

## AppRootUiBindingGenerator

Produces one `FileSpec` containing two declarations:

- An `AppRootProvider` interface with one `val` for each non-modifier parameter on the annotated composable. The properties are declared in declaration order; their
  names and types are read off `AppRootUiData.parameters`.
- A `@Composable AppRootProvider.AppRootContent(modifier: Modifier = Modifier)` extension that invokes the composable through `MemberName`, passing each parameter from
  the matching property on the receiver. When `AppRootUiData.hasModifier` is `true`, the extension forwards `modifier`; otherwise the parameter is omitted from the
  invocation.

The extension lives on the generated provider interface, not on a specific consumer graph type. Consumers make their `@DependencyGraph` extend `AppRootProvider`, which
keeps the codegen independent of the consumer's graph type and avoids a circular module dependency between `features/root/ui` (where the annotated composable lives)
and the consumer's `:app` module (where the graph lives).

## Two output structure decisions

Two non obvious choices in the output that are worth understanding before editing a generator.

### `@BindingContainer object` for UI bindings, `interface + companion` for destination bindings

The bindings the codegen emits for `@NavDestination` use Metro's `interface + companion object` structure. The bindings the codegen emits for `@ScreenUi`, `@SheetUi`, and
`@TabUi` use Metro's `@BindingContainer object` structure instead. The reason is a Metro detail.

`@Provides @IntoSet` declarations inside an `interface + companion object` only become contributions when Metro's `generateContributionProviders` flag is enabled, and
that flag is disabled in the consumer scaffold. The presenter side bindings work with the interface form because the consumer's Kotlin Multiplatform source set picks them
up through a separate Metro path. The Android only `ui` modules where the UI bindings land have no such fallback. Emitting `interface + companion` there would silently
produce an empty multibinding at build time. Targeting the `@BindingContainer object` structure is what makes the contributions discoverable without the flag.

### Route class as graph scope

`ScreenGraphGenerator` annotates each generated graph with `@GraphExtension(scope)`, where `scope` is the route class itself (for example `DebugRoute::class` or
`DiscoverRoot::class`). The processor never emits a separate `<Presenter>ScreenScope` class.

The reason is a KSP detail. KSP outputs land in source sets keyed by Kotlin Multiplatform target. A KSP generated `<Presenter>ScreenScope` in a presenter's `iosMain`
directory would not be visible from `commonMain`, sibling modules, or the consumer's app graph. The route class is already a manually written type in the feature's
`nav/api` module that is visible from every consumer. Reusing it as the scope marker keeps the graph universally addressable without needing a generated companion type,
which is what would force the visibility problem.
