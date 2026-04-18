# Get started

Codegen is a KSP processor that eliminates the per-destination DI boilerplate for screens, tabs, modal sheets (bottom sheets, dialogs, or overlays presented via Decompose's `childSlot`), and the Android platform-side renderer bindings that wire composables into the root Compose stack. A single annotation replaces a manually authored Metro `@GraphExtension` plus binding, or a manually authored `ScreenContent` / `SheetContent` multibinding contribution.

## Why it exists

Each root destination in a Metro + Decompose KMP app needs three artifacts written manually:

1. `FooRoute` in `nav/api` — the feature's public API, stays manual.
2. `FooScreenGraph` in `presenter/di` — Metro `@GraphExtension`.
3. `FooNavDestinationBinding` in `presenter/di` — contributes `NavDestination` + `NavRouteBinding` to the Metro multibinding.

The graph and binding are mechanical and derive entirely from the presenter + route. Separately, each Android Compose screen needs a small renderer binding that contributes a `ScreenContent` (or `SheetContent`) into the activity-scope multibinding consumed by the root Compose stack. That binding is also mechanical and derives entirely from the composable + presenter type. The codegen generates all of the above from a single annotation per artifact. The route and the composable stay manual because they are the feature's public API; the route doubles as the `@GraphExtension` scope marker, and the composable is the entry point the annotation is placed on.

## Supported annotation shapes

Five annotations cover the shapes the processor knows how to generate. See [annotations.md](annotations.md) for the full reference and [examples.md](examples.md) for concrete inputs and outputs.

Shared-code presenter annotations (target `CLASS`, live in the KMP `presenter` module):

1. `@NavScreen` marks a root destination (simple or parameterized). It generates a graph scoped to the route plus a binding that contributes `NavDestination` and `NavRouteBinding` into the Metro multibindings. The processor auto-detects `@AssistedInject` and a nested `@AssistedFactory` to switch between the simple and parameterized generation paths.
2. `@TabScreen` marks a home tab destination. It generates a graph scoped to the config plus a binding that contributes `TabDestination`. No `NavRouteBinding` is emitted because the host feature owns its own config serialization.
3. `@NavSheet` marks a modal sheet destination, a bottom sheet, dialog, or overlay presented on top of the current destination through Decompose's `childSlot`. It generates a graph scoped to the config plus a binding that contributes `SheetChildFactory` and `SheetConfigBinding`.

Android UI renderer annotations (target `FUNCTION`, live in the Android `ui` module):

4. `@ScreenUi` marks a `@Composable` screen function as the Android-side renderer for a root-stack presenter. It generates a `@BindingContainer` object contributing a `ScreenContent` into `Set<ScreenContent>` so the root Compose stack can iterate the set and dispatch to the right screen.
5. `@SheetUi` marks a `@Composable` sheet function as the Android-side renderer for a modal sheet presenter. Parallel to `@ScreenUi` but contributes a `SheetContent` into `Set<SheetContent>`.

## Dependency

1. Apply the plugin DSL. In a KMP presenter module's `build.gradle.kts`:

   ```kotlin
   plugins {
       alias(libs.plugins.app.kmp)
   }

   scaffold {
       useCodegen()
   }
   ```

   Same call in an Android `ui` module when using `@ScreenUi` / `@SheetUi`:

   ```kotlin
   plugins {
       alias(libs.plugins.app.android)
   }

   scaffold {
       useMetro()
       useCodegen()

       android {
           useCompose()
       }
   }
   ```

   `useCodegen()` is defined on `BaseExtension` in `plugins/src/main/kotlin/io/github/thomaskioko/gradle/plugins/extensions/BaseExtension.kt`. It applies `com.google.devtools.ksp`, adds `codegen-annotations` to the appropriate implementation configuration, and registers `codegen-processor` as a KSP processor for every target in the module.

2. Declare the two library entries in the consumer's `libs.versions.toml` so the DSL can resolve them via the version catalog:

   ```toml
   [libraries]
   codegen-annotations = { module = "io.github.thomaskioko.gradle.plugins:codegen-annotations", version.ref = "app-gradle-plugins" }
   codegen-processor = { module = "io.github.thomaskioko.gradle.plugins:codegen-processor", version.ref = "app-gradle-plugins" }
   ```

## Basic usage

Annotate the presenter (shared KMP layer):

```kotlin
@Inject
@NavScreen(route = DebugRoute::class, parentScope = ActivityScope::class)
public class DebugPresenter(...) : ComponentContext by componentContext
```

Annotate the matching composable (Android ui layer):

```kotlin
@ScreenUi(presenter = DebugPresenter::class, parentScope = ActivityScope::class)
@Composable
public fun DebugMenuScreen(
    presenter: DebugPresenter,
    modifier: Modifier = Modifier,
) { ... }
```

Build the modules. KSP generates the graph + nav binding into the presenter module's `di/` package, and the `ScreenContent` binding into the ui module's `di/` package. No further wiring is required inside each module.

For the `@ScreenUi` / `@SheetUi` contributions to be picked up by the app's Metro graph, the app module must declare a direct `implementation` dependency on each feature `ui` module. Transitive `implementation` dependencies through a root ui module do not expose the generated `metro/hints/` classes on the app's compile classpath, and Metro will report the multibinding as unexpectedly empty at build time.

## References

- Decompose: https://arkivanov.github.io/Decompose/
- Metro: https://zacsweers.github.io/metro/
- KSP: https://kotlinlang.org/docs/ksp-overview.html
- kctfork: https://github.com/ZacSweers/kotlin-compile-testing
- KotlinPoet: https://square.github.io/kotlinpoet/
