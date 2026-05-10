package io.github.thomaskioko.codegen.processor.parser

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.thomaskioko.codegen.processor.Constants
import io.github.thomaskioko.codegen.processor.data.AppRootData

/**
 * Parses an `@AppRoot` annotation on an `@AssistedInject` presenter implementation into the
 * [AppRootData] the [io.github.thomaskioko.codegen.processor.codegen.AppRootBindingGenerator]
 * consumes.
 *
 * The parser infers the bound interface from the implementation's supertypes by picking the
 * first non-marker interface. Decompose's `ComponentContext` (used as a delegate) is filtered
 * out. The nested `@AssistedFactory` interface is located, and its single factory function is
 * captured for the generated `@Provides` body.
 *
 * ## Errors
 *
 * Returns `null` and logs a compile error pinned to the implementation when:
 *
 * - The class does not carry `@AssistedInject`.
 * - The class does not declare a nested `@AssistedFactory` interface.
 * - The nested factory does not declare exactly one function.
 * - The class extends zero or more than one non-marker interface.
 *
 * @param presenter The class declaration the annotation is attached to.
 * @param logger KSP's diagnostic sink. Used to report validation errors.
 * @return The structured representation of the annotation, or `null` if validation failed.
 */
internal fun parseAppRootData(
    presenter: KSClassDeclaration,
    logger: KSPLogger,
): AppRootData? {
    val annotation = presenter.findAnnotation(Constants.APP_ROOT_FQN) ?: return null
    val parentScope = annotation.classArgument("parentScope")

    if (!presenter.hasAnnotation(Constants.ASSISTED_INJECT_FQN)) {
        logger.error(
            "@${Constants.APP_ROOT} requires @AssistedInject on the implementation class",
            presenter,
        )
        return null
    }

    val factory = presenter.findNestedAssistedFactory() ?: run {
        logger.error(
            "@${Constants.APP_ROOT} requires a nested @AssistedFactory interface on the " +
                "implementation class",
            presenter,
        )
        return null
    }

    val factoryFunction = factory.declarations
        .filterIsInstance<com.google.devtools.ksp.symbol.KSFunctionDeclaration>()
        .filter { it.simpleName.asString() != "<init>" }
        .toList()
    if (factoryFunction.size != 1) {
        logger.error(
            "@${Constants.APP_ROOT} requires the nested @AssistedFactory to declare exactly one " +
                "function (found ${factoryFunction.size})",
            factory,
        )
        return null
    }

    val boundInterface = inferBoundInterface(presenter, logger) ?: return null

    val pkg = presenter.packageName.asString()
    return AppRootData(
        implClassName = presenter.toClassName(),
        interfaceClassName = boundInterface,
        factoryClassName = factory.toClassName(),
        factoryFunctionName = factoryFunction.first().simpleName.asString(),
        parentScope = parentScope,
        packageName = if (pkg.isEmpty()) "di" else "$pkg.di",
    )
}

/**
 * Picks the bound interface for an `@AppRoot` implementation from its declared supertypes.
 *
 * Walks the resolved supertypes in declaration order, skips Kotlin's implicit `kotlin.Any`,
 * skips Decompose's `ComponentContext` (which is commonly used as a delegate via
 * `ComponentContext by componentContext`), and returns the first remaining interface. If there
 * is more than one such interface or none at all, the function logs a compile error and returns
 * `null`.
 *
 * @param presenter The annotated implementation class.
 * @param logger KSP's diagnostic sink. Used to report validation errors.
 * @return The bound interface as a KotlinPoet [ClassName], or `null` if validation failed.
 */
private fun inferBoundInterface(
    presenter: KSClassDeclaration,
    logger: KSPLogger,
): ClassName? {
    val candidates = presenter.superTypes
        .mapNotNull { reference ->
            val declaration = reference.resolve().declaration as? KSClassDeclaration ?: return@mapNotNull null
            val qualifiedName = declaration.qualifiedName?.asString() ?: return@mapNotNull null
            if (qualifiedName == "kotlin.Any") return@mapNotNull null
            if (qualifiedName == "com.arkivanov.decompose.ComponentContext") return@mapNotNull null
            if (declaration.classKind != com.google.devtools.ksp.symbol.ClassKind.INTERFACE) return@mapNotNull null
            declaration.toClassName()
        }
        .toList()

    return when (candidates.size) {
        1 -> candidates.first()
        0 -> {
            logger.error(
                "@${Constants.APP_ROOT} requires the implementation to extend exactly one " +
                    "interface (found 0). Declare the bound presenter interface and implement it.",
                presenter,
            )
            null
        }
        else -> {
            val names = candidates.joinToString(", ") { it.simpleName }
            logger.error(
                "@${Constants.APP_ROOT} requires the implementation to extend exactly one " +
                    "interface (found ${candidates.size}: $names). The processor cannot infer " +
                    "which one is the bound presenter interface.",
                presenter,
            )
            null
        }
    }
}
