package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks a `@Composable` function as the Android renderer for a screen presenter, so the codegen
 * processor can wire it into the consumer's UI multibinding at compile time.
 *
 * Without this annotation the consumer would have to manually write a Metro `@BindingContainer`
 * that `@Provides @IntoSet` a `ScreenContent`, with a `matches` predicate that downcasts the
 * active navigation child and a `content` lambda that invokes the composable. With this annotation
 * the processor emits all of that next to the composable.
 *
 * ## Example
 *
 * ```kotlin
 * @Composable
 * @ScreenUi(presenter = ShowsPresenter::class, parentScope = ActivityScope::class)
 * fun ShowsScreen(
 *     presenter: ShowsPresenter,
 *     modifier: Modifier = Modifier,
 * ) {
 *     // ... compose UI here
 * }
 * ```
 *
 * The processor emits one file (`ShowsScreenUiBinding.kt`) into the same module's `<package>.di`
 * sub package. The file contains a `@BindingContainer @ContributesTo(parentScope) object` whose
 * single `@Provides @IntoSet` function returns a `ScreenContent` that:
 *
 * - tests whether the active navigation child is a `ScreenDestination<*>` whose `presenter` is an
 *   instance of [presenter];
 * - invokes the annotated composable with the cast presenter and the incoming `Modifier`.
 *
 * ## Composable signature requirement
 *
 * The annotated function must match the signature
 * `@Composable fun <Name>(presenter: <PresenterType>, modifier: Modifier = Modifier)`. The
 * processor does not enforce the parameter names or default expression at parse time, but the
 * generated code calls the composable with `presenter = ..., modifier = modifier`. A function
 * whose parameters are out of order or named differently will fail to compile after generation,
 * not during processing.
 *
 * ## When to use it
 *
 * Pair this with `@NavDestination(kind = SCREEN)` on the matching presenter. The pair lives in
 * two modules: the presenter (Kotlin Multiplatform, with `@NavDestination`) and the composable
 * (Android only, with `@ScreenUi`). The processor emits one binding for each so they wire
 * together at runtime through Metro multibindings.
 *
 * @property presenter The presenter type this screen renders. Used to build the `matches`
 *   predicate and to cast the active child's presenter before invoking the composable.
 * @property parentScope The parent dependency injection scope hosting the generated binding.
 *   Typically `ActivityScope::class` in the consumer project.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class ScreenUi(
    val presenter: KClass<*>,
    val parentScope: KClass<*>,
)
