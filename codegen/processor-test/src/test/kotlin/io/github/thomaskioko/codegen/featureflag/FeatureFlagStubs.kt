package io.github.thomaskioko.codegen.featureflag

/**
 * Minimal source-level fakes of the consumer project types the feature flag generator references.
 *
 * The processor resolves type names against whatever is on the test compilation's classpath, so
 * each test must compile alongside source files that declare the consumer's `FeatureFlag` and
 * `FeatureFlagFactory` interfaces, the Metro `Qualifier`/`AppScope`/`@Provides`/`@IntoSet`/`@SingleIn`/`@ContributesTo`
 * annotations, and `kotlinx.datetime.LocalDate`. Bundling these as test sources rather than
 * depending on the real consumer jars keeps the test module hermetic and lets the compiler type
 * check the generated output in isolation.
 *
 * The type signatures here must match the constants in
 * `codegen/featureflag-processor/src/main/kotlin/io/github/thomaskioko/codegen/featureflag/processor/util/External.kt`
 * exactly. If the consumer's primitives change, both files must be updated together.
 */
internal object FeatureFlagStubs {

    val metroAnnotations = "MetroAnnotations.kt" to """
        package dev.zacsweers.metro

        import kotlin.reflect.KClass

        public abstract class AppScope private constructor()

        @Target(AnnotationTarget.ANNOTATION_CLASS)
        public annotation class Qualifier

        @Target(AnnotationTarget.CLASS)
        public annotation class ContributesTo(val scope: KClass<*>)

        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
        public annotation class Provides

        @Target(AnnotationTarget.FUNCTION)
        public annotation class IntoSet

        @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
        public annotation class SingleIn(val scope: KClass<*>)
    """.trimIndent()

    val kotlinxDatetime = "LocalDate.kt" to """
        package kotlinx.datetime

        public class LocalDate(
            public val year: Int,
            public val month: Int,
            public val day: Int,
        )
    """.trimIndent()

    val featureFlagApi = "FeatureFlag.kt" to """
        package com.thomaskioko.tvmaniac.featureflags

        import kotlinx.datetime.LocalDate

        public interface FeatureFlag<T>

        public interface FeatureFlagFactory {
            public fun boolean(
                key: String,
                title: String,
                description: String,
                defaultValue: Boolean,
                dateAdded: LocalDate,
            ): FeatureFlag<Boolean>
        }
    """.trimIndent()

    val baseStubs: List<Pair<String, String>> = listOf(
        metroAnnotations,
        kotlinxDatetime,
        featureFlagApi,
    )
}
