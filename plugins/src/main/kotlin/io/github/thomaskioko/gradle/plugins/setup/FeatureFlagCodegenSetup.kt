package io.github.thomaskioko.gradle.plugins.setup

import io.github.thomaskioko.gradle.plugins.utils.addImplementationDependency
import io.github.thomaskioko.gradle.plugins.utils.addKspDependencyForAllTargets
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import org.gradle.api.Project

/**
 * Wires the feature flag codegen tier into a consumer module.
 *
 * Resolves `codegen-featureflag-annotations` and `codegen-featureflag-processor` from the
 * consumer's version catalog, registers the processor against the `commonMain` metadata run and
 * every per-target run, and adds the annotation jar to `commonMainImplementation`. Applies Metro
 * first when absent, since the generated bindings reference `@Provides`, `@ContributesTo`, and
 * `@SingleIn` from `dev.zacsweers.metro`.
 *
 * Unlike [setupCodegen], which keeps the navigation processor on `commonMain` only, this tier uses
 * [addKspDependencyForAllTargets] so an anchor placed in a platform source set (`iosMain`,
 * `androidMain`) is generated into that platform's compilation. The processor's source-set guard
 * keeps each anchor generated exactly once, so attaching to every run does not redeclare the
 * `commonMain` anchors. The all-targets call rewrites the `metadata` target to
 * `kspCommonMainMetadata`, so it replaces rather than supplements the `commonMain` wiring.
 */
internal fun Project.setupFeatureFlagCodegen() {
    if (!plugins.hasPlugin("dev.zacsweers.metro")) {
        setupMetro()
    }

    setupKsp()
    addImplementationDependency(getDependency("codegen-featureflag-annotations"))
    addKspDependencyForAllTargets(getDependency("codegen-featureflag-processor"))
}
