package io.github.thomaskioko.codegen.processor.parser

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.thomaskioko.codegen.processor.Constants
import io.github.thomaskioko.codegen.processor.data.AppRootUiData
import io.github.thomaskioko.codegen.processor.data.AppRootUiParameter

/**
 * Parses an `@AppRootUi` annotation on a composable function into the [AppRootUiData] the
 * [io.github.thomaskioko.codegen.processor.codegen.AppRootUiBindingGenerator] consumes.
 *
 * The parser reads the composable's parameters in declaration order, skips a final
 * `modifier: Modifier` parameter (recording its presence on [AppRootUiData.hasModifier]), and
 * captures every other parameter as an [AppRootUiParameter]. The generator turns those entries
 * into properties on the generated `AppRootProvider` interface and uses the same names to call
 * the composable inside the generated extension.
 *
 * ## Errors
 *
 * Returns `null` and logs a compile error pinned to the function when:
 *
 * - The function lives in the default (empty) package.
 * - The function has no non-modifier parameters.
 * - The first non-modifier parameter type does not equal the annotation's `presenter` argument.
 *
 * @param function The composable function the annotation is attached to.
 * @param logger KSP's diagnostic sink. Used to report validation errors.
 * @return The structured representation of the annotation, or `null` if validation failed.
 */
internal fun parseAppRootUiData(
    function: KSFunctionDeclaration,
    logger: KSPLogger,
): AppRootUiData? {
    val annotation = function.annotations.firstOrNull { ann ->
        ann.annotationType.resolve().declaration.qualifiedName?.asString() == Constants.APP_ROOT_UI_FQN
    } ?: return null

    val pkg = function.packageName.asString()
    if (pkg.isEmpty()) {
        logger.error(
            "@${Constants.APP_ROOT_UI} cannot be applied to top-level functions in the default package",
            function,
        )
        return null
    }

    val presenterClass = annotation.classArgument("presenter")
    val parentScope = annotation.classArgument("parentScope")

    val allParameters = function.parameters
    val modifierParam = allParameters.lastOrNull { param ->
        val typeName = param.type.resolve().declaration.qualifiedName?.asString()
        param.name?.asString() == "modifier" && typeName == "androidx.compose.ui.Modifier"
    }
    val nonModifierParams = allParameters.filterNot { it === modifierParam }

    if (nonModifierParams.isEmpty()) {
        logger.error(
            "@${Constants.APP_ROOT_UI} requires the composable to declare at least one " +
                "non-modifier parameter for the root presenter",
            function,
        )
        return null
    }

    val firstParamTypeName = nonModifierParams.first().type.resolve().toTypeName()
    val expected = presenterClass.canonicalName
    val actual = firstParamTypeName.toString()
    if (actual != expected) {
        logger.error(
            "@${Constants.APP_ROOT_UI} first parameter type ($actual) does not match the " +
                "declared presenter type ($expected)",
            function,
        )
        return null
    }

    val parameters = nonModifierParams.map { param ->
        AppRootUiParameter(
            name = param.name?.asString()
                ?: error("Could not determine parameter name on @${Constants.APP_ROOT_UI} composable"),
            type = param.type.resolve().toTypeName(),
        )
    }

    return AppRootUiData(
        composableFunction = MemberName(pkg, function.simpleName.asString()),
        packageName = pkg,
        parameters = parameters,
        hasModifier = modifierParam != null,
        parentScope = parentScope,
    )
}
