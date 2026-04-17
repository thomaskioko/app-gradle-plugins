package io.github.thomaskioko.codegen.processor.parser

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.thomaskioko.codegen.processor.Constants
import io.github.thomaskioko.codegen.processor.data.AssistedParamMapping
import io.github.thomaskioko.codegen.processor.data.ParameterizedScreenData
import io.github.thomaskioko.codegen.processor.data.SheetData
import io.github.thomaskioko.codegen.processor.data.SimpleScreenData
import io.github.thomaskioko.codegen.processor.data.TabData

internal fun parseSimpleScreenData(
    presenter: KSClassDeclaration,
    @Suppress("UNUSED_PARAMETER") logger: KSPLogger,
): SimpleScreenData? {
    val annotation = presenter.findAnnotation(Constants.NAV_SCREEN_FQN) ?: return null
    val route = annotation.classArgument("route")
    val parentScope = annotation.classArgument("parentScope")
    return SimpleScreenData(
        presenterClass = presenter.toClassName(),
        baseName = presenter.baseName(),
        packageName = presenter.diPackage(),
        parentScope = parentScope,
        scope = route,
        route = route,
    )
}

internal fun parseParameterizedScreenData(
    presenter: KSClassDeclaration,
    nestedFactory: KSClassDeclaration,
    logger: KSPLogger,
): ParameterizedScreenData? {
    val annotation = presenter.findAnnotation(Constants.NAV_SCREEN_FQN) ?: return null
    val route = annotation.classArgument("route")
    val parentScope = annotation.classArgument("parentScope")
    val routeProperty = inferSingleRouteProperty(presenter, route.simpleName, logger) ?: return null
    return ParameterizedScreenData(
        presenterClass = presenter.toClassName(),
        baseName = presenter.baseName(),
        packageName = presenter.diPackage(),
        parentScope = parentScope,
        scope = route,
        route = route,
        factory = nestedFactory.toClassName(),
        routeProperty = routeProperty,
    )
}

internal fun parseTabData(
    presenter: KSClassDeclaration,
    logger: KSPLogger,
): TabData? {
    val annotation = presenter.findAnnotation(Constants.TAB_SCREEN_FQN) ?: return null

    if (presenter.findNestedAssistedFactory() != null) {
        logger.error(
            "@${Constants.TAB_SCREEN} does not support @AssistedInject presenters; tabs " +
                "must be plain @Inject",
            presenter,
        )
        return null
    }

    val parentScope = annotation.classArgument("parentScope")
    val configDecl = annotation.classDeclarationArgument("config")
    val config = configDecl.toClassName()
    val configEnclosing = config.enclosingClassName() ?: config

    return TabData(
        presenterClass = presenter.toClassName(),
        baseName = presenter.baseName(),
        packageName = presenter.diPackage(),
        parentScope = parentScope,
        scope = config,
        configEnclosing = configEnclosing,
    )
}

internal fun parseSheetData(
    presenter: KSClassDeclaration,
    nestedFactory: KSClassDeclaration,
    logger: KSPLogger,
): SheetData? {
    val annotation = presenter.findAnnotation(Constants.NAV_SHEET_FQN) ?: return null
    val route = annotation.classArgument("route")
    val parentScope = annotation.classArgument("parentScope")
    val routeDecl = annotation.classDeclarationArgument("route")
    val mappings = buildAssistedMappings(presenter, routeDecl, logger) ?: return null

    return SheetData(
        presenterClass = presenter.toClassName(),
        baseName = presenter.baseName(),
        packageName = presenter.diPackage(),
        parentScope = parentScope,
        scope = route,
        factory = nestedFactory.toClassName(),
        assistedMappings = mappings,
    )
}

private fun KSClassDeclaration.baseName(): String =
    simpleName.asString().removeSuffix("Presenter")

private fun KSClassDeclaration.diPackage(): String {
    val pkg = packageName.asString()
    return if (pkg.isEmpty()) DI_PACKAGE_SUFFIX else "$pkg.$DI_PACKAGE_SUFFIX"
}

private const val DI_PACKAGE_SUFFIX: String = "di"

private fun inferSingleRouteProperty(
    presenter: KSClassDeclaration,
    routeSimpleName: String,
    logger: KSPLogger,
): String? {
    val ctor = presenter.primaryConstructor ?: run {
        logger.error(
            "Parameterized @NavScreen presenter must have a primary constructor",
            presenter,
        )
        return null
    }
    val assistedParams = ctor.parameters.filter { it.hasAssistedAnnotation() }
    if (assistedParams.size != 1) {
        logger.error(
            "Parameterized @NavScreen presenter must have exactly one @Assisted constructor " +
                "parameter (found ${assistedParams.size}). Route class: $routeSimpleName",
            presenter,
        )
        return null
    }
    return assistedParams.first().name?.asString() ?: run {
        logger.error("Could not determine name of @Assisted parameter", presenter)
        null
    }
}

private fun buildAssistedMappings(
    presenter: KSClassDeclaration,
    configClass: KSClassDeclaration,
    logger: KSPLogger,
): List<AssistedParamMapping>? {
    val ctor = presenter.primaryConstructor ?: run {
        logger.error("@NavSheet presenter must have a primary constructor", presenter)
        return null
    }
    val assistedParams = ctor.parameters.filter { it.hasAssistedAnnotation() }
    if (assistedParams.isEmpty()) {
        logger.error(
            "@NavSheet presenter ${presenter.qualifiedName?.asString()} must declare at least " +
                "one @Assisted constructor parameter",
            presenter,
        )
        return null
    }

    val configProperties = configClass.primaryConstructor?.parameters
        ?.mapNotNull { p -> p.name?.asString()?.let { name -> name to p.type.resolve() } }
        .orEmpty()
    if (configProperties.isEmpty()) {
        logger.error(
            "@NavSheet route ${configClass.qualifiedName?.asString()} must expose its " +
                "properties via a primary constructor",
            configClass,
        )
        return null
    }

    val usedConfigProps = mutableSetOf<String>()
    val mappings = mutableListOf<AssistedParamMapping>()
    for (param in assistedParams) {
        val paramName = param.name?.asString() ?: run {
            logger.error("Could not determine name of @Assisted parameter", presenter)
            return null
        }
        val paramType = param.type.resolve()
        val match = configProperties.firstOrNull { (propName, _) ->
            propName == paramName && propName !in usedConfigProps
        } ?: configProperties.firstOrNull { (propName, propType) ->
            propName !in usedConfigProps && propType == paramType
        }
        if (match == null) {
            logger.error(
                "@NavSheet presenter @Assisted parameter '$paramName' " +
                    "(type ${paramType.declaration.qualifiedName?.asString()}) " +
                    "has no matching property on route ${configClass.qualifiedName?.asString()}",
                presenter,
            )
            return null
        }
        usedConfigProps += match.first
        mappings += AssistedParamMapping(
            parameterName = paramName,
            configProperty = match.first,
        )
    }
    return mappings
}
