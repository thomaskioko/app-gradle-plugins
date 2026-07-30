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
package io.github.thomaskioko.codegen.featureflag.processor.util

import com.squareup.kotlinpoet.ClassName

/*
 * Fully qualified `ClassName` constants for every type the generated code references in the
 * consumer project. Used by [FeatureFlagBindingGenerator] so the names live in one place.
 *
 * These are constants rather than configurable processor options. The rationale matches the
 * navigation processor's `External.kt`: a fork that changes the consumer's feature flag primitives
 * edits this file and the matching test stubs in `processor-test/`. The constants are deliberately
 * `internal` and impossible to override without a fork, which keeps the upstream processor
 * opinionated and small.
 *
 * The groupings below match the role each name plays at runtime.
 */

// Consumer feature flag primitives. The generated binding references the generic interface
// (FeatureFlag<Boolean>) and the construction surface (FeatureFlagFactory). The interface is the
// type consumers inject; the factory builds the underlying RemoteFlag through the consumer's
// FeatureFlagsRemoteConfig wiring.
internal const val FEATURE_FLAGS_PACKAGE: String = "com.thomaskioko.tvmaniac.featureflags"
internal val FeatureFlag: ClassName = ClassName(FEATURE_FLAGS_PACKAGE, "FeatureFlag")
internal val FeatureFlagFactory: ClassName = ClassName(FEATURE_FLAGS_PACKAGE, "FeatureFlagFactory")

// Metro: the dependency injection framework the codegen targets. Every generated annotation
// references one of these. AppScope is the parent dependency injection scope the generated
// interface contributes into.
internal const val METRO_PACKAGE: String = "dev.zacsweers.metro"
internal val AppScope: ClassName = ClassName(METRO_PACKAGE, "AppScope")
internal val ContributesTo: ClassName = ClassName(METRO_PACKAGE, "ContributesTo")
internal val IntoSet: ClassName = ClassName(METRO_PACKAGE, "IntoSet")
internal val Provides: ClassName = ClassName(METRO_PACKAGE, "Provides")
internal val Qualifier: ClassName = ClassName(METRO_PACKAGE, "Qualifier")
internal val SingleIn: ClassName = ClassName(METRO_PACKAGE, "SingleIn")

// kotlinx.datetime: the LocalDate type the generated factory call passes for the `dateAdded`
// parameter. The processor parses the annotation's ISO String at codegen time and emits a
// LocalDate constructor call with year, month, day literals.
internal const val DATETIME_PACKAGE: String = "kotlinx.datetime"
internal val LocalDate: ClassName = ClassName(DATETIME_PACKAGE, "LocalDate")
