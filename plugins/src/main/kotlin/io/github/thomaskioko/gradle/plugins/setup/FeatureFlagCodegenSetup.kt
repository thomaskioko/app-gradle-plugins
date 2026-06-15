package io.github.thomaskioko.gradle.plugins.setup

import io.github.thomaskioko.gradle.plugins.utils.addImplementationDependency
import io.github.thomaskioko.gradle.plugins.utils.addKspDependencyForCommonMain
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import org.gradle.api.Project

/**
 * Wires the feature flag codegen tier into a consumer module.
 *
 * Resolves `codegen-featureflag-annotations` and `codegen-featureflag-processor` from the
 * consumer's version catalog, registers the processor against `kspCommonMain`, and adds the
 * annotation jar to `commonMainImplementation`. Applies Metro first when absent, since the
 * generated bindings reference `@Provides`, `@ContributesTo`, and `@SingleIn` from
 * `dev.zacsweers.metro`.
 *
 * Mirrors [setupCodegen] for the navigation tier — same shape, different artifact aliases.
 */
internal fun Project.setupFeatureFlagCodegen() {
    if (!plugins.hasPlugin("dev.zacsweers.metro")) {
        setupMetro()
    }

    setupKsp()
    addImplementationDependency(getDependency("codegen-featureflag-annotations"))
    addKspDependencyForCommonMain(getDependency("codegen-featureflag-processor"))
}
