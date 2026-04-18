package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import io.github.thomaskioko.codegen.processor.data.SheetData
import io.github.thomaskioko.codegen.processor.util.ComponentContext
import io.github.thomaskioko.codegen.processor.util.IntoSet
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.SheetChild
import io.github.thomaskioko.codegen.processor.util.SheetChildFactory
import io.github.thomaskioko.codegen.processor.util.SheetConfig
import io.github.thomaskioko.codegen.processor.util.SheetConfigBinding
import io.github.thomaskioko.codegen.processor.util.SheetDestination
import io.github.thomaskioko.codegen.processor.util.parameterizedByStar

internal object SheetDestinationBindingGenerator {
    fun generate(data: SheetData): FileSpec = contributingBindingFile(
        bindingName = data.bindingClassName,
        parentScope = data.parentScope,
        childFactoryFun(data),
        configBindingFun(data),
    )

    private fun childFactoryFun(data: SheetData): FunSpec {
        val createCallArgs = CodeBlock.builder().apply {
            data.assistedMappings.forEachIndexed { index, mapping ->
                if (index > 0) add(", ")
                add("sheetConfig.%L", mapping.configProperty)
            }
        }.build()

        return FunSpec.builder("provide${data.baseName}ChildFactory")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .addParameter("graphFactory", data.graphFactoryClassName)
            .returns(SheetChildFactory)
            .addCode(
                CodeBlock.builder()
                    .add("return object : %T {\n", SheetChildFactory)
                    .indent()
                    .add(
                        "override fun matches(config: %T): Boolean = config is %T\n\n",
                        SheetConfig,
                        data.scope,
                    )
                    .add("override fun createChild(\n")
                    .indent()
                    .add("config: %T,\n", SheetConfig)
                    .add("componentContext: %T,\n", ComponentContext)
                    .unindent()
                    .add("): %T {\n", SheetChild)
                    .indent()
                    .add("val sheetConfig = config as %T\n", data.scope)
                    .add("return %T(\n", SheetDestination)
                    .indent()
                    .add("presenter = graphFactory.%L(componentContext)\n", data.graphFactoryFunName)
                    .add(".%L.create(%L),\n", data.graphPropertyName, createCallArgs)
                    .unindent()
                    .add(")\n")
                    .unindent()
                    .add("}\n")
                    .unindent()
                    .add("}\n")
                    .build(),
            )
            .build()
    }

    private fun configBindingFun(data: SheetData): FunSpec =
        FunSpec.builder("provide${data.baseName}ConfigBinding")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .returns(SheetConfigBinding.parameterizedByStar())
            .addStatement(
                "return %T(%T::class, %T.serializer())",
                SheetConfigBinding,
                data.scope,
                data.scope,
            )
            .build()
}
