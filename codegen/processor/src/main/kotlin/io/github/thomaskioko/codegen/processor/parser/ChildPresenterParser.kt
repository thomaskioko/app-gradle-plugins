package io.github.thomaskioko.codegen.processor.parser

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.thomaskioko.codegen.processor.Constants
import io.github.thomaskioko.codegen.processor.data.ChildPresenterData

/**
 * Parses a `@ChildPresenter` annotation on a presenter class into [ChildPresenterData].
 *
 * @param presenter The annotated class.
 * @param logger KSP's diagnostic sink. Used to report validation errors.
 * @return The structured representation of the annotation, or `null` if validation failed.
 */
internal fun parseChildPresenterData(
    presenter: KSClassDeclaration,
    logger: KSPLogger,
): ChildPresenterData? {
    val annotation = presenter.findAnnotation(Constants.CHILD_PRESENTER_FQN) ?: return null
    val scope = annotation.classArgument("scope")
    val parentScope = annotation.classArgument("parentScope")

    val pkg = presenter.packageName.asString()
    val baseName = presenter.simpleName.asString().removeSuffix("Presenter")

    return ChildPresenterData(
        presenterClass = presenter.toClassName(),
        baseName = baseName,
        packageName = if (pkg.isEmpty()) "di" else "$pkg.di",
        scope = scope,
        parentScope = parentScope,
    )
}
