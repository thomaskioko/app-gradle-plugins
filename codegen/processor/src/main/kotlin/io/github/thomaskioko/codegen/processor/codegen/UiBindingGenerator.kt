package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import io.github.thomaskioko.codegen.processor.data.UiBindingData
import io.github.thomaskioko.codegen.processor.data.UiBindingKind
import io.github.thomaskioko.codegen.processor.util.FOUR_SPACE_INDENT
import io.github.thomaskioko.codegen.processor.util.IntoSet
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.ScreenContent
import io.github.thomaskioko.codegen.processor.util.ScreenDestination
import io.github.thomaskioko.codegen.processor.util.SheetContent
import io.github.thomaskioko.codegen.processor.util.SheetDestination
import io.github.thomaskioko.codegen.processor.util.bindingContainer
import io.github.thomaskioko.codegen.processor.util.contributesTo

/**
 * Generates the binding file for a `@ScreenUi` or `@SheetUi` annotated composable.
 *
 * The output is a `@BindingContainer @ContributesTo(parentScope) object ${FunctionName}UiBinding`.
 * The object contributes a `ScreenContent` (when [UiBindingData.kind] is [UiBindingKind.Screen])
 * or a `SheetContent` (when it is [UiBindingKind.Sheet]) into the activity scope multibinding.
 * The `matches` predicate tests for a `ScreenDestination<*>` or `SheetDestination<*>` whose
 * `presenter` is an instance of the declared type. The `content` lambda invokes the annotated
 * composable with the cast presenter, and for screens forwards the incoming `Modifier`.
 *
 * ## Why a `@BindingContainer object` rather than `interface + companion`
 *
 * The bindings the codegen emits for `@NavDestination` use Metro's `interface + companion object`
 * structure. UI bindings cannot. The Android-only `ui` modules where these bindings land do not
 * pick up `@Provides @IntoSet` declarations from a companion object the way the Kotlin
 * Multiplatform presenter modules do. Emitting that structure would silently produce an empty
 * multibinding at build time. Targeting `@BindingContainer object` is what makes the
 * contributions discoverable. The full reasoning lives in
 * [codegen/docs/architecture/generators.md](../../../../../../../../../docs/architecture/generators.md).
 */
internal object UiBindingGenerator {

    /**
     * Generates the binding file for one `@ScreenUi` or `@SheetUi` composable.
     *
     * @param data The parsed annotation, which carries the composable function reference, the
     *   presenter type, the package, and the parent scope.
     * @return The generated binding file as a KotlinPoet [FileSpec].
     */
    fun generate(data: UiBindingData): FileSpec {
        val variant = variantFor(data.kind)

        val provideFun = FunSpec.builder(data.provideFunName)
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .returns(variant.contentType)
            .addCode(buildProvideBody(data, variant))
            .build()

        val bindingObject = TypeSpec.objectBuilder(data.bindingClassName)
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(bindingContainer())
            .addAnnotation(contributesTo(data.parentScope))
            .addFunction(provideFun)
            .build()

        return FileSpec.builder(data.bindingClassName)
            .indent(FOUR_SPACE_INDENT)
            .addType(bindingObject)
            .build()
    }

    /**
     * Generates the body of the `@Provides @IntoSet` function inside the binding object.
     *
     * The body returns a `ScreenContent` or `SheetContent` literal. Its `matches` predicate
     * decides whether this renderer applies to the active navigation child, and its `content`
     * lambda invokes the annotated composable with the cast presenter. Screens additionally
     * forward the incoming `Modifier`; overlays do not, because `SheetContent.content` accepts
     * only the child.
     *
     * @param data The parsed annotation, which carries the composable function reference and the
     *   presenter type.
     * @param variant The kind-specific values ([Variant]) chosen by [variantFor].
     * @return A KotlinPoet [CodeBlock] that becomes the body of the `@Provides @IntoSet`
     *   function.
     */
    private fun buildProvideBody(data: UiBindingData, variant: Variant): CodeBlock {
        val builder = CodeBlock.builder()
            .add("return %T(\n", variant.contentType)
            .indent()
            .add(
                "matches = { (it as? %T<*>)?.presenter is %T },\n",
                variant.destinationType,
                data.presenterClass,
            )
        if (variant.forwardsModifier) {
            builder.add("content = { child, modifier ->\n")
        } else {
            builder.add("content = { child ->\n")
        }
        builder.indent()
            .add("%M(\n", data.composableFunction)
            .indent()
            .add(
                "presenter = (child as %T<*>).presenter as %T,\n",
                variant.destinationType,
                data.presenterClass,
            )
        if (variant.forwardsModifier) {
            builder.add("modifier = modifier,\n")
        }
        builder.unindent()
            .add(")\n")
            .unindent()
            .add("},\n")
            .unindent()
            .add(")\n")
        return builder.build()
    }

    /**
     * The three values that vary between `@ScreenUi` and `@SheetUi` output.
     *
     * @property contentType The multibinding entry type the generated function returns
     *   (`ScreenContent` or `SheetContent`).
     * @property destinationType The destination subclass the active navigation child is cast to
     *   (`ScreenDestination` or `SheetDestination`).
     * @property forwardsModifier Whether the generated `content` lambda accepts a `Modifier` and
     *   forwards it to the composable. `true` for screens, `false` for overlays.
     */
    private data class Variant(
        val contentType: ClassName,
        val destinationType: ClassName,
        val forwardsModifier: Boolean,
    )

    /**
     * Picks the [Variant] for the given renderer kind.
     *
     * `Screen` produces a `ScreenContent` paired with a `ScreenDestination` cast and forwards
     * `Modifier`. `Sheet` produces a `SheetContent` paired with a `SheetDestination` cast and
     * does not forward `Modifier`. The latter difference is forced by the consumer's
     * `SheetContent.content` signature, which accepts only the child.
     *
     * @param kind The renderer kind read from `@ScreenUi` or `@SheetUi`.
     * @return The [Variant] of values to use when generating the binding body.
     */
    private fun variantFor(kind: UiBindingKind): Variant = when (kind) {
        UiBindingKind.Screen -> Variant(ScreenContent, ScreenDestination, forwardsModifier = true)
        UiBindingKind.Sheet -> Variant(SheetContent, SheetDestination, forwardsModifier = false)
    }
}
