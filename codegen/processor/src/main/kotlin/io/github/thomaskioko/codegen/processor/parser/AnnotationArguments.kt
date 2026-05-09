package io.github.thomaskioko.codegen.processor.parser

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.thomaskioko.codegen.processor.Constants

/*
 * KSP helpers for picking apart `KSAnnotation` values. Centralised here so the parsers themselves
 * read as straight line transformation logic with no KSP boilerplate. Every helper that extracts a
 * required argument throws on missing or mistyped input. That is intentional: a missing argument
 * means the annotation class itself was changed without updating the parser, which is a
 * programming error in this repo, not a user error in the consumer project. User errors are
 * surfaced by the parsers through `KSPLogger` instead.
 */

/**
 * Returns the annotation argument named [name], falling back to its declared default if the
 * caller did not pass an explicit value.
 *
 * ## Example
 *
 * ```kotlin
 * val kind = annotation.findArgument("kind")
 * ```
 *
 * @param name The argument name as written in the annotation declaration.
 * @return The matching [KSValueArgument].
 * @throws IllegalStateException if the argument is missing both an explicit value and a declared
 *   default. This indicates the annotation class was changed without updating the parser.
 */
internal fun KSAnnotation.findArgument(name: String): KSValueArgument =
    arguments.firstOrNull { it.name?.asString() == name }
        ?: defaultArguments.firstOrNull { it.name?.asString() == name }
        ?: error("Annotation @${shortName.asString()} missing required '$name' argument")

/**
 * Reads a `KClass<*>` annotation argument and returns it as a KotlinPoet [ClassName] ready for
 * use in generated code.
 *
 * ## Example
 *
 * ```kotlin
 * val routeClass: ClassName = annotation.classArgument("route")
 * ```
 *
 * @param name The argument name as written in the annotation declaration.
 * @return The argument's value as a KotlinPoet [ClassName].
 * @throws IllegalStateException if the argument is missing or is not a class literal.
 */
internal fun KSAnnotation.classArgument(name: String): ClassName {
    val type = findArgument(name).value as? KSType
        ?: error("Annotation @${shortName.asString()} '$name' argument is not a KClass literal")
    val decl = type.declaration as? KSClassDeclaration
        ?: error("Annotation @${shortName.asString()} '$name' argument is not a class declaration")
    return decl.toClassName()
}

/**
 * Reads an enum typed annotation argument as the simple name of the selected constant.
 *
 * KSP can represent enum arguments in three different forms depending on KSP version and the
 * call site (`KSType`, `KSClassDeclaration`, or a raw `String` like `"DestinationKind.SCREEN"`).
 * This helper normalises all three to just the constant name (`"SCREEN"`).
 *
 * ## Example
 *
 * ```kotlin
 * val kind: String = annotation.enumArgument("kind") // returns "SCREEN", "OVERLAY", or "TAB_ROOT"
 * ```
 *
 * @param name The argument name as written in the annotation declaration.
 * @return The simple name of the selected enum constant.
 * @throws IllegalStateException if the argument value is none of the three expected forms.
 */
internal fun KSAnnotation.enumArgument(name: String): String {
    return when (val raw = findArgument(name).value) {
        is KSType -> raw.declaration.simpleName.asString()

        is KSClassDeclaration -> raw.simpleName.asString()

        is String -> raw.substringAfterLast('.')

        else -> error(
            "Annotation @${shortName.asString()} '$name' argument is not an enum value " +
                "(got ${raw?.let { it::class.simpleName }})",
        )
    }
}

/**
 * Returns the first annotation on this class whose fully qualified name equals [fqn], or `null`
 * if no such annotation is present.
 *
 * @param fqn Fully qualified name of the annotation to look up.
 * @return The matching annotation, or `null` if absent.
 */
internal fun KSClassDeclaration.findAnnotation(fqn: String): KSAnnotation? =
    annotations.firstOrNull { annotation ->
        annotation.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
    }

/**
 * Returns `true` if this class is annotated with the annotation whose fully qualified name is
 * [fqn]. Convenience for [findAnnotation] when only the presence of the annotation matters.
 *
 * @param fqn Fully qualified name of the annotation to look up.
 */
internal fun KSClassDeclaration.hasAnnotation(fqn: String): Boolean = findAnnotation(fqn) != null

/**
 * Returns the first nested class declaration on this class that carries Metro's `@AssistedFactory`
 * annotation, or `null` if none is present. Used by [parseNavDestinationData] to detect whether a
 * presenter is parameterized.
 */
internal fun KSClassDeclaration.findNestedAssistedFactory(): KSClassDeclaration? =
    declarations
        .filterIsInstance<KSClassDeclaration>()
        .firstOrNull { it.hasAnnotation(Constants.ASSISTED_FACTORY_FQN) }

/**
 * Returns `true` if this constructor parameter is annotated with Metro's `@Assisted`. Used by the
 * parameterized presenter validation in [inferSingleRoutePropertyForNavDestination].
 */
internal fun KSValueParameter.hasAssistedAnnotation(): Boolean =
    annotations.any { ann ->
        ann.annotationType.resolve().declaration.qualifiedName?.asString() == Constants.ASSISTED_FQN
    }
