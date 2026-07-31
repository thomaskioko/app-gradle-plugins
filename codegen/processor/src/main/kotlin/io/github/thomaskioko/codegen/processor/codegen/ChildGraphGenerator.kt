/*
 * Copyright 2026 Thomas Kioko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import io.github.thomaskioko.codegen.processor.util.hideFromObjC
import io.github.thomaskioko.codegen.processor.util.optInObjCRefinement

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
     * @param hideFromObjC Whether to hide the generated types from the Objective-C API. True when
     *   the compilation has a non-JVM target.
     * @return The generated graph file as a KotlinPoet [FileSpec].
     */
    fun generate(data: ChildPresenterData, hideFromObjC: Boolean): FileSpec {
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
            .hideFromObjC(hideFromObjC)
            .addAnnotation(contributesTo(data.parentScope))
            .addAnnotation(graphExtensionFactory())
            .addFunction(factoryFun)
            .build()

        val presenterProperty = PropertySpec.builder(data.graphPropertyName, data.graphPropertyType)
            .addModifiers(KModifier.PUBLIC)
            .build()

        val graphInterface = TypeSpec.interfaceBuilder(data.graphClassName)
            .addModifiers(KModifier.PUBLIC)
            .hideFromObjC(hideFromObjC)
            .addAnnotation(graphExtension(data.scope))
            .addProperty(presenterProperty)
            .addType(factoryInterface)
            .build()

        return FileSpec.builder(data.graphClassName)
            .indent(FOUR_SPACE_INDENT)
            .optInObjCRefinement(hideFromObjC)
            .addType(graphInterface)
            .build()
    }
}
