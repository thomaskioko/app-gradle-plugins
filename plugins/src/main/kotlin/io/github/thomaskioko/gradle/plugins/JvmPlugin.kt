package io.github.thomaskioko.gradle.plugins

import io.github.thomaskioko.gradle.plugins.extensions.JvmExtension
import io.github.thomaskioko.gradle.plugins.utils.baseExtension
import io.github.thomaskioko.gradle.plugins.utils.defaultTestSetup
import io.github.thomaskioko.gradle.plugins.utils.java
import io.github.thomaskioko.gradle.plugins.utils.javaTargetVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

public abstract class JvmPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.kotlin.jvm")
        target.plugins.apply(BasePlugin::class.java)

        target.baseExtension.extensions.create("jvm", JvmExtension::class.java)

        target.java {
            sourceCompatibility = target.javaTargetVersion.get()
            targetCompatibility = target.javaTargetVersion.get()
        }

        target.tasks.withType(JavaCompile::class.java).configureEach {
            it.options.release.set(target.javaTargetVersion.get().majorVersion.toInt())
        }

        target.tasks.withType(Test::class.java).configureEach(Test::defaultTestSetup)
    }
}
