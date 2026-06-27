package io.github.thomaskioko.gradle.plugins.analysis

/**
 * Dependency exclusion lists used by the dependency-analysis plugin in `RootPlugin`.
 *
 * Centralized here as a pure data object so additions and removals do not touch plugin code,
 * and so the rationale per group lives next to the entries. Not exposed as a public DSL.
 * Internal scaffolding for the plugin suite.
 */
internal object AnalysisExclusions {
    // False positives that incorrectly map standard libs to a different configuration.
    val incorrectConfiguration: List<String> = listOf(
        "org.jetbrains.kotlin:kotlin-stdlib",
        "androidx.core:core-ktx",
        "androidx.lifecycle:lifecycle-runtime-ktx",
        "io.coil-kt:coil-compose",
        // KMP "intermediate" source sets (jvmAndroidMain) share dependencies across jvm and
        // android targets.
        "junit:junit",
        // Added globally by setupSerialization() and setupMetro() at commonMainImplementation
        // scope.
        "org.jetbrains.kotlinx:kotlinx-serialization-core",
        "dev.zacsweers.metro:runtime",
    )

    // Libs flagged as unused but actually consumed transitively or via reflection / runtime lookup.
    val unusedDependencies: List<String> = listOf(
        "io.coil-kt:coil-compose",
        "io.coil-kt:coil-compose-base",
        "androidx.compose.foundation:foundation",
        "androidx.compose.material:material",
        // Members of the unittest bundle in libs.versions.toml.
        "app.cash.turbine:turbine",
        "org.jetbrains.kotlin:kotlin-test",
        "io.kotest:kotest-assertions-core",
        // Added by setupCodegen()
        "io.github.thomaskioko.gradle.plugins:codegen-annotations",
        // Added by setupFeatureFlagCodegen(); the @FeatureFlag annotations have source/binary
        // retention and are consumed by KSP, so DAGP cannot see them in bytecode.
        "io.github.thomaskioko.gradle.plugins:codegen-featureflag-annotations",
        // setupMetro() / setupCodegen() add metro-runtime
        "dev.zacsweers.metro:runtime",
        // Added by setupResourceGenerator() / Moko Resources plugin.
        "dev.icerock.moko:resources-compose",
    )

    // Libs declared transitively but used directly. Flagged as "used transitive". Suppressed
    // because they are stable and the noise outweighs the cleanup value.
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
        "org.jetbrains.kotlinx:kotlinx-serialization-core",
        "org.jetbrains.kotlinx:kotlinx-serialization-json",
        "app.cash.sqldelight:android-driver",
        "app.cash.sqldelight:runtime",
        "app.cash.sqldelight:sqlite-driver",
        "com.arkivanov.decompose:decompose",
        // Decompose / Essenty sub-artifacts reached transitively via decompose-decompose.
        "com.arkivanov.essenty:back-handler",
        "com.arkivanov.essenty:instance-keeper",
        "com.arkivanov.essenty:state-keeper",
        "com.arkivanov.essenty:lifecycle",
        // Ktor sub-artifacts pulled in transitively via ktor-client-core.
        "io.ktor:ktor-http",
        "io.ktor:ktor-utils",
        "io.ktor:ktor-serialization",
        // Test dependencies
        "junit:junit",
        "androidx.test.ext:junit",
        "io.kotest:kotest-assertions-shared",
    )

    // KMP test source sets where DAGP's "incorrect configuration" advice produces
    // api(...) recommendations that Kotlin warns are unsupported and slated for removal.
    val ignoredIncorrectConfigurationSourceSets: List<String> = listOf(
        "commonTest",
        "jvmTest",
        "iosArm64Test",
        "iosSimulatorArm64Test",
        "iosX64Test",
        "androidHostTest",
        "androidDeviceTest",
        "androidUnitTest",
        "androidInstrumentedTest",
        "test",
        "androidTest",
    )

    // KMP source sets where DAGP's "used transitively, declare directly" advice fires for
    // dependencies already declared in commonMain. dependency-analysis 3.16.0 extends this to the
    // android variant, mirroring the commonMain "unused" reports (see below) into androidMain /
    // androidDeviceTest "declare directly" advice.
    val ignoredUsedTransitiveSourceSets: List<String> = listOf(
        "jvmMain",
        "jvmTest",
        "androidHostTest",
        "androidMain",
        "androidDeviceTest",
    )

    // KMP source sets where dependency-analysis 3.16.0 reports shared project dependencies as
    // unused: it no longer merges commonMain / intermediate source-set usage into the android
    // variant analysis, so dependencies used from common code (or shared test fixtures) surface as
    // unused with mirrored "declare in androidMain" advice. Applying that advice breaks compilation.
    val ignoredUnusedDependencySourceSets: List<String> = listOf(
        "commonMain",
        "commonTest",
        "jvmAndroidMain",
    )
}
