package io.github.thomaskioko.codegen.processor.data

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

/**
 * Whether a [UiBindingData] models a screen renderer or an overlay renderer.
 *
 * Screen renderers pair with `@ScreenUi` annotated composables and contribute a `ScreenContent`
 * to the consumer's screen multibinding. They receive a `Modifier` from the caller. Overlay
 * renderers pair with `@SheetUi` annotated composables and contribute a `SheetContent` to the
 * consumer's overlay multibinding. They do not receive a `Modifier` because an overlay decides
 * its own modal layout in the composable body.
 */
internal enum class UiBindingKind {
    /** Renderer for a screen presenter. Pairs with `@ScreenUi`. The generated binding forwards `Modifier`. */
    Screen,

    /** Renderer for an overlay presenter. Pairs with `@SheetUi`. The generated binding does not forward `Modifier`. */
    Sheet,

    /** Renderer for a tab-root presenter. Pairs with `@TabUi`. The generated binding downcasts to `TabChild` and forwards `Modifier`. */
    Tab,
}

/**
 * Structured representation of a `@ScreenUi` or `@SheetUi` annotated composable. Produced by
 * [io.github.thomaskioko.codegen.processor.parser.parseUiBindingData] and consumed by
 * [io.github.thomaskioko.codegen.processor.codegen.UiBindingGenerator].
 *
 * The generator emits a `@BindingContainer @ContributesTo(parentScope) object` named
 * `${functionName}UiBinding` containing one `@Provides @IntoSet` function that returns the
 * matching `ScreenContent` or `SheetContent` instance.
 *
 * @property kind Whether this is a screen renderer or an overlay renderer.
 * @property composableFunction Reference to the annotated function. Held as a [MemberName] rather
 *   than a [ClassName] because the generated code calls the composable as a top level function
 *   through KotlinPoet's `%M` interpolation. Carrying it pre formed avoids re deriving it inside
 *   the generator.
 * @property presenterClass The presenter type the composable renders. Used by the generated
 *   `matches` predicate and by the cast inside the `content` lambda.
 * @property packageName The composable's package. The generated binding lands in `$packageName.di`.
 * @property parentScope The parent dependency injection scope the binding contributes to (typically `ActivityScope`).
 */
internal data class UiBindingData(
    val kind: UiBindingKind,
    val composableFunction: MemberName,
    val presenterClass: ClassName,
    val packageName: String,
    val parentScope: ClassName,
) {
    val functionName: String get() = composableFunction.simpleName
    val bindingClassName: ClassName = ClassName("$packageName.di", "${functionName}UiBinding")
    val provideFunName: String = "provide${functionName}Content"
}
