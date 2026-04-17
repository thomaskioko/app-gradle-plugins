package io.github.thomaskioko.codegen.processor.util

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR

internal const val FOUR_SPACE_INDENT: String = "    "

internal fun contributesTo(scope: ClassName): AnnotationSpec =
    AnnotationSpec.builder(ContributesTo)
        .addMember("%T::class", scope)
        .build()

internal fun graphExtension(scope: ClassName): AnnotationSpec =
    AnnotationSpec.builder(GraphExtension)
        .addMember("%T::class", scope)
        .build()

internal fun graphExtensionFactory(): AnnotationSpec =
    AnnotationSpec.builder(GraphExtension.nestedClass("Factory")).build()

internal fun ClassName.parameterizedByStar(): ParameterizedTypeName = parameterizedBy(STAR)
