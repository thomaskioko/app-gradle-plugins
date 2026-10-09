# Architecture

These pages describe how the navigation codegen processor is built. They are for contributors who want to read or change the processor itself. If you only want to
use the codegen, start at [get-started.md](../navigation/get-started.md) and [annotations.md](../navigation/annotations.md) instead.

The processor turns one annotated symbol into one or two Kotlin source files. Each page below covers one stage of that path, in pipeline order.

1. [pipeline.md](pipeline.md) covers the KSP entry point: how we find annotated symbols, decide what to do with each one, and write the result.
2. [data-model.md](data-model.md) covers the typed values we pass from the parser stage to the generator stage.
3. [parsers.md](parsers.md) covers how we read and validate each annotation, including `@AssistedInject` detection and the rules that show up as compile errors.
4. [generators.md](generators.md) covers how we turn the typed values into Kotlin source with KotlinPoet, and two structural choices in the output that are easy to miss.
5. [consumer-contract.md](consumer-contract.md) covers the consumer type names the processor depends on, the runtime flow from a navigation request to a rendered
   presenter, and how state survives process death.
6. [testing.md](testing.md) covers the `kctfork` and golden file test setup, the test stubs that stand in for consumer types, and how to update goldens.

## Glossary

All the architecture pages use this vocabulary. Terms that only one page needs are defined on that page.

- **variant**. The structural form of a generated artifact. There are eleven, and each has its own golden directory under
  `codegen/processor-test/src/test/resources/golden/`: a presenter with no runtime parameters, a parameterized presenter, a tab root, a screen renderer, an overlay
  renderer, a tab pager renderer, a child presenter graph pinned to one host, a reusable child presenter graph, a parameterized child presenter graph, the
  application's root presenter binding, and the application's root host composable. Overlay presenters have no golden. `NavDestinationTest` checks the
  `NavDestination.Overlay` subclass inline instead. [testing.md](testing.md#goldens) maps each directory to its annotation.
- **binding**. A Kotlin interface or object that adds one or more entries to a Metro multibinding. The processor emits one binding for each annotated destination and
  one for each annotated UI renderer.
- **multibinding**. A Metro pattern where many `@Provides` contributions are collected into one `Set<T>`, which other code can request as a whole. The codegen
  feeds multibindings of `NavDestination<*>`, `NavRouteBinding<*>`, `NavRootBinding<*>`, `NavRoot`, `ScreenContent`, and `SheetContent`.
- **graph extension**. A Metro interface annotated with `@GraphExtension`. It declares part of a dependency injection graph, scoped to a specific type. The codegen emits
  one for each annotated destination, with the route class as the scope marker, and one for each `@ChildPresenter`, with its `scope` argument as the marker.
- **route**. The class the user navigates to. For stack screens and overlays it implements the consumer's `NavRoute` interface. For tab roots it implements `NavRoot`. The
  route also serves as the graph extension's scope marker.
- **slot**. A Decompose primitive that hosts one child at a time. We use it for modal overlays. The host filters the active overlay destinations and renders one of them
  in the slot.
- **router**. The `when` expression in `processNavDestination` that picks the screen or the tab binding generator for a parsed `@NavDestination`.
- **aggregating**. A KSP incremental compilation flag. `aggregating = false` tells KSP that a generated file depends only on its own source file. Editing one feature
  then does not force KSP to reprocess the others.
- **Metro**. A compile time dependency injection framework by Zac Sweers. The processor emits Metro annotations (`@GraphExtension`, `@ContributesTo`, `@Provides`,
  `@IntoSet`, `@BindingContainer`). See [Metro docs](https://zacsweers.github.io/metro/).
- **Decompose**. A Kotlin Multiplatform navigation library by Arkadii Ivanov. The consumer project hosts presenters as Decompose components. See
  [Decompose docs](https://arkivanov.github.io/Decompose/).
- **KSP**. Kotlin Symbol Processing, the compiler API the processor uses to read annotated symbols and emit Kotlin source files. See
  [KSP docs](https://kotlinlang.org/docs/ksp-overview.html).

## Modules

The `codegen/` build has five Gradle sub modules, listed in `codegen/settings.gradle.kts`. These pages cover the three navigation modules. The other two,
`featureflag-annotations/` and `featureflag-processor/`, belong to the feature flag codegen described in [feature-flags.md](../feature-flags.md).

- `annotations/` is a Kotlin Multiplatform library that defines `@NavDestination`, `@ScreenUi`, `@SheetUi`, `@TabUi`, `@ChildPresenter`, `@AppRoot`, and `@AppRootUi`. It
  has no logic. It is only the surface consumers depend on.
- `processor/` is a JVM library with the KSP `SymbolProcessor` and the KotlinPoet generators. Every page after [pipeline.md](pipeline.md) is about code in this module.
- `processor-test/` is a JVM test module. It uses `dev.zacsweers.kctfork` to compile annotated input and compares the output against goldens under
  `src/test/resources/golden/`. It tests both processors. See [testing.md](testing.md).
