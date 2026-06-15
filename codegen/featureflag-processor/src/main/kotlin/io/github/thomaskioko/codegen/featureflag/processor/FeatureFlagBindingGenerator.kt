package io.github.thomaskioko.codegen.featureflag.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import io.github.thomaskioko.codegen.featureflag.processor.util.AppScope
import io.github.thomaskioko.codegen.featureflag.processor.util.ContributesTo
import io.github.thomaskioko.codegen.featureflag.processor.util.FeatureFlag
import io.github.thomaskioko.codegen.featureflag.processor.util.FeatureFlagFactory
import io.github.thomaskioko.codegen.featureflag.processor.util.IntoSet
import io.github.thomaskioko.codegen.featureflag.processor.util.LocalDate
import io.github.thomaskioko.codegen.featureflag.processor.util.Provides
import io.github.thomaskioko.codegen.featureflag.processor.util.SingleIn

/**
 * Emits the per-qualifier binding file for one `@FeatureFlag`-decorated qualifier annotation.
 *
 * The output is one [FileSpec] containing a `@ContributesTo(AppScope::class) public interface
 * <baseName>Binding` with two `@Provides` methods:
 *
 * - `provide<baseName>(factory: FeatureFlagFactory): FeatureFlag<Boolean>` — qualified with the
 *   consumer's qualifier annotation and `@SingleIn(AppScope::class)`. Body calls
 *   `factory.boolean(key, title, description, defaultValue, dateAdded)` with the literal arguments
 *   parsed from the source annotation.
 * - `bind<baseName>(flag: FeatureFlag<Boolean>): FeatureFlag<Boolean>` — annotated `@IntoSet`. Body
 *   returns the qualified parameter so the same instance enters the
 *   `Set<FeatureFlag<Boolean>>` multibinding.
 *
 * The generated interface is `public` so the consumer's IDE can navigate to it and so Metro can
 * pick up the contributed methods.
 */
internal object FeatureFlagBindingGenerator {

    /**
     * Builds the binding file for one parsed [FeatureFlagData].
     *
     * @param data The parsed annotation arguments and computed names.
     * @return A KotlinPoet [FileSpec] ready to write to `<packageName>.<baseName>Binding.kt`.
     */
    fun generate(data: FeatureFlagData): FileSpec {
        val flagOfBoolean = FeatureFlag.parameterizedBy(BOOLEAN)

        val provideFun = FunSpec.builder("provide${data.baseName}")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(Provides).build())
            .addAnnotation(
                AnnotationSpec.builder(SingleIn)
                    .addMember("%T::class", AppScope)
                    .build(),
            )
            .addAnnotation(AnnotationSpec.builder(data.qualifierClassName).build())
            .addParameter("factory", FeatureFlagFactory)
            .returns(flagOfBoolean)
            .addStatement(
                "return factory.boolean(\n" +
                    "    key = %S,\n" +
                    "    title = %S,\n" +
                    "    description = %S,\n" +
                    "    defaultValue = %L,\n" +
                    "    dateAdded = %T(%L, %L, %L),\n" +
                    ")",
                data.key,
                data.title,
                data.description,
                data.defaultValue,
                LocalDate,
                data.year,
                data.month,
                data.day,
            )
            .build()

        val bindFun = FunSpec.builder("bind${data.baseName}")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(Provides).build())
            .addAnnotation(AnnotationSpec.builder(IntoSet).build())
            .addParameter(
                ParameterSpec.builder("flag", flagOfBoolean)
                    .addAnnotation(AnnotationSpec.builder(data.qualifierClassName).build())
                    .build(),
            )
            .returns(flagOfBoolean)
            .addStatement("return flag")
            .build()

        val bindingInterface = TypeSpec.interfaceBuilder("${data.baseName}Binding")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(
                AnnotationSpec.builder(ContributesTo)
                    .addMember("%T::class", AppScope)
                    .build(),
            )
            .addFunction(provideFun)
            .addFunction(bindFun)
            .build()

        return FileSpec.builder(data.packageName, "${data.baseName}Binding")
            .addType(bindingInterface)
            .build()
    }
}
