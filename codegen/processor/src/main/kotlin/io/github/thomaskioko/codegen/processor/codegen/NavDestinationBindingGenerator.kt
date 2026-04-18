package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import io.github.thomaskioko.codegen.processor.data.ScreenData
import io.github.thomaskioko.codegen.processor.util.ComponentContext
import io.github.thomaskioko.codegen.processor.util.IntoSet
import io.github.thomaskioko.codegen.processor.util.NavDestination
import io.github.thomaskioko.codegen.processor.util.NavRoute
import io.github.thomaskioko.codegen.processor.util.NavRouteBinding
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.RootChild
import io.github.thomaskioko.codegen.processor.util.ScreenDestination
import io.github.thomaskioko.codegen.processor.util.parameterizedByStar

internal object NavDestinationBindingGenerator {
    fun generate(data: ScreenData): FileSpec = contributingBindingFile(
        bindingName = data.bindingClassName,
        parentScope = data.parentScope,
        destinationFun(data),
        routeBindingFun(data.baseName, data.route),
    )

    private fun destinationFun(data: ScreenData): FunSpec =
        FunSpec.builder("provide${data.baseName}NavDestination")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .addParameter("graphFactory", data.graphFactoryClassName)
            .returns(NavDestination)
            .addCode(destinationBody(data))
            .build()

    private fun destinationBody(data: ScreenData): CodeBlock {
        val builder = CodeBlock.builder()
            .add("return object : %T {\n", NavDestination)
            .indent()
            .add("override fun matches(route: %T): Boolean = route is %T\n\n", NavRoute, data.route)
            .add("override fun createChild(\n")
            .indent()
            .add("route: %T,\n", NavRoute)
            .add("componentContext: %T,\n", ComponentContext)
            .unindent()
        if (data.isParameterized) {
            val routeLocal = data.route.simpleName.replaceFirstChar { it.lowercaseChar() }
            builder
                .add("): %T {\n", RootChild)
                .indent()
                .add("val %L = route as %T\n", routeLocal, data.route)
                .add("val graph = graphFactory.%L(componentContext)\n", data.graphFactoryFunName)
                .add(
                    "return %T(graph.%L.create(%L.%L))\n",
                    ScreenDestination,
                    data.graphPropertyName,
                    routeLocal,
                    data.routeProperty,
                )
                .unindent()
                .add("}\n")
        } else {
            builder
                .add("): %T = %T(\n", RootChild, ScreenDestination)
                .indent()
                .add(
                    "presenter = graphFactory.%L(componentContext).%L,\n",
                    data.graphFactoryFunName,
                    data.graphPropertyName,
                )
                .unindent()
                .add(")\n")
        }
        builder.unindent().add("}\n")
        return builder.build()
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
