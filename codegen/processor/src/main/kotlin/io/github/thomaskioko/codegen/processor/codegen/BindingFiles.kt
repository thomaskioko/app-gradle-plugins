package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import io.github.thomaskioko.codegen.processor.util.FOUR_SPACE_INDENT
import io.github.thomaskioko.codegen.processor.util.contributesTo

/**
 * Builds a `@ContributesTo(parentScope) public interface [bindingName] { public companion object { ... } }`
 * file with the given [providers] in the companion. Used by every destination-binding generator
 * (`NavDestinationBindingGenerator`, `SheetDestinationBindingGenerator`,
 * `TabDestinationBindingGenerator`) so they don't each spell the scaffold out by hand.
 */
internal fun contributingBindingFile(
    bindingName: ClassName,
    parentScope: ClassName,
    vararg providers: FunSpec,
): FileSpec {
    val companion = TypeSpec.companionObjectBuilder()
        .addModifiers(KModifier.PUBLIC)
        .also { builder -> providers.forEach { builder.addFunction(it) } }
        .build()

    val bindingInterface = TypeSpec.interfaceBuilder(bindingName)
        .addModifiers(KModifier.PUBLIC)
        .addAnnotation(contributesTo(parentScope))
        .addType(companion)
        .build()

    return FileSpec.builder(bindingName)
        .indent(FOUR_SPACE_INDENT)
        .addType(bindingInterface)
        .build()
}
