# Testing

The processor is tested by `codegen/processor-test/`, a JVM test module. It runs the real `NavigationCodegenProcessor` over inline source strings using
`dev.zacsweers.kctfork` (a fork of `kotlin-compile-testing` with KSP2 support), and compares the generated files against checked in golden files.

## How a test runs

`ProcessorTestRunner.run(sources)` is the only entry point. It builds a `KotlinCompilation` from the given `Map<String, String>` of source files and registers
`NavigationCodegenProcessorProvider` as the only KSP processor. It then runs the compilation under KSP2, walks the KSP output directory, and returns every generated `.kt`
file as a `Map<file name -> contents>`, together with the raw `JvmCompilationResult`.

```kotlin
fun run(sources: Map<String, String>): RunResult {
    val compilation = KotlinCompilation().apply {
        this.sources = sources.map { (name, content) -> SourceFile.kotlin(name, content) }
        useKsp2()
        symbolProcessorProviders = mutableListOf(NavigationCodegenProcessorProvider())
        kspProcessorOptions = mutableMapOf()
        inheritClassPath = true
        messageOutputStream = System.out
    }
    val result = compilation.compile()
    val generated = collectGeneratedKotlinFiles(compilation.kspSourcesDir)
    return RunResult(result = result, generatedFiles = generated)
}
```

`inheritClassPath = true` gives the compilation the test module's runtime classpath. That is how it finds the real `codegen-annotations` jar, so the processor reads
the actual `@NavDestination` symbol and not a stub.

## The stubs

`TestStubs.kt` holds minimal source fakes of the consumer types the generators use. Each stub is a `Pair<String, String>` of file name and source
text. Three lists group them by what each set of tests needs.

- `baseStubs` is the common set: Decompose `ComponentContext`, `ActivityScope`, the navigation primitives, Metro annotations (including `@SingleIn`), and
  `kotlinx.serialization`. Every test uses these.
- `tabStubs` adds the `TabChild` type from the home navigation package (`com.thomaskioko.tvmaniac.home.nav`). Tab root tests and `@TabUi` tests use these.
- `uiStubs` adds the Compose UI annotations and the navigation UI primitives (`ScreenContent`, `SheetContent`). `@ScreenUi`, `@SheetUi`, and `@TabUi` tests use these.
- `appRootUiStubs` adds the same UI primitives plus a separate `androidx.compose.runtime.Composable` stub, because `@AppRootUi` puts that annotation directly on the
  generated extension. `@AppRootUi` tests use these.

We keep the stubs as small as possible. Their type signatures must match the constants in
`codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/util/External.kt` exactly. Compiling the test suite is what catches drift: if
the stubs and `External.kt` disagree, the end to end compilation fails. If you update one, update the other.

## Goldens

Each test asserts through `GoldenFileAssert.assertMatches(variant, fileName, actual)`. Goldens live under
`codegen/processor-test/src/test/resources/golden/<variant>/<file>.kt`. There are ten navigation codegen variants
(the feature flag processor keeps its own `featureflag/` golden, documented in [featureflag.md](../feature-flags.md)):

- `simple/` for `@NavDestination(kind = SCREEN)` with plain `@Inject`.
- `parameterized/` for `@NavDestination(kind = SCREEN)` with `@AssistedInject`.
- `tab/` for `@NavDestination(kind = TAB_ROOT)`.
- `screen-ui/` for `@ScreenUi`.
- `sheet-ui/` for `@SheetUi`.
- `tab-ui/` for `@TabUi`.
- `child-presenter/` for `@ChildPresenter` pinned to one host (`parentScope` is a tab root).
- `child-presenter-embeddable/` for `@ChildPresenter` made reusable (`parentScope` is the shared `ActivityScope`).
- `app-root/` for `@AppRoot`.
- `app-root-ui/` for `@AppRootUi`.

Tests are grouped by annotation, not by variant:

- `NavDestinationTest` covers all three `@NavDestination` kinds (SCREEN, OVERLAY, TAB_ROOT) plus the parameterized SCREEN variant.
- `ScreenUiTest` covers `@ScreenUi`, `SheetUiTest` covers `@SheetUi`, and `TabUiTest` covers `@TabUi`.
- `ChildPresenterTest` covers `@ChildPresenter` in both the flat (pinned) and embeddable (shared `ActivityScope`) shapes.
- `AppRootTest` covers `@AppRoot` plus three error paths (missing `@AssistedInject`, missing nested factory, missing bound interface).
- `AppRootUiTest` covers `@AppRootUi` plus two error paths (no parameter other than the modifier, presenter type mismatch).
- `ErrorPathTest` runs the validation branches of the navigation parsers and asserts on the compilation messages instead of a golden file.

`GoldenFileAssert` normalises both the expected and the actual text before comparing. It trims trailing whitespace on each line and trims the whole file, so a
trailing newline difference does not cause flaky failures.

## Updating goldens

Set `golden.update=true` (system property) or `GOLDEN_UPDATE=true` (environment variable) and run the suite again. `GoldenFileAssert` then writes the actual output to the
golden file instead of failing.

The repo wraps this in the `/update-golden` skill. It sets the property, runs the suite, and shows the diff so you can review the change before committing. Always read the
diff. Goldens are the contract you are committing to, and a bulk update nobody reviewed hides regressions.

## Adding a fixture

Adding a test fixture takes three steps.

1. Add a test class under `codegen/processor-test/src/test/kotlin/io/github/thomaskioko/codegen/processor/`. Build the input as a `Map<String, String>` of source
   files (usually `TestStubs.baseStubs` plus the feature source) and run it through `ProcessorTestRunner`. Assert that the expected files exist, and call
   `GoldenFileAssert.assertMatches` on each one.
2. Run the suite with `golden.update=true` to create the golden directory.
3. Read the generated files. If they look right, commit them. If not, fix the generator (or the parser) and run it again.

Do not commit a golden you have not read.
