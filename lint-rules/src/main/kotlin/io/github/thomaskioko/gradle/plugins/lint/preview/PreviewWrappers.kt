package io.github.thomaskioko.gradle.plugins.lint.preview

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CommaSeparatedListValueParser
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.psi.KtFile

/**
 * `.editorconfig` property listing simple call names that count as redundant preview wrappers.
 *
 * Property key: `ktlint_tvmaniac_preview_wrappers`. Comma-separated. Each entry is matched as a
 * literal simple name against the call site inside a preview body. Whitespace is trimmed; blank
 * entries are ignored. Setting the property to `unset` (or to an empty value) disables simple
 * name matching entirely; the rule then relies on [PREVIEW_WRAPPER_PACKAGES_PROPERTY] alone, and
 * fires nowhere if both are empty.
 *
 * Default: `TvManiacTheme, TvManiacBackground, Surface, MaterialTheme`. The first two cover the
 * project's design system wrappers; the last two cover the generic Material wrappers most likely
 * to be reached for as a substitute.
 *
 * ## Example
 *
 * ```
 * [*.{kt,kts}]
 * ktlint_tvmaniac_preview_wrappers = TvManiacTheme, TvManiacBackground, Surface
 * ```
 */
internal val PREVIEW_WRAPPERS_PROPERTY: EditorConfigProperty<Set<String>> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "ktlint_tvmaniac_preview_wrappers",
            "Comma-separated simple names of composables that count as redundant preview wrappers.",
            CommaSeparatedListValueParser(),
            emptySet<String>(),
        ),
        defaultValue = setOf(
            "TvManiacTheme",
            "TvManiacBackground",
            "Surface",
            "MaterialTheme",
        ),
        propertyWriter = { value -> value.joinToString(",") },
    )

/**
 * `.editorconfig` property listing fully qualified name prefixes that mark a call as a redundant
 * preview wrapper, regardless of its simple name.
 *
 * Property key: `ktlint_tvmaniac_preview_wrapper_packages`. Default: empty. Comma-separated. The
 * rule walks the file's `import` directives to resolve each call's simple name to its fully
 * qualified name, then checks whether that FQN equals or starts with any configured prefix.
 *
 * This is the rename-resilience input. A rename of `TvManiacTheme` to `MyAppTheme` inside the
 * package `com.thomaskioko.tvmaniac.designsystem.theme` is still caught when that package is in
 * the prefix set, even if the new name is not in [PREVIEW_WRAPPERS_PROPERTY].
 *
 * Star imports cannot be resolved without type information, so a call whose binding comes from a
 * star import will not match a package prefix. List the symbol name explicitly via
 * [PREVIEW_WRAPPERS_PROPERTY] in that case.
 *
 * ## Example
 *
 * ```
 * [*.{kt,kts}]
 * ktlint_tvmaniac_preview_wrapper_packages = com.thomaskioko.tvmaniac.designsystem.theme,
 *   androidx.compose.material3.Surface
 * ```
 */
internal val PREVIEW_WRAPPER_PACKAGES_PROPERTY: EditorConfigProperty<Set<String>> =
    EditorConfigProperty(
        type = PropertyType.LowerCasingPropertyType(
            "ktlint_tvmaniac_preview_wrapper_packages",
            "Comma-separated fully qualified name prefixes whose imported symbols count as redundant preview wrappers.",
            CommaSeparatedListValueParser(),
            emptySet<String>(),
        ),
        defaultValue = emptySet(),
        propertyWriter = { value -> value.joinToString(",") },
    )

/**
 * Resolves [simpleName] to the fully qualified name it refers to in this file's `import` list.
 *
 * Each non star import is inspected in turn. The effective bound name is the import's alias when
 * one is present, otherwise the imported short name. The first import whose effective name equals
 * [simpleName] yields its imported FQN as a string. Star imports are skipped because their bound
 * names are not knowable without type resolution.
 *
 * Returns `null` when no matching import is found (the call may be from the same package or from
 * a star import). The caller should treat `null` as "cannot resolve" rather than as a definitive
 * negative match.
 *
 * @param simpleName The simple call name to resolve.
 */
internal fun KtFile.fqnFor(simpleName: String): String? {
    val directives = importList?.imports ?: return null
    for (directive in directives) {
        if (directive.isAllUnder) continue
        val fqName = directive.importedFqName ?: continue
        val effectiveName = directive.aliasName ?: fqName.shortName().asString()
        if (effectiveName == simpleName) return fqName.asString()
    }
    return null
}

/**
 * Returns `true` when this fully qualified name equals any [prefixes] entry exactly, or starts
 * with one followed by a `.` separator. Whitespace around each prefix is trimmed; blank prefixes
 * are ignored. Matching is case-sensitive.
 *
 * @param prefixes Fully qualified name prefixes resolved from
 *   [PREVIEW_WRAPPER_PACKAGES_PROPERTY]. Each entry may be a package name (such as
 *   `com.example.theme`) or a complete symbol FQN (such as `androidx.compose.material3.Surface`).
 */
internal fun String.startsWithAnyFqn(prefixes: Set<String>): Boolean =
    prefixes
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .any { prefix -> this == prefix || startsWith("$prefix.") }
