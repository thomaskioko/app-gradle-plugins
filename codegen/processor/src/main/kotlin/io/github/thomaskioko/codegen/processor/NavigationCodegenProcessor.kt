package io.github.thomaskioko.codegen.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.thomaskioko.codegen.processor.codegen.FileGenerator
import io.github.thomaskioko.codegen.processor.codegen.UiBindingGenerator
import io.github.thomaskioko.codegen.processor.data.NavData
import io.github.thomaskioko.codegen.processor.data.UiBindingKind
import io.github.thomaskioko.codegen.processor.parser.findNestedAssistedFactory
import io.github.thomaskioko.codegen.processor.parser.parseScreenData
import io.github.thomaskioko.codegen.processor.parser.parseSheetData
import io.github.thomaskioko.codegen.processor.parser.parseTabData
import io.github.thomaskioko.codegen.processor.parser.parseUiBindingData

public class NavigationCodegenProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        processAnnotation(resolver, Constants.NAV_SCREEN_FQN, Constants.NAV_SCREEN) { presenter ->
            parseScreenData(presenter, logger)
        }
        processAnnotation(resolver, Constants.TAB_SCREEN_FQN, Constants.TAB_SCREEN) { presenter ->
            parseTabData(presenter, logger)
        }
        processAnnotation(resolver, Constants.NAV_SHEET_FQN, Constants.NAV_SHEET, ::parseNavSheet)
        processUiBinding(resolver, Constants.SCREEN_UI_FQN, Constants.SCREEN_UI, UiBindingKind.Screen)
        processUiBinding(resolver, Constants.SHEET_UI_FQN, Constants.SHEET_UI, UiBindingKind.Sheet)
        return emptyList()
    }

    private fun processUiBinding(
        resolver: Resolver,
        fqn: String,
        shortName: String,
        kind: UiBindingKind,
    ) {
        for (symbol in resolver.getSymbolsWithAnnotation(fqn)) {
            val function = symbol as? KSFunctionDeclaration ?: run {
                logger.error("@$shortName can only be applied to functions", symbol)
                continue
            }
            val data = parseUiBindingData(function, kind, logger) ?: continue
            writeFunctionFiles(function, listOf(UiBindingGenerator.generate(data)))
        }
    }

    private fun writeFunctionFiles(source: KSFunctionDeclaration, files: List<FileSpec>) {
        val containingFile = source.containingFile ?: run {
            logger.warn("Cannot determine containing file for ${source.qualifiedName?.asString()}", source)
            return
        }
        val deps = Dependencies(aggregating = false, containingFile)
        for (file in files) {
            file.writeTo(codeGenerator, deps)
        }
    }

    private fun processAnnotation(
        resolver: Resolver,
        fqn: String,
        shortName: String,
        parse: (KSClassDeclaration) -> NavData?,
    ) {
        for (symbol in resolver.getSymbolsWithAnnotation(fqn)) {
            val declaration = symbol as? KSClassDeclaration ?: run {
                logger.error("@$shortName can only be applied to classes", symbol)
                continue
            }
            val data = parse(declaration) ?: continue
            writeFiles(declaration, FileGenerator.generate(data))
        }
    }

    private fun parseNavSheet(presenter: KSClassDeclaration): NavData? {
        val nestedFactory = presenter.findNestedAssistedFactory() ?: run {
            logger.error(
                "@${Constants.NAV_SHEET} presenter ${presenter.qualifiedName?.asString()} " +
                    "must declare a nested @AssistedFactory",
                presenter,
            )
            return null
        }
        return parseSheetData(presenter, nestedFactory, logger)
    }

    private fun writeFiles(source: KSClassDeclaration, files: List<FileSpec>) {
        val containingFile = source.containingFile ?: run {
            logger.warn("Cannot determine containing file for ${source.qualifiedName?.asString()}", source)
            return
        }
        val deps = Dependencies(aggregating = false, containingFile)
        for (file in files) {
            file.writeTo(codeGenerator, deps)
        }
    }
}
