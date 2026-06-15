package io.github.thomaskioko.codegen.annotations

/**
 * Decorates a `@Qualifier`-annotated annotation class to declare a feature flag, so the codegen
 * processor can emit the Metro binding pair that wires the flag through `FeatureFlagFactory`.
 *
 * Without this annotation the consumer hand-writes two `@Provides` functions per flag inside a
 * `FlagBindings` interface: one returning the qualified `FeatureFlag<Boolean>` via
 * `factory.boolean(...)`, and one binding that same instance into the
 * `Set<FeatureFlag<Boolean>>` multibinding the debug screen iterates. With this annotation the
 * processor emits both functions next to the qualifier declaration.
 *
 * ## Example
 *
 * ```kotlin
 * @Qualifier
 * @FeatureFlag(
 *     key = "enable_continue_watching_nitro",
 *     title = "Progress Endpoint",
 *     description = "Use Trakt's internal /sync/progress/up_next_nitro call instead of the documented multi-step progress fetch.",
 *     defaultValue = false,
 *     dateAdded = "2026-05-20",
 * )
 * annotation class ContinueWatchingNitroFlagQualifier
 * ```
 *
 * The processor emits one file (`<QualifierBaseName>Binding.kt`) into the qualifier's package. The
 * file contains a `@ContributesTo(AppScope::class)` interface declaring two `@Provides` methods:
 * one qualified factory call and one `@IntoSet` rebind into `Set<FeatureFlag<Boolean>>`.
 *
 * The `<QualifierBaseName>` is the qualifier's `simpleName` with a trailing `Qualifier` suffix
 * stripped if present. `ContinueWatchingNitroFlagQualifier` becomes `ContinueWatchingNitroFlag`;
 * `MyFlag` (no suffix) stays as `MyFlag`. The emitted interface is named `<QualifierBaseName>Binding`.
 *
 * ## Validation
 *
 * The processor reports a compile error if any of the following hold:
 *
 * - The annotated symbol is not an `annotation class`.
 * - The annotated class does not also carry `@dev.zacsweers.metro.Qualifier`.
 * - [key] is blank.
 * - [title] is blank.
 * - [dateAdded] is not a valid ISO `YYYY-MM-DD` date.
 *
 * @property key Firebase Remote Config key. Drives both Firebase lookups and debug-store overrides.
 * @property title Human-readable name shown on the debug screen row.
 * @property description One-line summary shown beneath the title on the debug screen.
 * @property defaultValue Fallback returned until Firebase serves an explicit value.
 * @property dateAdded ISO `YYYY-MM-DD` date the flag entered the codebase. Drives the debug
 *   screen's "Date Added" sort. Stored as `String` because Kotlin annotations cannot accept
 *   `LocalDate`; the processor parses to `kotlinx.datetime.LocalDate` at codegen time.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class FeatureFlag(
    val key: String,
    val title: String,
    val description: String,
    val defaultValue: Boolean,
    val dateAdded: String,
)
