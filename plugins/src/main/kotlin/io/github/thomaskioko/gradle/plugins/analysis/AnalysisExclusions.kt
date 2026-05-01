package io.github.thomaskioko.gradle.plugins.analysis

/**
 * Dependency exclusion lists used by the dependency-analysis plugin in `RootPlugin`.
 *
 * Centralized here as a pure data object so additions/removals don't require touching plugin
 * code, and so the rationale per group lives next to the entries. Not exposed as a public DSL —
 * this is internal scaffolding for the plugin suite.
 */
internal object AnalysisExclusions {
    // False positives that incorrectly map standard libs to a different configuration.
    val incorrectConfiguration: List<String> = listOf(
        "org.jetbrains.kotlin:kotlin-stdlib",
        "androidx.core:core-ktx",
        "androidx.lifecycle:lifecycle-runtime-ktx",
        "io.coil-kt:coil-compose",
    )

    // Libs flagged as unused but actually consumed transitively or via reflection / runtime lookup.
    val unusedDependencies: List<String> = listOf(
        "io.coil-kt:coil-compose",
        "io.coil-kt:coil-compose-base",
        "androidx.compose.foundation:foundation",
        "androidx.compose.material:material",
    )

    // Libs declared transitively but used directly — flagged as "used transitive". Suppressed
    // because they're stable and the noise outweighs the cleanup value.
    val usedTransitive: List<String> = listOf(
        // Common Kotlin dependencies
        "org.jetbrains.kotlin:kotlin-stdlib",
        // Common Compose dependencies
        "androidx.compose.material:material",
        "androidx.compose.animation:animation",
        "androidx.compose.material:material-icons-core",
        "androidx.compose.ui:ui-tooling-preview",
        "androidx.compose.ui:ui",
        // Common libraries
        "androidx.lifecycle:lifecycle-runtime-compose",
        "androidx.lifecycle:lifecycle-runtime",
        "org.jetbrains.kotlinx:kotlinx-collections-immutable",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core",
        "dev.icerock.moko:resources",
        "dev.icerock.moko:resources-compose",
        "androidx.compose.material:material-icons-extended",
        // Common Android libraries
        "androidx.activity:activity",
        "androidx.paging:paging-common",
        "androidx.sqlite:sqlite",
        "androidx.datastore:datastore-core",
        // Network / serialization / database
        "com.squareup.okhttp3:okhttp",
        "org.jetbrains.kotlinx:kotlinx-serialization-json",
        "app.cash.sqldelight:android-driver",
        "app.cash.sqldelight:runtime",
        "app.cash.sqldelight:sqlite-driver",
        "com.arkivanov.decompose:decompose",
        // Test dependencies
        "junit:junit",
        "androidx.test.ext:junit",
        "io.kotest:kotest-assertions-shared",
    )
}
