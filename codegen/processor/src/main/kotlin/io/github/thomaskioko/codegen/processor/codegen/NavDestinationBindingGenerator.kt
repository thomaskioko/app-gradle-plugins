package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import io.github.thomaskioko.codegen.processor.data.ParameterizedScreenData
import io.github.thomaskioko.codegen.processor.data.SimpleScreenData
import io.github.thomaskioko.codegen.processor.util.ComponentContext
import io.github.thomaskioko.codegen.processor.util.FOUR_SPACE_INDENT
import io.github.thomaskioko.codegen.processor.util.IntoSet
import io.github.thomaskioko.codegen.processor.util.NavDestination
import io.github.thomaskioko.codegen.processor.util.NavRoute
import io.github.thomaskioko.codegen.processor.util.NavRouteBinding
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.RootChild
import io.github.thomaskioko.codegen.processor.util.ScreenDestination
import io.github.thomaskioko.codegen.processor.util.contributesTo
import io.github.thomaskioko.codegen.processor.util.parameterizedByStar

internal object NavDestinationBindingGenerator {
    fun generate(data: SimpleScreenData): FileSpec = buildBindingFile(
        bindingName = data.bindingClassName,
        parentScope = data.parentScope,
        provideDestination = simpleDestinationFun(data),
        provideRouteBinding = routeBindingFun(data.baseName, data.route),
    )

    fun generate(data: ParameterizedScreenData): FileSpec = buildBindingFile(
        bindingName = data.bindingClassName,
        parentScope = data.parentScope,
        provideDestination = parameterizedDestinationFun(data),
        provideRouteBinding = routeBindingFun(data.baseName, data.route),
    )

    private fun buildBindingFile(
        bindingName: ClassName,
        parentScope: ClassName,
        provideDestination: FunSpec,
        provideRouteBinding: FunSpec,
    ): FileSpec {
        val companion = TypeSpec.companionObjectBuilder()
            .addModifiers(KModifier.PUBLIC)
            .addFunction(provideDestination)
            .addFunction(provideRouteBinding)
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

    private fun simpleDestinationFun(data: SimpleScreenData): FunSpec =
        FunSpec.builder("provide${data.baseName}NavDestination")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .addParameter("graphFactory", data.graphFactoryClassName)
            .returns(NavDestination)
            .addCode(
                CodeBlock.builder()
                    .add("return object : %T {\n", NavDestination)
                    .indent()
                    .add("override fun matches(route: %T): Boolean = route is %T\n\n", NavRoute, data.route)
                    .add("override fun createChild(\n")
                    .indent()
                    .add("route: %T,\n", NavRoute)
                    .add("componentContext: %T,\n", ComponentContext)
                    .unindent()
                    .add("): %T = %T(\n", RootChild, ScreenDestination)
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

    private fun parameterizedDestinationFun(data: ParameterizedScreenData): FunSpec {
        val routeLocalName = data.route.simpleName.replaceFirstChar { it.lowercaseChar() }
        return FunSpec.builder("provide${data.baseName}NavDestination")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .addParameter("graphFactory", data.graphFactoryClassName)
            .returns(NavDestination)
            .addCode(
                CodeBlock.builder()
                    .add("return object : %T {\n", NavDestination)
                    .indent()
                    .add("override fun matches(route: %T): Boolean = route is %T\n\n", NavRoute, data.route)
                    .add("override fun createChild(\n")
                    .indent()
                    .add("route: %T,\n", NavRoute)
                    .add("componentContext: %T,\n", ComponentContext)
                    .unindent()
                    .add("): %T {\n", RootChild)
                    .indent()
                    .add("val %L = route as %T\n", routeLocalName, data.route)
                    .add("val graph = graphFactory.%L(componentContext)\n", data.graphFactoryFunName)
                    .add(
                        "return %T(graph.%L.create(%L.%L))\n",
                        ScreenDestination,
                        data.graphPropertyName,
                        routeLocalName,
                        data.routeProperty,
                    )
                    .unindent()
                    .add("}\n")
                    .unindent()
                    .add("}\n")
                    .build(),
            )
            .build()
    }

    private fun routeBindingFun(baseName: String, route: ClassName): FunSpec =
        FunSpec.builder("provide${baseName}RouteBinding")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .returns(NavRouteBinding.parameterizedByStar())
            .addStatement(
                "return %T(%T::class, %T.serializer())",
                NavRouteBinding,
                route,
                route,
            )
            .build()
}
