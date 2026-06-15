package io.github.thomaskioko.codegen.featureflag.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * KSP processor that turns `@FeatureFlag`-decorated qualifier annotations into Metro binding
 * interfaces. Loaded once per build by KSP through [FeatureFlagCodegenProcessorProvider].
 *
 * For each annotated symbol the processor validates the shape (annotation class, has `@Qualifier`,
 * non-blank `key` and `title`, ISO-formatted `dateAdded`), parses the annotation arguments,
 * computes the base name (qualifier simple name minus trailing `Qualifier` suffix), and hands the
 * result to [FeatureFlagBindingGenerator] which emits one `<BaseName>Binding.kt` file into the
 * qualifier's package. The processor never throws on bad input. Validation failures are reported
 * through [KSPLogger.error] pinned to the offending symbol so KSP turns them into compile errors at
 * the source position.
 *
 * @property codeGenerator KSP's file writer. Receives every produced [FileSpec].
 * @property logger KSP's diagnostic sink. Reports validation errors pinned to symbols.
 */
public class FeatureFlagCodegenProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    /**
     * Runs one KSP round. Iterates every symbol annotated with `@FeatureFlag`, validates and
     * parses each into a [FeatureFlagData], and writes one binding file per symbol.
     *
     * @param resolver KSP's lookup interface for the current round.
     * @return An empty list. The processor does not defer work to later rounds.
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (symbol in resolver.getSymbolsWithAnnotation(FEATURE_FLAG_FQN)) {
            val declaration = symbol as? KSClassDeclaration ?: run {
                logger.error(
                    "[FeatureFlag/InvalidTarget] @FeatureFlag must annotate an annotation class. " +
                        "Saw it on a non-class symbol.",
                    symbol,
                )
                continue
            }
            val data = parse(declaration) ?: continue
            writeFile(declaration, FeatureFlagBindingGenerator.generate(data))
        }
        return emptyList()
    }

    /**
     * Validates and parses the `@FeatureFlag` annotation on [declaration].
     *
     * Returns `null` and logs a compile error via [KSPLogger.error] when any validation rule fails:
     * the target is not an annotation class, the annotation class lacks `@Qualifier`, `key` is
     * blank, `title` is blank, or `dateAdded` does not parse as an ISO `YYYY-MM-DD` date.
     */
    private fun parse(declaration: KSClassDeclaration): FeatureFlagData? {
        val qualifierName = declaration.simpleName.asString()

        if (declaration.classKind != ClassKind.ANNOTATION_CLASS) {
            logger.error(
                "[FeatureFlag/InvalidTarget] @FeatureFlag must annotate an annotation class. " +
                    "Saw @$qualifierName on a ${declaration.classKind.type}.",
                declaration,
            )
            return null
        }

        if (!declaration.hasAnnotation(QUALIFIER_FQN)) {
            logger.error(
                "[FeatureFlag/MissingQualifier] @FeatureFlag must be applied to an annotation " +
                    "class also annotated with @dev.zacsweers.metro.Qualifier. " +
                    "Saw @$qualifierName declared without @Qualifier.",
                declaration,
            )
            return null
        }

        val annotation = declaration.annotations.firstOrNull { it.shortName.asString() == FEATURE_FLAG_SHORT_NAME }
            ?: run {
                logger.error("Missing @FeatureFlag annotation", declaration)
                return null
            }

        val key = annotation.stringArgument("key")
        if (key.isBlank()) {
            logger.error(
                "[FeatureFlag/EmptyKey] @FeatureFlag(key = \"\") on @$qualifierName is invalid. " +
                    "Provide the Firebase Remote Config key.",
                declaration,
            )
            return null
        }

        val title = annotation.stringArgument("title")
        if (title.isBlank()) {
            logger.error(
                "[FeatureFlag/EmptyTitle] @FeatureFlag(title = \"\") on @$qualifierName is invalid. " +
                    "Provide the debug-screen title.",
                declaration,
            )
            return null
        }

        val description = annotation.stringArgument("description")
        val defaultValue = annotation.booleanArgument("defaultValue")
        val dateAdded = annotation.stringArgument("dateAdded")
        val date = parseIsoDate(dateAdded)
        if (date == null) {
            logger.error(
                "[FeatureFlag/InvalidDate] @FeatureFlag(dateAdded = \"$dateAdded\") on " +
                    "@$qualifierName is not a valid ISO date (YYYY-MM-DD).",
                declaration,
            )
            return null
        }

        val baseName = qualifierName.removeSuffix(QUALIFIER_SUFFIX)

        return FeatureFlagData(
            packageName = declaration.packageName.asString(),
            qualifierClassName = declaration.toClassName(),
            baseName = baseName,
            key = key,
            title = title,
            description = description,
            defaultValue = defaultValue,
            year = date.year,
            month = date.month,
            day = date.day,
        )
    }

    /**
     * Writes [file] to disk via KSP's [CodeGenerator]. Uses `aggregating = false` so the output
     * depends only on the source file the annotation lives in; editing one flag does not
     * invalidate sibling flags.
     */
    private fun writeFile(source: KSClassDeclaration, file: FileSpec) {
        val containingFile = source.containingFile ?: run {
            logger.warn("Cannot determine containing file for ${source.qualifiedName?.asString()}", source)
            return
        }
        file.writeTo(codeGenerator, Dependencies(aggregating = false, containingFile))
    }

    private fun KSClassDeclaration.hasAnnotation(fqn: String): Boolean =
        annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == fqn }

    private fun KSAnnotation.stringArgument(name: String): String {
        val value = arguments.firstOrNull { it.name?.asString() == name }?.value
            ?: defaultArguments.firstOrNull { it.name?.asString() == name }?.value
            ?: error("@FeatureFlag missing required '$name' argument")
        return value as? String
            ?: error("@FeatureFlag '$name' argument is not a String (got ${value::class.simpleName})")
    }

    private fun KSAnnotation.booleanArgument(name: String): Boolean {
        val value = arguments.firstOrNull { it.name?.asString() == name }?.value
            ?: defaultArguments.firstOrNull { it.name?.asString() == name }?.value
            ?: error("@FeatureFlag missing required '$name' argument")
        return value as? Boolean
            ?: error("@FeatureFlag '$name' argument is not a Boolean (got ${value::class.simpleName})")
    }

    private fun parseIsoDate(value: String): IsoDate? {
        val parts = value.split('-')
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (month !in 1..12) return null
        if (day !in 1..31) return null
        return IsoDate(year, month, day)
    }

    private data class IsoDate(val year: Int, val month: Int, val day: Int)

    internal companion object {
        internal const val FEATURE_FLAG_FQN: String = "io.github.thomaskioko.codegen.annotations.FeatureFlag"
        internal const val FEATURE_FLAG_SHORT_NAME: String = "FeatureFlag"
        internal const val QUALIFIER_FQN: String = "dev.zacsweers.metro.Qualifier"
        internal const val QUALIFIER_SUFFIX: String = "Qualifier"
    }
}
