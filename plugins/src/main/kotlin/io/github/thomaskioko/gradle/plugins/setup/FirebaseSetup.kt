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
