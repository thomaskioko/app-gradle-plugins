package io.github.thomaskioko.codegen.processor.data

import com.squareup.kotlinpoet.ClassName

internal sealed interface NavData {
    val presenterClass: ClassName
    val baseName: String
    val packageName: String
    val parentScope: ClassName
    val scope: ClassName
    val graphClassName: ClassName
    val graphFactoryFunName: String
    val bindingClassName: ClassName
    val graphPropertyType: ClassName
    val graphPropertyName: String
    val graphFactoryClassName: ClassName
        get() = graphClassName.nestedClass("Factory")
}

/**
 * A root-stack screen. When [factory] is `null` the presenter is plain `@Inject` and the graph
 * exposes it directly; when non-`null` the presenter is `@AssistedInject` with exactly one
 * `@Assisted` constructor parameter whose name equals [routeProperty] on the route class.
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

internal data class SheetData(
    override val presenterClass: ClassName,
    override val baseName: String,
    override val packageName: String,
    override val parentScope: ClassName,
    override val scope: ClassName,
    val factory: ClassName,
    val assistedMappings: List<AssistedParamMapping>,
) : NavData {
    override val graphClassName: ClassName = ClassName(packageName, "${baseName}ScreenGraph")
    override val graphFactoryFunName: String = "create${baseName}Graph"
    override val bindingClassName: ClassName = ClassName(packageName, "${baseName}DestinationBinding")
    override val graphPropertyType: ClassName = factory
    override val graphPropertyName: String = factoryAccessor(baseName)
}

internal data class AssistedParamMapping(
    val parameterName: String,
    val configProperty: String,
)

private fun presenterAccessor(baseName: String): String =
    baseName.replaceFirstChar { it.lowercaseChar() } + "Presenter"

private fun factoryAccessor(baseName: String): String =
    baseName.replaceFirstChar { it.lowercaseChar() } + "Factory"
