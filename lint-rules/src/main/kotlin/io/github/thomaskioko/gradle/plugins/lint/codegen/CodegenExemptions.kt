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
package io.github.thomaskioko.gradle.plugins.lint.codegen

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CommaSeparatedListValueParser
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType

/**
 * `.editorconfig` property identifying presenter classes that intentionally do not carry a
 * navigation codegen annotation.
 *
 * Property key: `ktlint_tvmaniac_unrouted_presenters`. Default: empty. Comma-separated list of
 * simple class names. Each entry is matched against the annotated class's `simpleName`. Use this
 * list for child presenters that are exposed through a manual `@GraphExtension` rather than
 * routed by the codegen system, for example tab-internal sub-presenters or pager children.
 *
 * ## Example
 *
 * ```
 * [*.{kt,kts}]
 * ktlint_tvmaniac_unrouted_presenters = UpNextPresenter, CalendarPresenter
 * ```
 */
internal val UNROUTED_PRESENTERS_PROPERTY: EditorConfigProperty<Set<String>> =
    EditorConfigProperty(
        type = PropertyType(
            "ktlint_tvmaniac_unrouted_presenters",
            "Comma-separated simple class names for presenters that opt out of the codegen annotation requirement.",
            CommaSeparatedListValueParser(),
            emptySet<String>(),
        ),
        defaultValue = emptySet(),
        propertyWriter = { value -> value.joinToString(",") },
    )
