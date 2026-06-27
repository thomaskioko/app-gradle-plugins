package io.github.thomaskioko.gradle.plugins.setup

import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import io.github.thomaskioko.gradle.plugins.utils.androidApp
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

internal fun Project.setupFirebase() {
    val hasGoogleServicesConfig =
        file("google-services.json").exists() ||
            file("src/debug/google-services.json").exists() ||
            file("src/release/google-services.json").exists()

    if (!hasGoogleServicesConfig) return

    plugins.apply("com.google.gms.google-services")
    plugins.apply("com.google.firebase.crashlytics")

    androidApp {
        val release = buildTypes.getByName("release") as ExtensionAware
        release.extensions.configure(CrashlyticsExtension::class.java) {
            it.mappingFileUploadEnabled = true
        }
    }
}
