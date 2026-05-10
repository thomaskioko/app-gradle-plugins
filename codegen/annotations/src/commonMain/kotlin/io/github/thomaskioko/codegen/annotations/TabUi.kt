package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks a `@Composable` function as the Android renderer for a tab-root presenter.
 *
 * Tab roots are wrapped in `TabChild<*>` rather than `ScreenDestination<*>` (per the codegen for
 * `@NavDestination(kind = TAB_ROOT)`), so the matcher emitted for `@ScreenUi` does not fire on
 * them. `@TabUi` emits a parallel renderer that downcasts to `TabChild` so the same
 * `Set<ScreenContent>` multibinding can dispatch tab roots and stack screens uniformly. The host
 * `Children` body becomes a single `firstOrNull { it.matches(child) }` lookup rather than a
 * hand-written `when` over tab presenter types.
 *
 * ## Example
 *
 * ```kotlin
 * @Composable
 * @TabUi(presenter = DiscoverShowsPresenter::class, parentScope = ActivityScope::class)
 * fun DiscoverScreen(
 *     presenter: DiscoverShowsPresenter,
 *     modifier: Modifier = Modifier,
 * ) {
 *     // ... compose UI here
 * }
 * ```
 *
 * The processor emits one file (`DiscoverScreenUiBinding.kt`) into the same module's `<package>.di`
 * sub-package. The file contains a `@BindingContainer @ContributesTo(parentScope) object` whose
 * single `@Provides @IntoSet` function returns a `ScreenContent` that:
 *
 * - tests whether the active navigation child is a `TabChild<*>` whose `presenter` is an instance
 *   of [presenter];
 * - invokes the annotated composable with the cast presenter and the incoming `Modifier`.
 *
 * ## Composable signature requirement
 *
 * The annotated function must match the signature
 * `@Composable fun <Name>(presenter: <PresenterType>, modifier: Modifier = Modifier)`. The
 * processor does not enforce parameter names at parse time, but the generated code calls the
 * composable with `presenter = ..., modifier = modifier`.
 *
 * ## When to use it
 *
 * Pair this with `@NavDestination(kind = TAB_ROOT)` on the matching tab presenter. The presenter
 * lives in the shared Kotlin Multiplatform layer; the composable lives in an Android-only `ui`
 * module. The processor emits one binding for each so they wire together at runtime through the
 * `Set<ScreenContent>` multibinding the navigation host iterates.
 *
 * @property presenter The tab presenter type this screen renders. Used to build the `matches`
 *   predicate and to cast the active child's presenter before invoking the composable.
 * @property parentScope The parent dependency injection scope hosting the generated binding.
 *   Typically `ActivityScope::class` in the consumer project.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class TabUi(
    val presenter: KClass<*>,
    val parentScope: KClass<*>,
)
