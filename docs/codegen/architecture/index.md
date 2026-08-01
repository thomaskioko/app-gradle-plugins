# Architecture

These pages explain how the navigation codegen processor is built. They are written for contributors who want to read or modify the processor itself. If you only want to
use the codegen, start at [get-started.md](../get-started.md) and [annotations.md](../annotations.md) instead.

The processor turns one annotated symbol into one or two Kotlin source files on disk. Every page below covers one stage of that journey, in pipeline order.

1. [pipeline.md](pipeline.md) covers the KSP entry point: how the processor finds annotated symbols, decides what to do with each, and writes the result.
2. [data-model.md](data-model.md) covers the typed intermediate values that travel between the parser stage and the generator stage.
3. [parsers.md](parsers.md) covers how each annotation is read and validated, including the `@AssistedInject` detection and the rules that surface as compile errors.
4. [generators.md](generators.md) covers how the typed values are turned into Kotlin source through KotlinPoet, and the two non obvious structural decisions the generators make.
5. [consumer-contract.md](consumer-contract.md) covers the consumer project type names the processor depends on, the runtime flow from a navigation request to a rendered
   presenter, and how state is preserved across process death.
6. [testing.md](testing.md) covers the `kctfork` and golden file test setup, the test stubs that stand in for consumer types, and the workflow for updating goldens.

## Glossary

Every architecture page draws from this single vocabulary. New terms introduced on a specific page are defined inline on that page.

- **variant**. The structural form of a generated artifact. Nine variants exist: a presenter with no runtime parameters, a parameterized presenter, a tab root, a screen
  renderer, an overlay renderer, a tab pager renderer, a parent-owned child presenter graph, the application's root presenter binding, and the application's root host
  composable. Each variant has its own golden directory under `codegen/processor-test/src/test/resources/golden/`.
- **binding**. A Kotlin interface or object that contributes one or more entries to a Metro multibinding. The processor emits one binding per annotated destination and
  one per annotated UI renderer.
- **multibinding**. A Metro pattern where many `@Provides` contributions are collected into a single `Set<T>` that downstream code can request as a whole. The codegen
  feeds multibindings keyed by `NavDestination<*>`, `NavRouteBinding<*>`, `NavRootBinding<*>`, `ScreenContent`, and `SheetContent`.
- **graph extension**. A Metro `@GraphExtension` annotated interface that declares a fragment of a dependency injection graph scoped to a specific type. The codegen emits
  one for each annotated destination, using the route class as the scope marker.
- **route**. The class the user navigates to. For stack screens and overlays it implements the consumer's `NavRoute` interface. For tab roots it implements `NavRoot`. The
  route doubles as the graph extension's scope marker.
- **slot**. A Decompose primitive that hosts a single child at a time, used for modal overlays. The host filters the active overlay destinations and renders one of them
  in the slot.
- **router**. The internal `when` expression in the processor that picks the right code generator for each parsed annotation. Lives in `FileGenerator`.
- **aggregating**. A KSP incremental compilation flag. `aggregating = false` tells KSP that the generated file depends only on its own source file, so editing one feature
  does not force reprocessing of siblings.
- **Metro**. A compile time dependency injection framework by Zac Sweers. The processor emits Metro annotations (`@GraphExtension`, `@ContributesTo`, `@Provides`,
  `@IntoSet`, `@BindingContainer`). See [Metro docs](https://zacsweers.github.io/metro/).
- **Decompose**. A Kotlin Multiplatform navigation library by Arkadii Ivanov. The consumer project hosts presenters as Decompose components. See
  [Decompose docs](https://arkivanov.github.io/Decompose/).
- **KSP**. Kotlin Symbol Processing, the compiler API the processor uses to read annotated symbols and emit Kotlin source files. See
  [KSP docs](https://kotlinlang.org/docs/ksp-overview.html).

## Sub modules

The `codegen/` directory contains three Gradle sub modules that the architecture pages reference repeatedly.

- `annotations/` is a Kotlin Multiplatform library that defines `@NavDestination`, `@ScreenUi`, `@SheetUi`, `@TabUi`, `@ChildPresenter`, `@AppRoot`, and `@AppRootUi`. It
  carries no logic; it is just the surface that consumers depend on.
- `processor/` is a JVM library that hosts the KSP `SymbolProcessor` plus the KotlinPoet generators. This is where every architecture page after [pipeline.md](pipeline.md) lives.
- `processor-test/` is a JVM test module that uses `dev.zacsweers.kctfork` to compile annotated input and assert the output against goldens under
  `src/test/resources/golden/`. Covered in [testing.md](testing.md).
