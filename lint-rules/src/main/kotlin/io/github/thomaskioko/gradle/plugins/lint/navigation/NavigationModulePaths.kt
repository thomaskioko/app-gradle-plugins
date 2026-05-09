package io.github.thomaskioko.gradle.plugins.lint.navigation

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CommaSeparatedListValueParser
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType

/**
 * `.editorconfig` property identifying which path segments mark the navigation layer.
 *
 * Property key: `ktlint_tvmaniac_navigation_module_paths`. Default: `navigation`. Comma-separated
 * list. Each entry is matched against the file path as a `/`-bounded fragment, so an entry of
 * `routing` matches any file under a directory named `routing`. Leading and trailing slashes are
 * stripped before matching, blank entries are ignored, and matching is case-sensitive.
 *
 * Multi-segment entries such as `feature/nav` are kept verbatim and matched as `/feature/nav/`.
 *
 * Setting the property to `unset` (or to an empty value) makes the rules treat no path as part of
 * the navigation layer; rules then fire everywhere their primary check matches.
 *
 * ## Example
 *
 * ```
 * [*.{kt,kts}]
 * ktlint_tvmaniac_navigation_module_paths = navigation, routing
 * ```
 */
internal val NAVIGATION_MODULE_PATHS_PROPERTY: EditorConfigProperty<Set<String>> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "ktlint_tvmaniac_navigation_module_paths",
            "Comma-separated path fragments that identify modules forming the navigation layer.",
            CommaSeparatedListValueParser(),
            emptySet<String>(),
        ),
        defaultValue = setOf("navigation"),
        propertyWriter = { value -> value.joinToString(",") },
    )

/**
 * Returns `true` if this file path lives inside a module identified as part of the navigation
 * layer by [modulePaths].
 *
 * Each entry in [modulePaths] is normalised by trimming whitespace and surrounding slashes, then
 * wrapped as `/$entry/` for substring matching. Blank entries are dropped. An empty
 * [modulePaths] returns `false` for every input.
 *
 * @param modulePaths Path fragments resolved from [NAVIGATION_MODULE_PATHS_PROPERTY].
 */
internal fun String.isInNavigationModule(modulePaths: Set<String>): Boolean =
    modulePaths
        .asSequence()
        .mapNotNull(::normalizeModuleSegment)
        .any { fragment -> contains(fragment) }

private fun normalizeModuleSegment(raw: String): String? =
    raw
        .trim()
        .trim('/')
        .takeIf { it.isNotBlank() }
        ?.let { "/$it/" }
