package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import io.github.thomaskioko.codegen.processor.data.TabData
import io.github.thomaskioko.codegen.processor.util.IntoSet
import io.github.thomaskioko.codegen.processor.util.NavDestination
import io.github.thomaskioko.codegen.processor.util.NavDestinationTabRoot
import io.github.thomaskioko.codegen.processor.util.NavRoot
import io.github.thomaskioko.codegen.processor.util.NavRootBinding
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.TabChild
import io.github.thomaskioko.codegen.processor.util.parameterizedByStar

/**
 * Generates the binding file for a presenter annotated with `@NavDestination(kind = TAB_ROOT)`.
 *
 * Mirrors [NavDestinationBindingGenerator] for tab roots, with two structural differences. The
 * destination function returns a `NavDestination.TabRoot` rather than a `NavDestination.Screen`,
 * and the factory lambda wraps the produced presenter in `TabChild(...)` rather than
 * `ScreenDestination(...)`. The route binding function returns `NavRootBinding<*>` instead of
 * `NavRouteBinding<*>`. A third function contributes the route singleton itself into
 * `Set<NavRoot>` so consumers do not have to hand-write a parallel binding next to each tab.
 *
 * There is no parameterized branch. Tab roots must use plain `@Inject`. The parser
 * ([io.github.thomaskioko.codegen.processor.parser.parseNavDestinationData]) rejects
 * `@AssistedInject` tab presenters with a compile error before the data ever reaches this
 * generator.
 *
 * ## Output structure
 *
 * For `DiscoverPresenter` annotated `@Inject @NavDestination(route = DiscoverRoot::class, ..., kind = TAB_ROOT)`:
 *
 * ```kotlin
 * @ContributesTo(ActivityScope::class)
 * public interface DiscoverTabDestinationBinding {
 *     public companion object {
 *         @Provides
 *         @IntoSet
 *         public fun provideDiscoverNavDestination(graphFactory: DiscoverTabGraph.Factory): NavDestination<*> = NavDestination.TabRoot(
 *             routeClass = DiscoverRoot::class,
 *         ) { _, componentContext ->
 *             TabChild(graphFactory.createDiscoverTabGraph(componentContext).discoverPresenter)
 *         }
 *
 *         @Provides
 *         @IntoSet
 *         public fun provideDiscoverNavRoot(): NavRoot = DiscoverRoot
 *
 *         @Provides
 *         @IntoSet
 *         public fun provideDiscoverRootBinding(): NavRootBinding<*> =
 *             NavRootBinding(DiscoverRoot::class, DiscoverRoot.serializer())
 *     }
 * }
 * ```
 */
internal object TabDestinationBindingGenerator {
    /**
     * Generates the binding file for one tab root presenter annotation.
     *
     * @param data The parsed annotation, which carries every name and scope the generator needs.
     * @return The generated binding file as a KotlinPoet [FileSpec].
     */
    fun generate(data: TabData): FileSpec = contributingBindingFile(
        bindingName = data.bindingClassName,
        parentScope = data.parentScope,
        destinationFun(data),
        navRootFun(data),
        rootBindingFun(data),
    )

    /**
     * Builds the `provide<BaseName>NavDestination` function. The function returns
     * `NavDestination<*>` and its body constructs a `NavDestination.TabRoot` whose factory lambda
     * calls the graph factory and wraps the produced presenter in `TabChild(...)`.
     */
    private fun destinationFun(data: TabData): FunSpec =
        FunSpec.builder("provide${data.baseName}NavDestination")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .addParameter("graphFactory", data.graphFactoryClassName)
            .returns(NavDestination.parameterizedByStar())
            .addCode(
                CodeBlock.builder()
                    .add("return %T(\n", NavDestinationTabRoot)
                    .indent()
                    .add("routeClass = %T::class,\n", data.scope)
                    .add(") { _, componentContext ->\n")
                    .indent()
                    .add(
                        "%T(graphFactory.%L(componentContext).%L)\n",
                        TabChild,
                        data.graphFactoryFunName,
                        data.graphPropertyName,
                    )
                    .unindent()
                    .add("}\n")
                    .build(),
            )
            .build()

    /**
     * Builds the `provide<BaseName>NavRoot` function. Returns the route singleton typed as
     * `NavRoot` and contributes it to `Set<NavRoot>`, replacing the hand-written
     * `<Feature>RootBinding` files that consumers used to keep alongside each tab.
     */
    private fun navRootFun(data: TabData): FunSpec =
        FunSpec.builder("provide${data.baseName}NavRoot")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .returns(NavRoot)
            .addStatement("return %T", data.scope)
            .build()

    /**
     * Builds the `provide<BaseName>RootBinding` function. Returns `NavRootBinding<*>` so the tab
     * root participates in polymorphic save and restore alongside other `NavRoot` instances.
     */
    private fun rootBindingFun(data: TabData): FunSpec =
        FunSpec.builder("provide${data.baseName}RootBinding")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(IntoSet)
            .returns(NavRootBinding.parameterizedByStar())
            .addStatement(
                "return %T(%T::class, %T.serializer())",
                NavRootBinding,
                data.scope,
                data.scope,
            )
            .build()
}
