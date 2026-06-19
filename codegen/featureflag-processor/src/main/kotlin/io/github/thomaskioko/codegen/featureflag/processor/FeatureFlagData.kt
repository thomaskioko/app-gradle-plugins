package io.github.thomaskioko.codegen.featureflag.processor

import com.squareup.kotlinpoet.ClassName

/**
 * Typed intermediate representation of one `@FeatureFlag`-decorated anchor.
 *
 * Produced by [FeatureFlagCodegenProcessor] after parsing the annotation arguments and computing
 * the base name. Consumed by [FeatureFlagQualifierGenerator] to emit the qualifier file and by
 * [FeatureFlagBindingGenerator] to emit the binding file.
 *
 * @property packageName Package of the annotated anchor. The generated qualifier and binding files
 *   live in the same package so they sit next to the anchor declaration.
 * @property qualifierClassName Fully qualified name of the generated qualifier annotation
 *   (`<packageName>.<baseName>Qualifier`). Referenced by the generated `@Provides` function and the
 *   `@IntoSet` rebind parameter.
 * @property baseName The anchor's simple name verbatim. Drives the generated qualifier name
 *   (`<baseName>Qualifier`), the generated interface name (`<baseName>Binding`), and the generated
 *   function names (`provide<baseName>`, `bind<baseName>`).
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
