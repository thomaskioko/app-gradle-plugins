package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks a `@Composable` function as the Android renderer for a modal overlay presenter, so the
 * codegen processor can wire it into the consumer's overlay multibinding at compile time.
 *
 * This is the overlay equivalent of [ScreenUi]. The processor emits a binding with the same
 * structure but contributes a `SheetContent` instead of a `ScreenContent`, so the consumer's
 * overlay slot can pick it up.
 *
 * ## Example
 *
 * ```kotlin
 * @Composable
 * @SheetUi(presenter = EpisodeSheetPresenter::class, parentScope = ActivityScope::class)
 * fun EpisodeSheet(
 *     presenter: EpisodeSheetPresenter,
 *     modifier: Modifier = Modifier,
 * ) {
 *     // ... compose your ModalBottomSheet here
 * }
 * ```
 *
 * The processor emits one file (`EpisodeSheetUiBinding.kt`) into the same module's `<package>.di`
 * sub package. The file contains a `@BindingContainer @ContributesTo(parentScope) object` whose
 * single `@Provides @IntoSet` function returns a `SheetContent` that:
 *
 * - tests whether the active overlay child is a `SheetDestination<*>` whose `presenter` is an
 *   instance of [presenter];
 * - invokes the annotated composable with the cast presenter (no `Modifier`).
 *
 * ## Composable signature requirement
 *
 * The annotated function must match the signature
 * `@Composable fun <Name>(presenter: <PresenterType>, modifier: Modifier = Modifier)`. Only the
 * `presenter` argument is forwarded by the generated code. The generated wrapper does not pass a
 * modifier because `SheetContent.content` is typed as `(SheetChild) -> Unit`. A modal overlay
 * decides its own layout (typically inside a `ModalBottomSheet`) in the composable body, not at
 * the call site. The composable's `modifier` parameter still exists so the function can be called
 * directly from preview code.
 *
 * ## When to use it
 *
 * Pair this with `@NavDestination(kind = OVERLAY)` on the matching presenter. The presenter lives
 * in the shared Kotlin Multiplatform layer; the composable lives in an Android only `ui` module.
 * The pair wires together through Metro multibindings at runtime.
 *
 * @property presenter The presenter type this overlay renders. Used to build the `matches`
 *   predicate and to cast the active child's presenter before invoking the composable.
 * @property parentScope The parent dependency injection scope hosting the generated binding.
 *   Typically `ActivityScope::class` in the consumer project.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class SheetUi(
    val presenter: KClass<*>,
    val parentScope: KClass<*>,
)
