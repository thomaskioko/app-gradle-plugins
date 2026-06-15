package io.github.thomaskioko.codegen.featureflag.processor

import com.squareup.kotlinpoet.ClassName

/**
 * Typed intermediate representation of one `@FeatureFlag`-decorated qualifier annotation.
 *
 * Produced by [FeatureFlagCodegenProcessor] after parsing the annotation arguments and computing
 * the base name. Consumed by [FeatureFlagBindingGenerator] to emit the per-qualifier binding file.
 *
 * @property packageName Package of the annotated qualifier class. The generated binding file
 *   lives in the same package so it sits next to the qualifier declaration.
 * @property qualifierClassName Fully qualified name of the annotated qualifier class. Referenced
 *   by the generated `@Provides` function and the `@IntoSet` rebind parameter.
 * @property baseName The qualifier's simple name with a trailing `Qualifier` suffix removed (if
 *   present). Drives the generated interface name (`<baseName>Binding`) and the generated function
 *   names (`provide<baseName>`, `bind<baseName>`).
 * @property key Firebase Remote Config key. Passed verbatim to `factory.boolean(...)`.
 * @property title Human-readable name. Passed verbatim to `factory.boolean(...)`.
 * @property description One-line summary. Passed verbatim to `factory.boolean(...)`.
 * @property defaultValue Fallback Boolean. Passed verbatim to `factory.boolean(...)`.
 * @property year Year component of `dateAdded`. Emitted as a literal in the `LocalDate(...)` call.
 * @property month Month component of `dateAdded` (1..12). Emitted as a literal.
 * @property day Day-of-month component of `dateAdded` (1..31). Emitted as a literal.
 */
internal data class FeatureFlagData(
    val packageName: String,
    val qualifierClassName: ClassName,
    val baseName: String,
    val key: String,
    val title: String,
    val description: String,
    val defaultValue: Boolean,
    val year: Int,
    val month: Int,
    val day: Int,
)
