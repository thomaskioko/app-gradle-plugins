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
package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.thomaskioko.codegen.processor.data.AppRootData
import io.github.thomaskioko.codegen.processor.util.ComponentContext
import io.github.thomaskioko.codegen.processor.util.FOUR_SPACE_INDENT
import io.github.thomaskioko.codegen.processor.util.Provides
import io.github.thomaskioko.codegen.processor.util.bindingContainer
import io.github.thomaskioko.codegen.processor.util.contributesTo
import io.github.thomaskioko.codegen.processor.util.singleIn

/**
 * Generates the binding file for an `@AppRoot` annotated presenter implementation.
 *
 * The output is a `@BindingContainer @ContributesTo(parentScope) object <InterfaceName>BindingContainer`
 * with one `@Provides @SingleIn(parentScope)` function that takes a `ComponentContext` and the
 * nested `Factory`, and returns the bound interface. The function body invokes the factory's
 * single function with the supplied `ComponentContext`.
 *
 * The output replaces the hand-written binding container the consumer would otherwise have to
 * keep in sync with the implementation's factory function name and the bound interface name.
 */
internal object AppRootBindingGenerator {

    /**
     * Generates the binding file for one `@AppRoot` implementation.
     *
     * @param data The parsed annotation, which carries the implementation, the bound interface,
     *   the factory class and function names, and the parent scope.
     * @return The generated binding file as a KotlinPoet [FileSpec].
     */
    fun generate(data: AppRootData): FileSpec {
        val factoryParam = ParameterSpec.builder("factory", data.factoryClassName).build()
        val componentContextParam = ParameterSpec.builder("componentContext", ComponentContext).build()

        val provideFun = FunSpec.builder(data.provideFunName)
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(Provides)
            .addAnnotation(singleIn(data.parentScope))
            .addParameter(componentContextParam)
            .addParameter(factoryParam)
            .returns(data.interfaceClassName)
            .addCode(buildProvideBody(data))
            .build()

        val bindingObject = TypeSpec.objectBuilder(data.bindingClassName)
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(bindingContainer())
            .addAnnotation(contributesTo(data.parentScope))
            .addFunction(provideFun)
            .build()

        return FileSpec.builder(data.bindingClassName)
            .indent(FOUR_SPACE_INDENT)
            .addType(bindingObject)
            .build()
    }

    /**
     * Generates the body of the `@Provides` function. The body returns a single expression that
     * invokes the factory function with the supplied `ComponentContext`.
     *
     * @param data The parsed annotation.
     * @return A KotlinPoet [CodeBlock] that becomes the body of the `@Provides` function.
     */
    private fun buildProvideBody(data: AppRootData): CodeBlock =
        CodeBlock.of("return factory.%L(componentContext)\n", data.factoryFunctionName)
}
