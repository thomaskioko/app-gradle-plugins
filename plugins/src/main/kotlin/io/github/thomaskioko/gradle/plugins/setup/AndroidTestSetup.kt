package io.github.thomaskioko.gradle.plugins.setup

import com.android.build.api.dsl.TestExtension
import com.android.build.api.dsl.TestOptions
import io.github.thomaskioko.gradle.plugins.utils.androidApp
import io.github.thomaskioko.gradle.plugins.utils.androidLibrary
import org.gradle.api.Project

/**
 * Applies the given configuration block to the active Android plugin's `testOptions`.
 *
 * Routes to the right extension type based on which Android plugin is applied
 * (`com.android.application`, `com.android.library`, or `com.android.test`).
 * Modules without any Android plugin applied are no-ops.
 */
internal fun Project.configureTestOptions(block: TestOptions.() -> Unit) {
    when {
        plugins.hasPlugin("com.android.application") ->
            androidApp { testOptions(block) }

        plugins.hasPlugin("com.android.library") ->
            androidLibrary { testOptions(block) }

        plugins.hasPlugin("com.android.test") ->
            extensions.configure(TestExtension::class.java) { it.testOptions(block) }
    }
}
