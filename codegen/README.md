# Navigation Codegen

KSP based code generation for Kotlin Multiplatform navigation destinations. The codegen targets
[Decompose](https://arkivanov.github.io/Decompose/) for navigation and
[Metro](https://zacsweers.github.io/metro/) for dependency injection. It does not work with
Jetpack Navigation, Voyager, Appyx, or any other navigation library; the generated output references
Decompose's `ChildStack`, `ChildSlot`, and `ComponentContext` primitives directly.

One annotation (`@NavDestination`) covers stack screens, modal overlays, and bottom navigation tab
roots. Two more (`@ScreenUi`, `@SheetUi`) cover the renderer bindings that join Android composables
to the navigation host. The processor emits the boilerplate that each destination would otherwise
need: a Metro `@GraphExtension` graph, a `NavDestination` binding for the Decompose host, and a UI
renderer binding for the Android side.

## What you need

The consumer project must already use Decompose and Metro. Specifically:

- Decompose components hosted as presenters, with each destination identified by a `NavRoute` or
  `NavRoot` class.
- Metro dependency graphs that the generated `@GraphExtension` can plug into.
- A Decompose-based navigation host (typically a `ChildStack` for screens plus a `ChildSlot` for
  overlays) that consumes the `Set<NavDestination<*>>` multibinding the codegen contributes to.

The Tv Maniac project is the reference implementation. The full runtime contract, including the
exact consumer types the generated code references, lives in
[architecture/consumer-contract.md](docs/architecture/consumer-contract.md).

## Docs

- [Get started](docs/get-started.md): what the codegen does and how to wire it into a consumer project.
- [Annotation reference](docs/annotations.md): every parameter and the validation rules the processor enforces.
- [Examples](docs/examples.md): input and generated output for every variant.
- [Architecture](docs/architecture/index.md): how the processor is built, for contributors.

## References

- Decompose: https://arkivanov.github.io/Decompose/
- Metro: https://zacsweers.github.io/metro/
- KSP: https://kotlinlang.org/docs/ksp-overview.html
- kctfork: https://github.com/ZacSweers/kotlin-compile-testing
- KotlinPoet: https://square.github.io/kotlinpoet/
