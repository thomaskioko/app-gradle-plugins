package io.github.thomaskioko.codegen.processor.parser

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.MemberName
import io.github.thomaskioko.codegen.processor.Constants
import io.github.thomaskioko.codegen.processor.data.UiBindingData
import io.github.thomaskioko.codegen.processor.data.UiBindingKind

/**
 * Parses an `@ScreenUi` or `@SheetUi` annotation on a composable function into [UiBindingData]
 * the [io.github.thomaskioko.codegen.processor.codegen.UiBindingGenerator] consumes.
 *
 * The two annotations share this single parser because the generated binding has the same
 * structure for both. The fields that differ between the two (the content type, the destination
 * cast target, whether a `Modifier` is forwarded) are picked at generation time. The caller
 * passes the [kind] so the parser knows which annotation to look up.
 *
 * ## Errors
 *
 * Returns `null` and logs a compile error pinned to the function when:
 *
 * - The function lives in the default (empty) package. Generated bindings need a non empty
 *   package to land in.
 *
 * Throws (through [classArgument]) if the annotation is missing its required `presenter` or
 * `parentScope` arguments. That indicates the annotation class was changed without updating this
 * parser, not a user error.
 *
 * @param function The composable function the annotation is attached to.
 * @param kind Whether this is a screen renderer or an overlay renderer.
 * @param logger KSP's diagnostic sink. Used to report validation errors.
 * @return The structured representation of the annotation, or `null` if validation failed.
 */
internal fun parseUiBindingData(
    function: KSFunctionDeclaration,
    kind: UiBindingKind,
    logger: KSPLogger,
): UiBindingData? {
    val (fqn, shortName) = when (kind) {
        UiBindingKind.Screen -> Constants.SCREEN_UI_FQN to Constants.SCREEN_UI
        UiBindingKind.Sheet -> Constants.SHEET_UI_FQN to Constants.SHEET_UI
    }
    val annotation = function.annotations.firstOrNull { ann ->
        ann.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
    } ?: return null

    val pkg = function.packageName.asString()
    if (pkg.isEmpty()) {
        logger.error("@$shortName cannot be applied to top-level functions in the default package", function)
        return null
    }

    return UiBindingData(
        kind = kind,
        composableFunction = MemberName(pkg, function.simpleName.asString()),
        presenterClass = annotation.classArgument("presenter"),
        packageName = pkg,
        parentScope = annotation.classArgument("parentScope"),
    )
}
