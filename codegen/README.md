# Navigation Codegen

KSP-based code generation for Kotlin Multiplatform navigation destinations built on Metro DI and Decompose. Eliminates the
per-destination graph + binding boilerplate for screens, tabs, and modal sheets (bottom sheets, dialogs, or overlays) in a Metro-based navigation stack, and the per-screen renderer binding boilerplate on the Android Compose layer.

## Docs

- [Get started](docs/get-started.md) — what it does, how to wire it into a consumer project
- [Annotations reference](docs/annotations.md) — every parameter and the invariants the processor checks
- [Examples](docs/examples.md) — input + generated output for each shape

## References

- Decompose: https://arkivanov.github.io/Decompose/
- Metro: https://zacsweers.github.io/metro/
- KSP: https://kotlinlang.org/docs/ksp-overview.html
- kctfork: https://github.com/ZacSweers/kotlin-compile-testing
- KotlinPoet: https://square.github.io/kotlinpoet/
