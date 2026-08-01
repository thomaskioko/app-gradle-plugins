/*
 * Copyright 2026 Thomas Kioko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thomaskioko.gradle.plugins.analysis

/**
 * Dependency exclusion lists used by the dependency-analysis plugin in `RootPlugin`.
 *
 * Centralized here as a pure data object so additions and removals do not touch plugin code,
 * and so the rationale per group lives next to the entries. Not exposed as a public DSL.
 * Internal scaffolding for the plugin suite.
 */
internal object AnalysisExclusions {
    // Dependencies this plugin suite adds to consumer modules itself. DAGP flags them in every
    // module that never names them in its own build script, so the suite suppresses the noise it
    // creates. Coordinates a consumer declares itself do not belong here; those go in the
    // consuming module through scaffold { ignoreUnusedDependencies(...) }.
    val incorrectConfiguration: List<String> = listOf(
        "org.jetbrains.kotlin:kotlin-stdlib",
        // KMP "intermediate" source sets (jvmAndroidMain) share dependencies across jvm and
        // android targets.
        "junit:junit",
        // Added globally by setupSerialization() and setupMetro() at commonMainImplementation
        // scope.
        "org.jetbrains.kotlinx:kotlinx-serialization-core",
        "dev.zacsweers.metro:runtime",
    )

    val unusedDependencies: List<String> = listOf(
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
