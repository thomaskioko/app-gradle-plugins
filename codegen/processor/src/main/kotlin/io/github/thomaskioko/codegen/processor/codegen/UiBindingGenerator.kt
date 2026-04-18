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
 * Emits `@BindingContainer @ContributesTo(parentScope) object ${FunctionName}UiBinding` that
 * contributes a `ScreenContent` (or `SheetContent`, depending on [UiBindingData.kind]) into the
 * activity-scope multibinding. The `matches` predicate tests for a `ScreenDestination<*>` /
 * `SheetDestination<*>` whose presenter is an instance of the declared type, and the `content`
 * lambda invokes the annotated composable with the cast presenter (plus a `Modifier` for screens).
 */
internal object UiBindingGenerator {

    fun generate(data: UiBindingData): FileSpec {
        val shape = shapeFor(data.kind)

        val provideFun = FunSpec.builder(data.provideFunName)
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .returns(shape.contentType)
            .addCode(buildProvideBody(data, shape))
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

    private fun buildProvideBody(data: UiBindingData, shape: Shape): CodeBlock {
        val builder = CodeBlock.builder()
            .add("return %T(\n", shape.contentType)
            .indent()
            .add(
                "matches = { (it as? %T<*>)?.presenter is %T },\n",
                shape.destinationType,
                data.presenterClass,
            )
        if (shape.forwardsModifier) {
            builder.add("content = { child, modifier ->\n")
        } else {
            builder.add("content = { child ->\n")
        }
        builder.indent()
            .add("%M(\n", data.composableFunction)
            .indent()
            .add(
                "presenter = (child as %T<*>).presenter as %T,\n",
                shape.destinationType,
                data.presenterClass,
            )
        if (shape.forwardsModifier) {
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

    private data class Shape(
        val contentType: ClassName,
        val destinationType: ClassName,
        val forwardsModifier: Boolean,
    )

    private fun shapeFor(kind: UiBindingKind): Shape = when (kind) {
        UiBindingKind.Screen -> Shape(ScreenContent, ScreenDestination, forwardsModifier = true)
        UiBindingKind.Sheet -> Shape(SheetContent, SheetDestination, forwardsModifier = false)
    }
}
