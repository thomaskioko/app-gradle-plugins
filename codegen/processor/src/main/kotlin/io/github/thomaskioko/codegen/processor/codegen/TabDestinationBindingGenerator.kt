package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import io.github.thomaskioko.codegen.processor.data.TabData
import io.github.thomaskioko.codegen.processor.util.ComponentContext
import io.github.thomaskioko.codegen.processor.util.IntoSet
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.TabChild
import io.github.thomaskioko.codegen.processor.util.TabDestination

internal object TabDestinationBindingGenerator {
    fun generate(data: TabData): FileSpec = contributingBindingFile(
        bindingName = data.bindingClassName,
        parentScope = data.parentScope,
        destinationFun(data),
    )

    private fun destinationFun(data: TabData): FunSpec =
        FunSpec.builder("provide${data.baseName}TabDestination")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .addParameter("graphFactory", data.graphFactoryClassName)
            .returns(TabDestination)
            .addCode(
                CodeBlock.builder()
                    .add("return object : %T {\n", TabDestination)
                    .indent()
                    .add("override fun matches(config: %T): Boolean =\n", data.configEnclosing)
                    .indent()
                    .add("config is %T\n\n", data.scope)
                    .unindent()
                    .add("override fun createChild(\n")
                    .indent()
                    .add("config: %T,\n", data.configEnclosing)
                    .add("componentContext: %T,\n", ComponentContext)
                    .unindent()
                    .add("): %T = %T(\n", TabChild.parameterizedBy(STAR), TabChild)
                    .indent()
                    .add(
                        "presenter = graphFactory.%L(componentContext).%L,\n",
                        data.graphFactoryFunName,
                        data.graphPropertyName,
                    )
                    .unindent()
                    .add(")\n")
                    .unindent()
                    .add("}\n")
                    .build(),
            )
            .build()
}
