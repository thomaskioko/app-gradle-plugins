package io.github.thomaskioko.codegen.processor.data

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

/**
 * Distinguishes the two UI renderer multibindings: a stack-side `ScreenContent` vs a modal
 * `SheetContent`. Both share the same generated binding shape; the differing bits (content type,
 * destination cast target, whether a `Modifier` is forwarded) are looked up from this kind at
 * generation time.
 */
internal enum class UiBindingKind { Screen, Sheet }

/**
 * Source data for a `@ScreenUi` or `@SheetUi`-annotated composable.
 *
 * The processor reads the annotation, resolves the declared presenter type, and produces one of
 * these per match. [UiBindingGenerator] turns it into a
 * `@BindingContainer @ContributesTo(parentScope) object ${functionName}UiBinding` that registers
 * a `ScreenContent` (or `SheetContent`) into the activity-scope multibinding.
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
