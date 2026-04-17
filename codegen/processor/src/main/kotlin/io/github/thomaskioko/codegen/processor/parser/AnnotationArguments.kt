package io.github.thomaskioko.codegen.processor.parser

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.thomaskioko.codegen.processor.Constants

internal fun KSAnnotation.findArgument(name: String): KSValueArgument =
    arguments.firstOrNull { it.name?.asString() == name }
        ?: defaultArguments.firstOrNull { it.name?.asString() == name }
        ?: error("Annotation @${shortName.asString()} missing required '$name' argument")

internal fun KSAnnotation.classArgument(name: String): ClassName {
    val type = findArgument(name).value as? KSType
        ?: error("Annotation @${shortName.asString()} '$name' argument is not a KClass literal")
    val decl = type.declaration as? KSClassDeclaration
        ?: error("Annotation @${shortName.asString()} '$name' argument is not a class declaration")
    return decl.toClassName()
}

internal fun KSAnnotation.classDeclarationArgument(name: String): KSClassDeclaration {
    val type = findArgument(name).value as? KSType
        ?: error("Annotation @${shortName.asString()} '$name' argument is not a KClass literal")
    return type.declaration as? KSClassDeclaration
        ?: error("Annotation @${shortName.asString()} '$name' argument is not a class declaration")
}

internal fun KSClassDeclaration.findAnnotation(fqn: String): KSAnnotation? =
    annotations.firstOrNull { annotation ->
        annotation.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
    }

internal fun KSClassDeclaration.hasAnnotation(fqn: String): Boolean = findAnnotation(fqn) != null

internal fun KSClassDeclaration.findNestedAssistedFactory(): KSClassDeclaration? =
    declarations
        .filterIsInstance<KSClassDeclaration>()
        .firstOrNull { it.hasAnnotation(Constants.ASSISTED_FACTORY_FQN) }

internal fun KSValueParameter.hasAssistedAnnotation(): Boolean =
    annotations.any { ann ->
        ann.annotationType.resolve().declaration.qualifiedName?.asString() == Constants.ASSISTED_FQN
    }
