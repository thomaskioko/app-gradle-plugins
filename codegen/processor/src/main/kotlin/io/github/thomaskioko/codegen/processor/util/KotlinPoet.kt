/*
 * Copyright 2026 Thomas Kioko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thomaskioko.codegen.processor.util

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec

/** Indent string used by every emitted [com.squareup.kotlinpoet.FileSpec]. KSP convention is four spaces. */
internal const val FOUR_SPACE_INDENT: String = "    "

/**
 * Builds the `@ContributesTo([scope]::class)` annotation that every generated binding interface
 * carries. This is the Metro annotation that adds the binding to the named scope's graph.
 *
 * @param scope The dependency injection scope the binding contributes to (typically
 *   `ActivityScope`).
 */
internal fun contributesTo(scope: ClassName): AnnotationSpec =
    AnnotationSpec.builder(ContributesTo)
        .addMember("%T::class", scope)
        .build()

/**
 * Builds the `@BindingContainer` marker annotation that the UI binding objects carry. Used only
 * by [io.github.thomaskioko.codegen.processor.codegen.UiBindingGenerator]; the destination binding generators use `interface + companion` and do
 * not need this annotation.
 */
internal fun bindingContainer(): AnnotationSpec =
    AnnotationSpec.builder(BindingContainer).build()

/**
 * Builds the `@SingleIn([scope]::class)` annotation that scopes a `@Provides` function to the
 * lifetime of the given dependency injection scope.
 *
 * @param scope The scope the bound instance lives in.
 */
internal fun singleIn(scope: ClassName): AnnotationSpec =
    AnnotationSpec.builder(SingleIn)
        .addMember("%T::class", scope)
        .build()

/**
 * Builds the `@GraphExtension([scope]::class)` annotation that every generated graph interface
 * carries. This is the Metro annotation that declares a graph fragment scoped to the given type.
 *
 * @param scope The graph scope, always the route class for this codegen.
 */
internal fun graphExtension(scope: ClassName): AnnotationSpec =
    AnnotationSpec.builder(GraphExtension)
        .addMember("%T::class", scope)
        .build()

/**
 * Builds the `@GraphExtension.Factory` marker annotation that the nested factory interface inside
 * each generated graph carries. This is the Metro annotation that marks an interface as a graph
 * factory.
 */
internal fun graphExtensionFactory(): AnnotationSpec =
    AnnotationSpec.builder(GraphExtension.nestedClass("Factory")).build()

/**
 * Adds the `@HiddenFromObjC` annotation when [hide] is true. Exported Objective-C classes cannot
 * be dead-stripped, and no Swift code names the generated dependency injection types, so hiding
 * them keeps the plumbing out of any framework header. The flag is false for JVM-only
 * compilations, where the annotation's `@OptionalExpectation` declaration does not resolve.
 */
internal fun TypeSpec.Builder.hideFromObjC(hide: Boolean): TypeSpec.Builder =
    if (hide) addAnnotation(AnnotationSpec.builder(HiddenFromObjC).build()) else this

/**
 * Adds the `@file:OptIn(ExperimentalObjCRefinement::class)` annotation when [hide] is true, which
 * [hideFromObjC] requires on Kotlin 2.4.
 */
internal fun FileSpec.Builder.optInObjCRefinement(hide: Boolean): FileSpec.Builder =
    if (hide) {
        addAnnotation(
            AnnotationSpec.builder(OptIn)
                .addMember("%T::class", ExperimentalObjCRefinement)
                .build(),
        )
    } else {
        this
    }

/**
 * Returns this class name parameterized by `*`, for example `NavDestination<*>`. Used when the
 * generated code references a generic type without specifying its type argument.
 */
internal fun ClassName.parameterizedByStar(): ParameterizedTypeName = parameterizedBy(STAR)
