package io.github.thomaskioko.codegen.processor.parser

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.thomaskioko.codegen.processor.Constants
import io.github.thomaskioko.codegen.processor.data.NavData
import io.github.thomaskioko.codegen.processor.data.ScreenData
import io.github.thomaskioko.codegen.processor.data.ScreenKind
import io.github.thomaskioko.codegen.processor.data.TabData

/**
 * Parses an `@NavDestination` annotation on a presenter class into the [NavData] the generators
 * consume.
 *
 * `SCREEN` and `OVERLAY` produce a [ScreenData] tagged with the matching [ScreenKind]. `TAB_ROOT`
 * produces a [TabData]. The parser also detects whether the presenter accepts a runtime parameter
 * (a presenter with a nested `@AssistedFactory`) and records that on the resulting [ScreenData]
 * so the generator emits the correct factory wiring.
 *
 * ## Errors
 *
 * Returns `null` and logs a compile error pinned to the presenter when:
 *
 * - [kind] is not one of `SCREEN`, `OVERLAY`, or `TAB_ROOT`;
 * - the presenter is parameterized but does not have exactly one `@Assisted` parameter (see
 *   [inferSingleRoutePropertyForNavDestination]);
 * - [kind] is `TAB_ROOT` and the presenter declares a nested `@AssistedFactory`.
 *
 * @param presenter The class declaration the annotation is attached to.
 * @param logger KSP's diagnostic sink. Used to report validation errors.
 * @return The structured representation of the annotation, or `null` if validation failed.
 */
internal fun parseNavDestinationData(
    presenter: KSClassDeclaration,
    logger: KSPLogger,
): NavData? {
    val annotation = presenter.findAnnotation(Constants.NAV_DESTINATION_FQN) ?: return null
    val route = annotation.classArgument("route")
    val parentScope = annotation.classArgument("parentScope")
    val kind = annotation.enumArgument("kind")

    return when (kind) {
        "SCREEN" -> parseScreenLike(presenter, route, parentScope, ScreenKind.SCREEN, logger)

        "OVERLAY" -> parseScreenLike(presenter, route, parentScope, ScreenKind.OVERLAY, logger)

        "TAB_ROOT" -> parseTabLike(presenter, route, parentScope, logger)

        else -> {
            logger.error(
                "@${Constants.NAV_DESTINATION} unknown kind '$kind'. " +
                    "Expected SCREEN, OVERLAY, or TAB_ROOT.",
                presenter,
            )
            null
        }
    }
}

/**
 * Parses a `SCREEN` or `OVERLAY` presenter into a [ScreenData].
 *
 * The parser looks for a nested `@AssistedFactory` on the presenter. If one is present, the
 * presenter is parameterized: the resulting [ScreenData] holds the factory's class name and the
 * name of the route property the assisted parameter is read from. If no nested factory is
 * present, the presenter is plain `@Inject`: the resulting [ScreenData] has neither.
 *
 * @param presenter The presenter class.
 * @param route The route class extracted from the annotation.
 * @param parentScope The parent dependency injection scope extracted from the annotation.
 * @param kind Whether this is a stack screen or a modal overlay.
 * @param logger KSP's diagnostic sink. Used to report validation errors.
 */
private fun parseScreenLike(
    presenter: KSClassDeclaration,
    route: com.squareup.kotlinpoet.ClassName,
    parentScope: com.squareup.kotlinpoet.ClassName,
    kind: ScreenKind,
    logger: KSPLogger,
): ScreenData? {
    val nestedFactory = presenter.findNestedAssistedFactory()
    val (factory, routeProperty) = if (nestedFactory == null) {
        null to null
    } else {
        val property = inferSingleRoutePropertyForNavDestination(
            presenter = presenter,
            routeSimpleName = route.simpleName,
            logger = logger,
        ) ?: return null
        nestedFactory.toClassName() to property
    }
    return ScreenData(
        presenterClass = presenter.toClassName(),
        baseName = presenter.baseName(),
        packageName = presenter.diPackage(),
        parentScope = parentScope,
        scope = route,
        route = route,
        factory = factory,
        routeProperty = routeProperty,
        kind = kind,
    )
}

/**
 * Parses a `TAB_ROOT` presenter into a [TabData].
 *
 * Tabs are always plain `@Inject`. A tab's route is a singleton `data object` that carries no
 * payload, so there is no value for the parser to thread from the route into the presenter at
 * navigation time. A tab presenter that declares a nested `@AssistedFactory` is rejected with a
 * compile error.
 *
 * @param presenter The presenter class.
 * @param route The route class extracted from the annotation.
 * @param parentScope The parent dependency injection scope extracted from the annotation.
 * @param logger KSP's diagnostic sink. Used to report validation errors.
 */
private fun parseTabLike(
    presenter: KSClassDeclaration,
    route: com.squareup.kotlinpoet.ClassName,
    parentScope: com.squareup.kotlinpoet.ClassName,
    logger: KSPLogger,
): TabData? {
    if (presenter.findNestedAssistedFactory() != null) {
        logger.error(
            "@${Constants.NAV_DESTINATION}(kind = TAB_ROOT) does not support @AssistedInject " +
                "presenters; tab roots must be plain @Inject",
            presenter,
        )
        return null
    }
    return TabData(
        presenterClass = presenter.toClassName(),
        baseName = presenter.baseName(),
        packageName = presenter.diPackage(),
        parentScope = parentScope,
        scope = route,
        configEnclosing = route.enclosingClassName() ?: route,
    )
}

/**
 * Strips the `Presenter` suffix from a class's simple name to derive the prefix for every
 * generated artifact. `ShowsPresenter` becomes `Shows`, which becomes `ShowsScreenGraph`,
 * `ShowsNavDestinationBinding`, etc.
 */
private fun KSClassDeclaration.baseName(): String =
    simpleName.asString().removeSuffix("Presenter")

/**
 * Returns the package the generated files should land in: the presenter's package plus a `.di`
 * suffix. Generated files always live next to the source presenter.
 */
private fun KSClassDeclaration.diPackage(): String {
    val pkg = packageName.asString()
    return if (pkg.isEmpty()) "di" else "$pkg.di"
}

/**
 * Validates that a parameterized presenter has exactly one `@Assisted` constructor parameter and
 * returns the parameter's name. The generator reads the route property of the same name at
 * navigation time and passes it through the assisted factory.
 *
 * Reports a compile error and returns `null` if the presenter has no primary constructor, if the
 * number of `@Assisted` parameters is not one, or if the parameter has no name (a KSP edge case).
 *
 * @param presenter The parameterized presenter class.
 * @param routeSimpleName The route class's simple name. Included in the error message so the user
 *   can see which presenter and route the validation refers to.
 * @param logger KSP's diagnostic sink. Used to report validation errors.
 * @return The name of the single `@Assisted` parameter, or `null` if validation failed.
 */
private fun inferSingleRoutePropertyForNavDestination(
    presenter: KSClassDeclaration,
    routeSimpleName: String,
    logger: KSPLogger,
): String? {
    val ctor = presenter.primaryConstructor ?: run {
        logger.error(
            "Parameterized @${Constants.NAV_DESTINATION} presenter must have a primary constructor",
            presenter,
        )
        return null
    }
    val assistedParams = ctor.parameters.filter { it.hasAssistedAnnotation() }
    if (assistedParams.size != 1) {
        logger.error(
            "Parameterized @${Constants.NAV_DESTINATION} presenter must have exactly one " +
                "@Assisted constructor parameter (found ${assistedParams.size}). " +
                "Route class: $routeSimpleName",
            presenter,
        )
        return null
    }
    return assistedParams.first().name?.asString() ?: run {
        logger.error("Could not determine name of @Assisted parameter", presenter)
        null
    }
}
