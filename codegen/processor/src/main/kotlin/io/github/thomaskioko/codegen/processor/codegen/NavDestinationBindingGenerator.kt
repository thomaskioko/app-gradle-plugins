package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import io.github.thomaskioko.codegen.processor.data.ScreenData
import io.github.thomaskioko.codegen.processor.data.ScreenKind
import io.github.thomaskioko.codegen.processor.util.ComponentContext
import io.github.thomaskioko.codegen.processor.util.IntoSet
import io.github.thomaskioko.codegen.processor.util.NavDestination
import io.github.thomaskioko.codegen.processor.util.NavDestinationOverlay
import io.github.thomaskioko.codegen.processor.util.NavDestinationScreen
import io.github.thomaskioko.codegen.processor.util.NavRouteBinding
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.ScreenDestination
import io.github.thomaskioko.codegen.processor.util.parameterizedByStar

/**
 * Generates the binding file that contributes one screen or overlay destination to the consumer's
 * navigation multibindings.
 *
 * The output is one [FileSpec] containing a `@ContributesTo(parentScope) interface` with a
 * companion object that holds two `@Provides @IntoSet` functions:
 *
 * - one returning a `NavDestination<*>` (specifically a `NavDestination.Screen` or
 *   `NavDestination.Overlay`, picked from [ScreenData.kind]);
 * - one returning a `NavRouteBinding<*>` so the route participates in polymorphic save and
 *   restore.
 *
 * ## Output for a presenter with no runtime parameters
 *
 * For `ShowsPresenter` annotated `@Inject @NavDestination(route = ShowsRoute::class, ..., kind = SCREEN)`:
 *
 * ```kotlin
 * @ContributesTo(ActivityScope::class)
 * public interface ShowsNavDestinationBinding {
 *     public companion object {
 *         @Provides
 *         @IntoSet
 *         public fun provideShowsNavDestination(graphFactory: ShowsScreenGraph.Factory): NavDestination<*> = NavDestination.Screen(
 *             routeClass = ShowsRoute::class,
 *         ) { _, componentContext ->
 *             ScreenDestination(graphFactory.createShowsGraph(componentContext).showsPresenter)
 *         }
 *
 *         @Provides
 *         @IntoSet
 *         public fun provideShowsRouteBinding(): NavRouteBinding<*> =
 *             NavRouteBinding(ShowsRoute::class, ShowsRoute.serializer())
 *     }
 * }
 * ```
 *
 * ## Output for a parameterized presenter
 *
 * When [ScreenData.isParameterized] is `true`, the generated factory lambda casts the route to
 * the route class, reads the property whose name is recorded in [ScreenData.routeProperty], and
 * passes it through the assisted factory recorded in [ScreenData.factory]:
 *
 * ```kotlin
 * @Provides
 * @IntoSet
 * public fun provideShowDetailsNavDestination(graphFactory: ShowDetailsScreenGraph.Factory): NavDestination<*> = NavDestination.Screen(
 *     routeClass = ShowDetailsRoute::class,
 * ) { showDetailsRoute, componentContext ->
 *     val graph = graphFactory.createShowDetailsGraph(componentContext)
 *     ScreenDestination(graph.showDetailsFactory.create(showDetailsRoute.param))
 * }
 * ```
 *
 * The choice between the two output forms lives in [destinationBody] and is the only place the
 * generated code branches based on whether the presenter accepts a runtime parameter from the
 * route.
 */
internal object NavDestinationBindingGenerator {
    /**
     * Generates the binding file for one screen or overlay presenter annotation.
     *
     * @param data The parsed annotation, which carries every name, scope, and parameterization
     *   detail the generator needs.
     * @return The generated binding file as a KotlinPoet [FileSpec].
     */
    fun generate(data: ScreenData): FileSpec = contributingBindingFile(
        bindingName = data.bindingClassName,
        parentScope = data.parentScope,
        destinationFun(data),
        routeBindingFun(data.baseName, data.route),
    )

    /**
     * Builds the `provide<BaseName>NavDestination` function. The function returns
     * `NavDestination<*>` and its body is built by [destinationBody]. The body picks between the
     * two output forms based on [ScreenData.isParameterized].
     */
    private fun destinationFun(data: ScreenData): FunSpec {
        val subclass = when (data.kind) {
            ScreenKind.SCREEN -> NavDestinationScreen
            ScreenKind.OVERLAY -> NavDestinationOverlay
        }
        return FunSpec.builder("provide${data.baseName}NavDestination")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .addParameter("graphFactory", data.graphFactoryClassName)
            .returns(NavDestination.parameterizedByStar())
            .addCode(destinationBody(data, subclass))
            .build()
    }

    /**
     * Generates the factory lambda that the destination uses to instantiate the presenter at
     * navigation time.
     *
     * For a presenter with no runtime parameters, the lambda ignores the incoming route, calls
     * the graph factory to obtain the graph, reads the presenter property from the graph, and
     * wraps it in `ScreenDestination(...)`.
     *
     * For a parameterized presenter, the lambda casts the incoming route to the declared route
     * type, reads the assisted factory from the graph, calls
     * `factory.create(route.<routeProperty>)`, and wraps the resulting presenter in
     * `ScreenDestination(...)`.
     *
     * @param data The parsed annotation. The factory lambda's structure depends on
     *   [ScreenData.isParameterized].
     * @param subclass The `NavDestination` subclass to return (`NavDestination.Screen` or
     *   `NavDestination.Overlay`).
     */
    private fun destinationBody(data: ScreenData, subclass: ClassName): CodeBlock {
        val routeLocal = data.route.simpleName.replaceFirstChar { it.lowercaseChar() }
        return if (data.isParameterized) {
            CodeBlock.builder()
                .add("return %T(\n", subclass)
                .indent()
                .add("routeClass = %T::class,\n", data.route)
                .add(") { %L, componentContext ->\n", routeLocal)
                .indent()
                .add("val graph = graphFactory.%L(componentContext)\n", data.graphFactoryFunName)
                .add(
                    "%T(graph.%L.create(%L.%L))\n",
                    ScreenDestination,
                    data.graphPropertyName,
                    routeLocal,
                    data.routeProperty,
                )
                .unindent()
                .add("}\n")
                .build()
        } else {
            CodeBlock.builder()
                .add("return %T(\n", subclass)
                .indent()
                .add("routeClass = %T::class,\n", data.route)
                .add(") { _, componentContext ->\n")
                .indent()
                .add(
                    "%T(graphFactory.%L(componentContext).%L)\n",
                    ScreenDestination,
                    data.graphFactoryFunName,
                    data.graphPropertyName,
                )
                .unindent()
                .add("}\n")
                .build()
        }
    }

    /**
     * Builds the `provide<BaseName>RouteBinding` function. Returns `NavRouteBinding<*>` whose
     * body is `NavRouteBinding(<Route>::class, <Route>.serializer())`. This entry feeds the
     * polymorphic save and restore step that the consumer's navigation state container performs
     * when the process is killed and later restored.
     *
     * @param baseName The presenter's base name (with `Presenter` suffix stripped). Used to name
     *   the generated function.
     * @param route The route class. Used as both the type argument to `NavRouteBinding` and the
     *   type whose `.serializer()` companion function is called.
     */
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
