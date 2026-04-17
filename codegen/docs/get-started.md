# Get started

Codegen is a KSP processor that eliminates the per-destination DI boilerplate for screens, tabs, and modal sheets (bottom sheets, dialogs, or overlays presented via Decompose's `childSlot`) in a Metro + Decompose Kotlin Multiplatform app. One annotation on the presenter generates both the Metro `@GraphExtension` graph and the binding that contributes it into the navigation multibinding.

## Why it exists

Each root destination in a Metro + Decompose KMP app hand-writes three artifacts:

1. `FooRoute` in `nav/api` — the feature's public API, stays hand-written.
2. `FooScreenGraph` in `presenter/di` — Metro `@GraphExtension`.
3. `FooNavDestinationBinding` in `presenter/di` — contributes `NavDestination` + `NavRouteBinding` to the Metro multibinding.

The graph and binding are mechanical and derive entirely from the presenter + route. The codegen generates both from a single annotation. The route stays hand-written because it is the feature's public API, and it doubles as the `@GraphExtension` scope marker — no separate scope class is generated.

## Supported annotation shapes

Three annotations cover the navigation shapes the processor knows how to generate. See [annotations.md](annotations.md) for the full reference and [examples.md](examples.md) for concrete inputs and outputs.

1. `@NavScreen` marks a root destination (simple or parameterized). It generates a graph scoped to the route plus a binding that contributes `NavDestination` and `NavRouteBinding` into the Metro multibindings. The processor auto-detects `@AssistedInject` and a nested `@AssistedFactory` to switch between the simple and parameterized generation paths.
2. `@TabScreen` marks a home tab destination. It generates a graph scoped to the config plus a binding that contributes `TabDestination`. No `NavRouteBinding` is emitted because the host feature owns its own config serialization.
3. `@NavSheet` marks a modal sheet destination — a bottom sheet, dialog, or overlay presented on top of the current destination through Decompose's `childSlot`. It generates a graph scoped to the config plus a binding that contributes `SheetChildFactory` and `SheetConfigBinding`.

## Dependency

1. Apply the plugin DSL in a presenter module's `build.gradle.kts`:

   ```kotlin
   plugins {
       alias(libs.plugins.app.kmp)
   }

   scaffold {
       useCodegen()
   }
   ```

   `useCodegen()` is defined on `BaseExtension` in `plugins/src/main/kotlin/io/github/thomaskioko/gradle/plugins/extensions/BaseExtension.kt`. It applies `com.google.devtools.ksp`, adds `codegen-annotations` to `commonMainImplementation`, and registers `codegen-processor` as a KSP processor for every KMP target.

2. Declare the two library entries in the consumer's `libs.versions.toml` so the DSL can resolve them via the version catalog:

   ```toml
   [libraries]
   codegen-annotations = { module = "io.github.thomaskioko.gradle.plugins:codegen-annotations", version.ref = "app-gradle-plugins" }
   codegen-processor = { module = "io.github.thomaskioko.gradle.plugins:codegen-processor", version.ref = "app-gradle-plugins" }
   ```

## Basic usage

Annotate the presenter:

```kotlin
@Inject
@NavScreen(route = DebugRoute::class, parentScope = ActivityScope::class)
public class DebugPresenter(...) : ComponentContext by componentContext
```

Build the module. KSP generates the graph and binding into the presenter module's `di/` package. No further wiring is required.

## References

- Decompose: https://arkivanov.github.io/Decompose/
- Metro: https://zacsweers.github.io/metro/
- KSP: https://kotlinlang.org/docs/ksp-overview.html
- kctfork: https://github.com/ZacSweers/kotlin-compile-testing
- KotlinPoet: https://square.github.io/kotlinpoet/

