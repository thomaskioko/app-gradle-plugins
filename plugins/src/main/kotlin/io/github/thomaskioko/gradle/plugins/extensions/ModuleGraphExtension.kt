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
package io.github.thomaskioko.gradle.plugins.extensions

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Controls the Mermaid dependency diagram written by the `graphDump` and `graphUpdate` tasks.
 *
 * Registered on the root project by [io.github.thomaskioko.gradle.plugins.RootPlugin], so the
 * block belongs in the root build file rather than in a module. `graphDump` writes the diagram
 * and `graphUpdate` rewrites the copy held under version control, which makes an unintended
 * change to the dependency tree show up as a diff in review.
 *
 * A diagram of every module in a large project is unreadable, so the defaults leave out the
 * modules a reader is not looking for and count only the configurations that carry a real
 * dependency.
 *
 * ```kotlin
 * // root build.gradle.kts
 * moduleGraph {
 *   ignore(":benchmark", ":sample")
 * }
 * ```
 */
@ScaffoldDsl
public abstract class ModuleGraphExtension {
    /**
     * Leaves out every project whose path matches this expression.
     *
     * Defaults to `.*:testing$`, which drops the fake implementations each data module publishes
     * for its own tests. Those depend on nearly everything and connect to nearly nothing, so they
     * add noise to the diagram without saying anything about how the project is put together.
     */
    public abstract val ignoredProjectsRegex: Property<String>

    /**
     * Leaves out the named projects, by exact path.
     *
     * Prefer [ignore] for adding to this set. Set the property directly only when the list is
     * built somewhere else, such as from a property or another task's output.
     */
    public abstract val ignoredProjects: SetProperty<String>

    /**
     * Configurations that count as a dependency between two modules.
     *
     * Defaults to `commonMainApi`, `commonMainImplementation`, `api` and `implementation`, which
     * covers a Kotlin Multiplatform module and a plain one. Test and compile-only configurations
     * are deliberately absent: a test fixture is not a statement about how the production code is
     * arranged, which is what the diagram is for.
     */
    public abstract val supportedConfigurations: SetProperty<String>

    init {
        ignoredProjectsRegex.convention(".*:testing$")
        supportedConfigurations.convention(
            setOf("commonMainApi", "commonMainImplementation", "api", "implementation"),
        )
    }

    /**
     * Leaves the named projects out of the diagram.
     *
     * Adds to [ignoredProjects] rather than replacing it, so several calls accumulate and the
     * default [ignoredProjectsRegex] still applies.
     *
     * @param projectPaths Gradle project paths, for example `":benchmark"`.
     */
    public fun ignore(vararg projectPaths: String) {
        ignoredProjects.addAll(*projectPaths)
    }
}
