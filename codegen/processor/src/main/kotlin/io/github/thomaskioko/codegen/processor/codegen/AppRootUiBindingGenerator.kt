package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.thomaskioko.codegen.processor.data.AppRootUiData
import io.github.thomaskioko.codegen.processor.util.Composable
import io.github.thomaskioko.codegen.processor.util.FOUR_SPACE_INDENT
import io.github.thomaskioko.codegen.processor.util.Modifier

/**
 * Generates the binding file for an `@AppRootUi` annotated composable.
 *
 * The output contains two declarations:
 *
 * - An `AppRootProvider` interface declaring one `val` for each non-modifier parameter on the
 *   annotated composable.
 * - A `@Composable AppRootProvider.AppRootContent(modifier: Modifier)` extension that invokes the
 *   annotated composable using the receiver's properties.
 *
 * The consumer makes its activity-scope `@DependencyGraph` extend `AppRootProvider`. The activity
 * call site collapses from one argument per dependency to one extension call.
 */
internal object AppRootUiBindingGenerator {

    /**
     * Generates the binding file for one `@AppRootUi` composable.
     *
     * @param data The parsed annotation, which carries the composable function reference, the
     *   parameter list, and whether the composable accepts a `Modifier`.
     * @return The generated binding file as a KotlinPoet [FileSpec].
     */
    fun generate(data: AppRootUiData): FileSpec {
        val providerInterface = TypeSpec.interfaceBuilder(data.providerInterfaceName)
            .addModifiers(KModifier.PUBLIC)
            .also { builder ->
                data.parameters.forEach { parameter ->
                    builder.addProperty(
                        PropertySpec.builder(parameter.name, parameter.type)
                            .addModifiers(KModifier.PUBLIC)
                            .build(),
                    )
                }
            }
            .build()

        val composableAnnotation = AnnotationSpec.builder(Composable).build()

        val modifierParam = ParameterSpec.builder("modifier", Modifier)
            .defaultValue("%T", Modifier)
            .build()

        val extension = FunSpec.builder("AppRootContent")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(composableAnnotation)
            .receiver(data.providerInterfaceName)
            .addParameter(modifierParam)
            .addCode(buildExtensionBody(data))
            .build()

        return FileSpec.builder(data.bindingClassName)
            .indent(FOUR_SPACE_INDENT)
            .addType(providerInterface)
            .addFunction(extension)
            .build()
    }

    /**
     * Generates the body of the `AppRootContent` extension. The body invokes the annotated
     * composable, passing each parameter from the matching property on the receiver. When the
     * composable accepts a `modifier` parameter, the extension's `modifier` argument is forwarded.
     *
     * @param data The parsed annotation.
     * @return A KotlinPoet [CodeBlock] that becomes the body of the extension.
     */
    private fun buildExtensionBody(data: AppRootUiData): CodeBlock {
        val builder = CodeBlock.builder()
            .add("%M(\n", data.composableFunction)
            .indent()
        data.parameters.forEach { parameter ->
            builder.add("%L = %L,\n", parameter.name, parameter.name)
        }
        if (data.hasModifier) {
            builder.add("modifier = modifier,\n")
        }
        builder.unindent()
            .add(")\n")
        return builder.build()
    }
}
