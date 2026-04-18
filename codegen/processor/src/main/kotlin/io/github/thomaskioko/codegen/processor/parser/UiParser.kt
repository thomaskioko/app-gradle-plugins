package io.github.thomaskioko.codegen.processor.parser

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.MemberName
import io.github.thomaskioko.codegen.processor.Constants
import io.github.thomaskioko.codegen.processor.data.UiBindingData
import io.github.thomaskioko.codegen.processor.data.UiBindingKind

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
