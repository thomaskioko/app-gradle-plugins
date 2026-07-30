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
package io.github.thomaskioko.codegen.processor.parser

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toClassName
import io.github.thomaskioko.codegen.processor.Constants
import io.github.thomaskioko.codegen.processor.data.ChildPresenterData

/**
 * Parses a `@ChildPresenter` annotation on a presenter class into [ChildPresenterData].
 *
 * @param presenter The annotated class.
 * @param logger KSP's diagnostic sink. Used to report validation errors.
 * @return The structured representation of the annotation, or `null` if validation failed.
 */
internal fun parseChildPresenterData(
    presenter: KSClassDeclaration,
    logger: KSPLogger,
): ChildPresenterData? {
    val annotation = presenter.findAnnotation(Constants.CHILD_PRESENTER_FQN) ?: return null
    val scope = annotation.classArgument("scope")
    val parentScope = annotation.classArgument("parentScope")

    val pkg = presenter.packageName.asString()
    val baseName = presenter.simpleName.asString().removeSuffix("Presenter")
    val factory = presenter.findNestedAssistedFactory()?.toClassName()

    return ChildPresenterData(
        presenterClass = presenter.toClassName(),
        baseName = baseName,
        packageName = if (pkg.isEmpty()) "di" else "$pkg.di",
        scope = scope,
        parentScope = parentScope,
        factory = factory,
    )
}
