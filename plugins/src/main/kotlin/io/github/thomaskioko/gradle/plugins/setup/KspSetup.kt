package io.github.thomaskioko.gradle.plugins.setup

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.process.CommandLineArgumentProvider

internal fun Project.setupKsp(arguments: List<CommandLineArgumentProvider> = emptyList()) {
    plugins.apply("com.google.devtools.ksp")

    extensions.configure(KspExtension::class.java) { extension ->
        arguments.forEach {
            extension.arg(it)
        }
    }
}
