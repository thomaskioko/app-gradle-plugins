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
        type = PropertyType.LowerCasingPropertyType(
            "ktlint_tvmaniac_unrouted_presenters",
            "Comma-separated simple class names for presenters that opt out of the codegen annotation requirement.",
            CommaSeparatedListValueParser(),
            emptySet<String>(),
        ),
        defaultValue = emptySet(),
        propertyWriter = { value -> value.joinToString(",") },
    )

/**
 * `.editorconfig` property identifying Compose screens that intentionally do not carry a
 * navigation codegen UI annotation.
 *
 * Property key: `ktlint_tvmaniac_unrouted_screens`. Default: empty. Comma-separated list of
 * simple function names. Use this list for screens dispatched manually inside a parent host
 * (typically tab-root screens invoked from the home host's `Children` block) rather than
 * resolved through `Set<ScreenContent>`.
 *
 * ## Example
 *
 * ```
 * [*.{kt,kts}]
 * ktlint_tvmaniac_unrouted_screens = DiscoverScreen, LibraryScreen, ProfileScreen, ProgressScreen
 * ```
 */
internal val UNROUTED_SCREENS_PROPERTY: EditorConfigProperty<Set<String>> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "ktlint_tvmaniac_unrouted_screens",
            "Comma-separated simple function names for Compose screens that opt out of the codegen UI annotation requirement.",
            CommaSeparatedListValueParser(),
            emptySet<String>(),
        ),
        defaultValue = emptySet(),
        propertyWriter = { value -> value.joinToString(",") },
    )
