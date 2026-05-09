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

/**
 * Generates the Metro `@GraphExtension` interface plus its nested `Factory` for one annotated
 * presenter.
 *
 * Screens and tab roots produce the same graph structure, because the route always doubles as
 * the scope marker. One generator therefore covers both `ScreenData` and `TabData`.
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
 * read off the [NavData] rather than re derived here. See
 * [io.github.thomaskioko.codegen.processor.data.NavData].
 */
internal object ScreenGraphGenerator {
    /**
     * Generates the graph file for one parsed presenter annotation.
     *
     * @param data The parsed annotation, which carries every name and scope the generator needs.
     * @return The generated graph file as a KotlinPoet [FileSpec].
     */
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
