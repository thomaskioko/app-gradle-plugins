package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.thomaskioko.codegen.processor.data.ChildPresenterData
import io.github.thomaskioko.codegen.processor.util.ComponentContext
import io.github.thomaskioko.codegen.processor.util.FOUR_SPACE_INDENT
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.contributesTo
import io.github.thomaskioko.codegen.processor.util.graphExtension
import io.github.thomaskioko.codegen.processor.util.graphExtensionFactory

/**
 * Generates the graph extension file for a `@ChildPresenter` annotated class.
 *
 * The output is a `@GraphExtension(scope) interface <Presenter>ChildGraph` exposing the
 * presenter (or its assisted factory for parameterized children) as a property and a
 * `@ContributesTo(parentScope) @GraphExtension.Factory` nested interface that takes a
 * `ComponentContext` and returns the graph.
 */
internal object ChildGraphGenerator {

    /**
     * Generates the graph extension file for one `@ChildPresenter` annotated class.
     *
     * @param data The parsed annotation, which carries the presenter class, the graph scope, and
     *   the parent scope.
     * @return The generated graph file as a KotlinPoet [FileSpec].
     */
    fun generate(data: ChildPresenterData): FileSpec {
        val factoryFun = FunSpec.builder(data.graphFactoryFunName)
            .addModifiers(KModifier.PUBLIC, KModifier.ABSTRACT)
            .addParameter(
                ParameterSpec.builder("componentContext", ComponentContext)
                    .addAnnotation(Provides)
                    .build(),
            )
            .returns(data.graphClassName)
            .build()

        val factoryInterface = TypeSpec.interfaceBuilder(data.graphClassName.nestedClass("Factory"))
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(contributesTo(data.parentScope))
            .addAnnotation(graphExtensionFactory())
            .addFunction(factoryFun)
            .build()

        val presenterProperty = PropertySpec.builder(data.graphPropertyName, data.graphPropertyType)
            .addModifiers(KModifier.PUBLIC)
            .build()

        val graphInterface = TypeSpec.interfaceBuilder(data.graphClassName)
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(graphExtension(data.scope))
            .addProperty(presenterProperty)
            .addType(factoryInterface)
            .build()

        return FileSpec.builder(data.graphClassName)
            .indent(FOUR_SPACE_INDENT)
            .addType(graphInterface)
            .build()
    }
}
