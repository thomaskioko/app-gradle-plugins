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
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.publish) apply false

    alias(libs.plugins.app.root)
    alias(libs.plugins.spotless)
    alias(libs.plugins.app.spotless)
}

spotless {
    kotlinGradle {
        target("*.kts")
        licenseHeaderFile(
            rootProject.file("spotless/spotless.kt"),
            "(import|plugins|pluginManagement|dependencyResolutionManagement)",
        )
    }
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publish plugins + codegen artifacts to mavenLocal."
    dependsOn(gradle.includedBuild("plugins").task(":publishToMavenLocal"))
    dependsOn(gradle.includedBuild("codegen").task(":annotations:publishToMavenLocal"))
    dependsOn(gradle.includedBuild("codegen").task(":processor:publishToMavenLocal"))
    dependsOn(gradle.includedBuild("codegen").task(":featureflag-annotations:publishToMavenLocal"))
    dependsOn(gradle.includedBuild("codegen").task(":featureflag-processor:publishToMavenLocal"))
    dependsOn(gradle.includedBuild("lint-rules").task(":publishToMavenLocal"))
}

tasks.register("dokkaAll") {
    group = "documentation"
    description = "Generate the API reference across plugins + lint-rules + codegen composite builds."
    dependsOn(gradle.includedBuild("plugins").task(":dokkaGenerate"))
    dependsOn(gradle.includedBuild("lint-rules").task(":dokkaGenerate"))
    dependsOn(gradle.includedBuild("codegen").task(":dokkaGenerate"))
}

tasks.register("buildHealthAll") {
    group = "verification"
    description = "Run buildHealth across plugins + lint-rules + codegen composite builds."
    dependsOn(gradle.includedBuild("plugins").task(":buildHealth"))
    dependsOn(gradle.includedBuild("lint-rules").task(":buildHealth"))
    dependsOn(gradle.includedBuild("codegen").task(":buildHealth"))
}

tasks.register("spotlessApplyAll") {
    group = "formatting"
    description = "Run spotlessApply across root + plugins + lint-rules + codegen composite builds."
    dependsOn("spotlessApply")
    dependsOn(gradle.includedBuild("plugins").task(":spotlessApply"))
    dependsOn(gradle.includedBuild("lint-rules").task(":spotlessApply"))
    dependsOn(gradle.includedBuild("codegen").task(":spotlessApplyAll"))
}

tasks.register("spotlessCheckAll") {
    group = "verification"
    description = "Run spotlessCheck across root + plugins + lint-rules + codegen composite builds."
    dependsOn("spotlessCheck")
    dependsOn(gradle.includedBuild("plugins").task(":spotlessCheck"))
    dependsOn(gradle.includedBuild("lint-rules").task(":spotlessCheck"))
    dependsOn(gradle.includedBuild("codegen").task(":spotlessCheckAll"))
}
