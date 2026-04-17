package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.thomaskioko.codegen.processor.data.NavData
import io.github.thomaskioko.codegen.processor.util.ComponentContext
import io.github.thomaskioko.codegen.processor.util.FOUR_SPACE_INDENT
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.contributesTo
import io.github.thomaskioko.codegen.processor.util.graphExtension
import io.github.thomaskioko.codegen.processor.util.graphExtensionFactory

internal object ScreenGraphGenerator {
    fun generate(data: NavData): FileSpec {
        val factoryInterface = TypeSpec.interfaceBuilder("Factory")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(contributesTo(data.parentScope))
            .addAnnotation(graphExtensionFactory())
            .addFunction(
                FunSpec.builder(data.graphFactoryFunName)
                    .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT)
                    .addParameter(
                        ParameterSpec.builder("componentContext", ComponentContext)
                            .addAnnotation(Provides)
                            .build(),
                    )
                    .returns(data.graphClassName)
                    .build(),
            )
            .build()

        val graphInterface = TypeSpec.interfaceBuilder(data.graphClassName)
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(graphExtension(data.scope))
            .addProperty(
                PropertySpec.builder(data.graphPropertyName, data.graphPropertyType)
                    .addModifiers(KModifier.PUBLIC)
                    .build(),
            )
            .addType(factoryInterface)
            .build()

        return FileSpec.builder(data.graphClassName)
            .indent(FOUR_SPACE_INDENT)
            .addType(graphInterface)
            .build()
    }
}
