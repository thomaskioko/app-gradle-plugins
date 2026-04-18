package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks a `@Composable` sheet function as the Android-side renderer for a modal sheet presenter.
 *
 * The navigation codegen processor reads this annotation and generates, in the same `ui` module's
 * `di/` package, a single binding:
 *
 * - `<FunctionName>UiBinding` — a `@BindingContainer @ContributesTo(parentScope)` object that
 *   `@Provides @IntoSet` a consumer-project `SheetContent` instance. The instance's `matches`
 *   lambda tests whether the active `SheetChild` is a `SheetDestination<*>` wrapping the declared
 *   [presenter] type, and its `content` lambda invokes the annotated composable with the cast
 *   presenter.
 *
 * The annotated function must match the signature
 * `@Composable fun <Name>(presenter: <PresenterType>, modifier: Modifier = Modifier)`; only the
 * `presenter` argument is forwarded by the generated code, since `SheetContent.content` does not
 * receive a `Modifier`.
 *
 * @property presenter the presenter type this sheet renders. Used to build the `matches` predicate
 *                     and to cast the active child's presenter before invoking the composable.
 * @property parentScope the parent DI scope hosting the generated binding. Typically
 *                       `ActivityScope::class` in the consumer project.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class SheetUi(
    val presenter: KClass<*>,
    val parentScope: KClass<*>,
)
