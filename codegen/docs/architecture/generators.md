# Generators

The generator layer turns the typed intermediate values from [data-model.md](data-model.md) into KotlinPoet `FileSpec` outputs. Five generator files live under
`codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/codegen/`:

- `FileGenerator.kt` is the router that picks which generators run for each `NavData`.
- `ScreenGraphGenerator.kt` emits the `@GraphExtension` interface plus its nested factory.
- `NavDestinationBindingGenerator.kt` emits the `NavDestination` and `NavRouteBinding` companion object bindings for screens and overlays.
- `TabDestinationBindingGenerator.kt` emits the `NavDestination.TabRoot` and `NavRootBinding` bindings for tab roots.
- `UiBindingGenerator.kt` emits the `@BindingContainer` object that contributes `ScreenContent` or `SheetContent` for composables annotated with `@ScreenUi` or `@SheetUi`.

`BindingFiles.kt` holds a shared scaffold that both destination binding generators reuse.

The two paths (one for annotated presenter classes, one for annotated composable functions) converge on KotlinPoet types but never share a generator entry point.
Presenter classes route through `FileGenerator`; composables go directly to `UiBindingGenerator`.

## FileGenerator routing

The router is a single `when` expression over the sealed `NavData`:

```kotlin
internal object FileGenerator {
    fun generate(data: NavData): List<FileSpec> = when (data) {
        is ScreenData -> listOf(
            ScreenGraphGenerator.generate(data),
            NavDestinationBindingGenerator.generate(data),
        )
        is TabData -> listOf(
            ScreenGraphGenerator.generate(data),
            TabDestinationBindingGenerator.generate(data),
        )
    }
}
```

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

Mirrors `NavDestinationBindingGenerator` for tab roots. It contributes `NavDestination.TabRoot(...)` and `NavRootBinding<*>` instead of `Screen` and `NavRouteBinding`.
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

## Two output structure decisions

Two non obvious choices in the output that are worth understanding before editing a generator.

### `@BindingContainer object` for UI bindings, `interface + companion` for destination bindings

The bindings the codegen emits for `@NavDestination` use Metro's `interface + companion object` structure. The bindings the codegen emits for `@ScreenUi` and `@SheetUi`
use Metro's `@BindingContainer object` structure instead. The reason is a Metro detail.

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
