package io.github.thomaskioko.codegen.annotations

/**
 * Decorates an anchor declaration (a public `object`) to declare a feature flag, so the codegen
 * processor can emit both the Metro `@Qualifier` annotation and the binding pair that wires the
 * flag through `FeatureFlagFactory`.
 *
 * Without this annotation the consumer hand-writes a `@Qualifier` annotation class plus two
 * `@Provides` functions per flag inside a `FlagBindings` interface: one returning the qualified
 * `FeatureFlag<Boolean>` via `factory.boolean(...)`, and one binding that same instance into the
 * `Set<FeatureFlag<Boolean>>` multibinding the debug screen iterates. With this annotation the
 * processor emits the qualifier and both functions from the single annotated anchor.
 *
 * ## Example
 *
 * ```kotlin
 * @FeatureFlag(
 *     key = "enable_continue_watching_nitro",
 *     title = "Progress Endpoint",
 *     description = "Use Trakt's internal /sync/progress/up_next_nitro call instead of the documented multi-step progress fetch.",
 *     defaultValue = false,
 *     dateAdded = "2026-05-20",
 * )
 * object ContinueWatchingNitroFlag
 * ```
 *
 * The processor emits two files into the anchor's package and source set:
 *
 * - `<BaseName>Qualifier.kt` — a Metro `@Qualifier` annotation class with default targets.
 * - `<BaseName>Binding.kt` — a `@ContributesTo(AppScope::class)` interface declaring two
 *   `@Provides` methods: one qualified factory call and one `@IntoSet` rebind into
 *   `Set<FeatureFlag<Boolean>>`.
 *
 * The `<BaseName>` is the anchor's `simpleName` verbatim. `ContinueWatchingNitroFlag` produces the
 * qualifier `ContinueWatchingNitroFlagQualifier` and the interface `ContinueWatchingNitroFlagBinding`.
 * Do not suffix the anchor with `Qualifier`, which would emit a doubled `QualifierQualifier`.
 *
 * ## Validation
 *
 * The processor reports a compile error if any of the following hold:
 *
 * - The annotated symbol is not a class, object, or interface (annotation classes are rejected).
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
 * @property platform Platform the flag is generated into. Defaults to [Platform.ALL] (both graphs).
 *   Set [Platform.IOS] or [Platform.JVM] to scope the flag to one platform at compile time; the
 *   anchor stays in `commonMain` either way. See [Platform].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class FeatureFlag(
    val key: String,
    val title: String,
    val description: String,
    val defaultValue: Boolean,
    val dateAdded: String,
    val platform: Platform = Platform.ALL,
)
