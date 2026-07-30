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
package io.github.thomaskioko.codegen.featureflag.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import io.github.thomaskioko.codegen.featureflag.processor.util.Qualifier

/**
 * Emits the Metro `@Qualifier` annotation for one `@FeatureFlag`-decorated anchor.
 *
 * The output is one [FileSpec] containing a `@Qualifier public annotation class <baseName>Qualifier`
 * in the anchor's package. The qualifier carries no explicit `@Target` or `@Retention`, a deliberate
 * match to the hand-written qualifiers it replaces: absent `@Target` it applies to every site
 * (the `@Provides` function and the `@param:`-use site on the `@IntoSet` rebind), and absent
 * `@Retention` it takes Kotlin's default binary retention — Metro's `@Qualifier` meta-annotation is
 * what drives the DI match.
 *
 * The generated annotation is `public` so the consumer's injection sites and the generated
 * [FeatureFlagBindingGenerator] output can reference it across modules.
 */
internal object FeatureFlagQualifierGenerator {

    /**
     * Builds the qualifier file for one parsed [FeatureFlagData].
     *
     * @param data The parsed annotation arguments and computed names.
     * @return A KotlinPoet [FileSpec] ready to write to `<packageName>.<baseName>Qualifier.kt`.
     */
    fun generate(data: FeatureFlagData): FileSpec {
        val qualifier = TypeSpec.annotationBuilder("${data.baseName}Qualifier")
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(Qualifier).build())
            .build()

        return FileSpec.builder(data.packageName, "${data.baseName}Qualifier")
            .addType(qualifier)
            .build()
    }
}
