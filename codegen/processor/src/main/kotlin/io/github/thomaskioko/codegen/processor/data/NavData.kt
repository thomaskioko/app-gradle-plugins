package io.github.thomaskioko.codegen.processor.data

import com.squareup.kotlinpoet.ClassName

/**
 * Structured representation of an annotated presenter class. Parsers produce one of these for each
 * `@NavDestination` annotation the processor sees, and generators consume nothing else.
 *
 * Every implementation carries the names a generator needs to emit its files (the graph interface
 * name, the binding interface name, the property the graph exposes, the factory function on the
 * graph) plus the dependency injection scopes those files contribute to. The implementations
 * differ only in destination role: [ScreenData] models stack screens and modal overlays, [TabData]
 * models top level tab anchors.
 *
 * The naming derived properties ([graphClassName], [graphFactoryFunName], [bindingClassName],
 * etc.) live on the data classes rather than in generators because they are a contract with the
 * consumer project: the generated file names are fixed by goldens and tests fail if they drift.
 * Centralising the naming convention here makes "what does the file get called" a single grep.
 *
 * @see ScreenData for stack screens and modal overlays.
 * @see TabData for top level tab roots.
 */
internal sealed interface NavData {
    /** The presenter class the annotation was attached to. */
    val presenterClass: ClassName

    /** The presenter's class name with the `Presenter` suffix stripped (`ShowsPresenter` becomes `Shows`). */
    val baseName: String

    /** Package the generated files land in: the presenter's package plus a `.di` suffix. */
    val packageName: String

    /** Parent dependency injection scope the binding contributes to (typically `ActivityScope`). */
    val parentScope: ClassName

    /** Scope of the generated `@GraphExtension`. Always the route class. */
    val scope: ClassName

    /** Class name of the generated graph interface (`<BaseName>ScreenGraph` or `<BaseName>TabGraph`). */
    val graphClassName: ClassName

    /** Function name on the generated graph factory (`create<BaseName>Graph` or `create<BaseName>TabGraph`). */
    val graphFactoryFunName: String

    /** Class name of the generated binding (`<BaseName>NavDestinationBinding` or `<BaseName>TabDestinationBinding`). */
    val bindingClassName: ClassName

    /** Type of the property exposed on the graph: the assisted factory for parameterized screens, the presenter otherwise. */
    val graphPropertyType: ClassName

    /** Camel cased property name the graph exposes (`showDetailsFactory` or `showsPresenter`). */
    val graphPropertyName: String

    /** Convenience: the nested `Factory` interface on the generated graph. */
    val graphFactoryClassName: ClassName
        get() = graphClassName.nestedClass("Factory")
}

/**
 * Whether a [ScreenData] models a stack screen or a modal overlay.
 *
 * Stack screens replace the current destination on the back stack; tapping back pops them off.
 * Modal overlays appear on top of the current screen without affecting the back stack; dismissing
 * them returns to the screen underneath. The generated graph and binding are structurally
 * identical for both. Only the `NavDestination` subclass the binding contributes differs.
 */
internal enum class ScreenKind {
    /** A screen pushed onto the navigation stack. The binding contributes a `NavDestination.Screen`. */
    SCREEN,

    /** A modal overlay (sheet, dialog, or menu). The binding contributes a `NavDestination.Overlay`. */
    OVERLAY,
}

/**
 * Structured representation of a `SCREEN` or `OVERLAY` destination.
 *
 * If the presenter uses plain `@Inject`, [factory] and [routeProperty] are both `null`, and the
 * generated graph exposes the presenter directly. If the presenter uses `@AssistedInject` with a
 * nested `@AssistedFactory`, [factory] holds the factory's class name and [routeProperty] holds
 * the name of the route property the assisted parameter is read from. The derived
 * [isParameterized] flag reads `factory != null`.
 *
 * @property presenterClass The presenter class the annotation was attached to.
 * @property baseName The presenter's class name with the `Presenter` suffix stripped.
 * @property packageName Package the generated files land in (the presenter's package plus `.di`).
 * @property parentScope Parent dependency injection scope the binding contributes to.
 * @property scope Scope of the generated `@GraphExtension`. Always the route class.
 * @property route The route class this destination represents. For SCREEN it implements
 *   `NavRoute`; for OVERLAY it implements `NavRoute` plus the consumer's overlay marker
 *   interface.
 * @property factory The presenter's nested `@AssistedFactory` class name when the presenter is
 *   parameterized, or `null` for plain `@Inject` presenters.
 * @property routeProperty Name of the route property the assisted parameter is read from when the
 *   presenter is parameterized, or `null` for plain `@Inject` presenters.
 * @property kind Whether this is a stack screen or a modal overlay. Picks which `NavDestination`
 *   subclass the binding contributes.
 * @see ScreenKind for the SCREEN versus OVERLAY distinction.
 */
internal data class ScreenData(
    override val presenterClass: ClassName,
    override val baseName: String,
    override val packageName: String,
    override val parentScope: ClassName,
    override val scope: ClassName,
    val route: ClassName,
    val factory: ClassName? = null,
    val routeProperty: String? = null,
    val kind: ScreenKind = ScreenKind.SCREEN,
) : NavData {
    val isParameterized: Boolean
        get() = factory != null
    override val graphClassName: ClassName = ClassName(packageName, "${baseName}ScreenGraph")
    override val graphFactoryFunName: String = "create${baseName}Graph"
    override val bindingClassName: ClassName = ClassName(packageName, "${baseName}NavDestinationBinding")
    override val graphPropertyType: ClassName = factory ?: presenterClass
    override val graphPropertyName: String =
        if (isParameterized) factoryAccessor(baseName) else presenterAccessor(baseName)
}

/**
 * Structured representation of a `TAB_ROOT` destination.
 *
 * Tabs are always plain `@Inject`. The parser rejects `@AssistedInject` tab presenters before
 * they ever reach a generator, so there is no factory or route property branch. The generated
 * graph always exposes the presenter directly.
 *
 * @property presenterClass The presenter class the annotation was attached to.
 * @property baseName The presenter's class name with the `Presenter` suffix stripped.
 * @property packageName Package the generated files land in (the presenter's package plus `.di`).
 * @property parentScope Parent dependency injection scope the binding contributes to.
 * @property scope Scope of the generated `@GraphExtension`. Always the route class.
 * @property configEnclosing The class enclosing the route, when the route is nested inside another
 *   class. For top level routes (the common case) this equals the route itself.
 */
internal data class TabData(
    override val presenterClass: ClassName,
    override val baseName: String,
    override val packageName: String,
    override val parentScope: ClassName,
    override val scope: ClassName,
    val configEnclosing: ClassName,
) : NavData {
    override val graphClassName: ClassName = ClassName(packageName, "${baseName}TabGraph")
    override val graphFactoryFunName: String = "create${baseName}TabGraph"
    override val bindingClassName: ClassName = ClassName(packageName, "${baseName}TabDestinationBinding")
    override val graphPropertyType: ClassName = presenterClass
    override val graphPropertyName: String = presenterAccessor(baseName)
}

private fun presenterAccessor(baseName: String): String =
    baseName.replaceFirstChar { it.lowercaseChar() } + "Presenter"

private fun factoryAccessor(baseName: String): String =
    baseName.replaceFirstChar { it.lowercaseChar() } + "Factory"
