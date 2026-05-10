package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks a `@Composable` function as the application's root host composable, so the codegen
 * processor can emit a provider interface and an extension that the activity's dependency graph
 * implements.
 *
 * Without this annotation the activity has to invoke the root composable with one argument for
 * each shared dependency the composable accepts. With this annotation the processor reads the
 * composable's parameter list, emits an `AppRootProvider` interface declaring one `val` for each
 * non-modifier parameter, and emits a `@Composable AppRootProvider.AppRootContent(modifier)`
 * extension that invokes the annotated composable using the receiver's properties. The activity
 * call site collapses from one argument per dependency to one extension call.
 *
 * ## Example
 *
 * ```kotlin
 * @Composable
 * @AppRootUi(presenter = RootPresenter::class, parentScope = ActivityScope::class)
 * fun RootScreen(
 *     rootPresenter: RootPresenter,
 *     screenContents: Set<ScreenContent>,
 *     sheetContents: Set<SheetContent>,
 *     modifier: Modifier = Modifier,
 * ) {
 *     // ... compose UI here
 * }
 * ```
 *
 * The processor emits one file (`<FunctionName>AppRootUiBinding.kt`) into the same module's
 * `<package>.di` sub package. The file contains the `AppRootProvider` interface and the
 * `AppRootContent` extension. The consumer makes its activity-scope `@DependencyGraph` extend
 * `AppRootProvider`, then calls `graph.AppRootContent()` from the activity.
 *
 * ## Composable signature requirement
 *
 * The annotated function must:
 *
 * - Be `@Composable`.
 * - Declare at least one non-modifier parameter.
 * - Declare its first non-modifier parameter as the [presenter] type.
 *
 * The processor reads the parameter list in order and skips any parameter named `modifier` whose
 * type is `androidx.compose.ui.Modifier`. Every other parameter becomes a property on the
 * generated `AppRootProvider` interface.
 *
 * ## Validation
 *
 * The processor reports a compile error if any of the following hold:
 *
 * - The annotated symbol is not a function.
 * - The function lives in the default (empty) package.
 * - The function has no non-modifier parameters.
 * - The first non-modifier parameter type does not equal [presenter].
 * - More than one `@AppRootUi` is declared in the same compilation round.
 *
 * @property presenter The root presenter type. Must equal the first non-modifier parameter type
 *   on the annotated composable. Used for compile-time validation.
 * @property parentScope The dependency injection scope hosting the generated artifacts (typically
 *   `ActivityScope::class` in the consumer project).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class AppRootUi(
    val presenter: KClass<*>,
    val parentScope: KClass<*>,
)
