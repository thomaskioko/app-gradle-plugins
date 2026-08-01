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
import io.github.thomaskioko.codegen.processor.data.GraphData
import io.github.thomaskioko.codegen.processor.util.ComponentContext
import io.github.thomaskioko.codegen.processor.util.FOUR_SPACE_INDENT
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.contributesTo
import io.github.thomaskioko.codegen.processor.util.graphExtension
import io.github.thomaskioko.codegen.processor.util.graphExtensionFactory
import io.github.thomaskioko.codegen.processor.util.hideFromObjC
import io.github.thomaskioko.codegen.processor.util.optInObjCRefinement

/**
 * Generates the Metro `@GraphExtension` interface plus its nested `Factory` for one annotated
 * presenter.
 *
 * Screens, tab roots, and child presenters produce the same graph structure, because the route
 * always doubles as the scope marker. One generator therefore covers `ScreenData`, `TabData`, and
 * `ChildPresenterData`, which all supply the names through [GraphData].
 *
 * ## Output structure
 *
 * For a presenter `ShowsPresenter` with `route = ShowsRoute::class` and
 * `parentScope = ActivityScope::class`, this generator emits:
 *
 * ```kotlin
 * @GraphExtension(ShowsRoute::class)
 * public interface ShowsScreenGraph {
 *     public val showsPresenter: ShowsPresenter
 *
 *     @ContributesTo(ActivityScope::class)
 *     @GraphExtension.Factory
 *     public interface Factory {
 *         public fun createShowsGraph(@Provides componentContext: ComponentContext): ShowsScreenGraph
 *     }
 * }
 * ```
 *
 * The exposed property is the presenter for plain `@Inject` presenters and tabs, or the assisted
 * factory for parameterized presenters. All names (interface, property, factory function) are
 * read off the [GraphData] rather than re derived here. See
 * [io.github.thomaskioko.codegen.processor.data.GraphData].
 */
internal object ScreenGraphGenerator {
    /**
     * Generates the graph file for one parsed presenter annotation.
     *
     * @param data The parsed annotation, which carries every name and scope the generator needs.
     * @param hideFromObjC Whether to hide the generated types from the Objective-C API. True when
     *   the compilation has a non-JVM target.
     * @return The generated graph file as a KotlinPoet [FileSpec].
     */
    fun generate(data: GraphData, hideFromObjC: Boolean): FileSpec {
        val factoryInterface = TypeSpec.interfaceBuilder("Factory")
            .addModifiers(KModifier.PUBLIC)
            .hideFromObjC(hideFromObjC)
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
            .hideFromObjC(hideFromObjC)
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
            .optInObjCRefinement(hideFromObjC)
            .addType(graphInterface)
            .build()
    }
}
