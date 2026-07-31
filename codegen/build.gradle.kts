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
import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.dokka.gradle.DokkaExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
    alias(libs.plugins.dependency.analysis)
}

val ktlintVersion = libs.versions.ktlint.get()
val licenseHeader = rootProject.file("../spotless/spotless.kt")
val ktsLicenseDelimiter = "(import|plugins|pluginManagement|dependencyResolutionManagement)"

spotless {
    kotlinGradle {
        ktlint(ktlintVersion)
        target("*.kts")
        licenseHeaderFile(licenseHeader, ktsLicenseDelimiter)
    }
}

tasks.register("spotlessApplyAll") {
    group = "formatting"
    description = "Run spotlessApply on this build and every module in it."
    dependsOn("spotlessApply")
    dependsOn(subprojects.map { "${it.path}:spotlessApply" })
}

tasks.register("spotlessCheckAll") {
    group = "verification"
    description = "Run spotlessCheck on this build and every module in it."
    dependsOn("spotlessCheck")
    dependsOn(subprojects.map { "${it.path}:spotlessCheck" })
}

dependencies {
    dokka(project(":annotations"))
    dokka(project(":featureflag-annotations"))
    dokka(project(":processor"))
    dokka(project(":featureflag-processor"))
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(rootProject.file("../docs/api/codegen"))
        suppressInheritedMembers.set(true)
        failOnWarning.set(false)
    }
}

subprojects {
    group = property("GROUP").toString()
    version = property("VERSION_NAME").toString()

    pluginManager.apply("com.autonomousapps.dependency-analysis")
    pluginManager.apply("com.diffplug.spotless")

    val moduleDir = projectDir
    val moduleName = name

    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        pluginManager.apply("org.jetbrains.dokka")

        configure<DokkaExtension> {
            dokkaSourceSets.configureEach {
                if (name.startsWith("ios")) {
                    suppress.set(true)
                }

                includes.from("MODULE.md")
                jdkVersion.set(libs.versions.java.target.get().toInt())
                skipDeprecated.set(true)
                reportUndocumented.set(true)

                sourceLink {
                    localDirectory.set(moduleDir)
                    remoteUrl("${property("POM_SCM_URL")}/blob/main/codegen/$moduleName")
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }

    configure<SpotlessExtension> {
        kotlin {
            ktlint(ktlintVersion).editorConfigOverride(mapOf("android" to "true"))
            target("src/**/*.kt")
            targetExclude("**/resources/**", "**/build/**")
            licenseHeaderFile(licenseHeader)
        }
        kotlinGradle {
            ktlint(ktlintVersion)
            target("*.kts")
            licenseHeaderFile(licenseHeader, ktsLicenseDelimiter)
        }
    }
}
