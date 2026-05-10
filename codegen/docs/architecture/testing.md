# Testing

The processor is exercised by `codegen/processor-test/`, a JVM test module that runs the real `NavigationCodegenProcessor` over inline source strings using
`dev.zacsweers.kctfork` (a fork of `kotlin-compile-testing` with KSP2 support) and asserts the produced files against checked in golden files.

## How a test runs

`ProcessorTestRunner.run(sources)` is the single entry point. It builds a `KotlinCompilation`, hands it the supplied `Map<String, String>` of source files, registers
`NavigationCodegenProcessorProvider` as the only KSP processor, runs the compilation under KSP2, then walks the KSP output directory and returns every generated `.kt`
file as a `Map<file name -> contents>` alongside the raw `JvmCompilationResult`.

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

`inheritClassPath = true` lets the compilation see the test module's runtime classpath, which is how it picks up the real `codegen-annotations` jar. The processor reads
the actual `@NavDestination` symbol, not a stub.

## The stubs

`TestStubs.kt` carries minimal source level fakes of the consumer project types the generator references. Each stub is a `Pair<String, String>` of file name and source
text. Three lists group them by what each set of tests needs.

- `baseStubs` covers the common set: Decompose `ComponentContext`, `ActivityScope`, the navigation primitives, Metro annotations (including `@SingleIn`),
  `kotlinx.serialization`. Every test pulls these.
- `tabStubs` adds the `TabChild` type from the home navigation package (`com.thomaskioko.tvmaniac.home.nav`). Tab root tests and `@TabUi` tests pull these.
- `uiStubs` adds the Compose UI annotations and the navigation UI primitives (`ScreenContent`, `SheetContent`). `@ScreenUi`, `@SheetUi`, and `@TabUi` tests pull these.
- `appRootUiStubs` adds the same UI primitives plus a separate `androidx.compose.runtime.Composable` stub because `@AppRootUi` emits the annotation directly on the
  generated extension. `@AppRootUi` tests pull these.

The stubs are deliberately minimal. Their type signatures must match the constants in
`codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/util/External.kt` exactly. Compilation in the test suite is the contract that catches drift: if
the stubs and `External.kt` disagree, end to end compilation fails. Updating one means updating the other.

## Goldens

Each test asserts through `GoldenFileAssert.assertMatches(variant, fileName, actual)`. Goldens live under
`codegen/processor-test/src/test/resources/golden/<variant>/<file>.kt`. Nine variants:

- `simple/` for `@NavDestination(kind = SCREEN)` with plain `@Inject`.
- `parameterized/` for `@NavDestination(kind = SCREEN)` with `@AssistedInject`.
- `tab/` for `@NavDestination(kind = TAB_ROOT)`.
- `screen-ui/` for `@ScreenUi`.
- `sheet-ui/` for `@SheetUi`.
- `tab-ui/` for `@TabUi`.
- `child-presenter/` for `@ChildPresenter`.
- `app-root/` for `@AppRoot`.
- `app-root-ui/` for `@AppRootUi`.

Test coverage is grouped by annotation, not by variant. `NavDestinationTest` covers all three `@NavDestination` kinds (SCREEN, OVERLAY, TAB_ROOT) plus the parameterized
SCREEN variant. `ScreenUiTest` covers `@ScreenUi`. `SheetUiTest` covers `@SheetUi`. `TabUiTest` covers `@TabUi`. `ChildPresenterTest` covers `@ChildPresenter`. `AppRootTest`
covers `@AppRoot` plus three error paths (missing `@AssistedInject`, missing nested factory, missing bound interface). `AppRootUiTest` covers `@AppRootUi` plus two error
paths (no non-modifier parameter, presenter type mismatch). `ErrorPathTest` exercises the navigation-side parser validation branches and asserts on the compilation
messages rather than against a golden file.

`GoldenFileAssert` normalises both expected and actual by trimming trailing whitespace per line and trimming the file as a whole before comparing, so trailing newline
drift does not cause flakes.

## Updating goldens

Set `golden.update=true` (system property) or `GOLDEN_UPDATE=true` (environment variable) and re run the suite. `GoldenFileAssert` writes the actual output back to the
golden file instead of failing.

The repo wraps this in the `/update-golden` skill. It sets the property, runs the suite, and surfaces the diff so the change is reviewable before commit. Always read the
diff. Goldens are the contract a contributor is committing to; an unreviewed bulk update masks regressions.

## Adding a fixture

Adding a new test fixture is a three-step workflow.

1. Add a new test class under `codegen/processor-test/src/test/kotlin/io/github/thomaskioko/codegen/processor/`. Build the input as a `Map<String, String>` of source
   files (typically `TestStubs.baseStubs` plus the feature source), run it through `ProcessorTestRunner`, assert that the expected files exist, and call
   `GoldenFileAssert.assertMatches` on each.
2. Run the suite with `golden.update=true` to seed the golden directory.
3. Read the generated files. If they look right, commit them. If they do not, fix the generator (or the parser) and re run.

Do not commit a golden you have not read.
