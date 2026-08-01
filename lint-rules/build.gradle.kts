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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.dokka)
    alias(libs.plugins.publish)
    alias(libs.plugins.spotless)
}

base {
    archivesName.set("lint-rules")
}

group = property("GROUP").toString()
version = property("VERSION_NAME").toString()

val ktlintVersion = libs.versions.ktlint.get()
val licenseHeader = rootProject.file("../spotless/spotless.kt")

spotless {
    kotlin {
        ktlint(ktlintVersion).editorConfigOverride(mapOf("android" to "true"))
        target("src/**/*.kt")
        targetExclude("**/resources/**", "**/build/**")
        licenseHeaderFile(licenseHeader)
    }
    kotlinGradle {
        ktlint(ktlintVersion)
        target("*.kts")
        licenseHeaderFile(licenseHeader, "(import|plugins|pluginManagement|dependencyResolutionManagement)")
    }
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("MODULE.md")
        jdkVersion.set(17)
        skipDeprecated.set(true)
        reportUndocumented.set(true)

        sourceLink {
            localDirectory.set(projectDir)
            remoteUrl("${property("POM_SCM_URL")}/blob/main/lint-rules")
            remoteLineSuffix.set("#L")
        }
    }

    dokkaPublications.html {
        outputDirectory.set(rootProject.file("../docs/api/lint-rules"))
        suppressInheritedMembers.set(true)
        failOnWarning.set(true)
    }
}

@OptIn(ExperimentalAbiValidation::class)
kotlin {
    abiValidation {}

    explicitApi()
    jvmToolchain(libs.versions.java.toolchain.get().toInt())
    compilerOptions {
        allWarningsAsErrors.set(true)
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.ktlint.rule.engine.core)
    implementation(libs.ktlint.cli.ruleset.core)

    lintChecks(libs.androidx.lint.gradle)

    testImplementation(libs.ktlint.test)
    testImplementation(libs.ktlint.rule.engine)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (findProperty("maven.central.publish")?.toString() == "true") {
        signAllPublications()
    }

    pom {
        name.set(property("POM_NAME").toString())
        description.set(property("POM_DESCRIPTION").toString())
        inceptionYear.set(property("INCEPTION_YEAR").toString())
        url.set(property("POM_URL").toString())

        licenses {
            license {
                name.set(property("POM_LICENSE_NAME").toString())
                url.set(property("POM_LICENSE_URL").toString())
                distribution.set(property("POM_LICENSE_DIST").toString())
            }
        }

        developers {
            developer {
                id.set(property("POM_DEVELOPER_ID").toString())
                name.set(property("POM_DEVELOPER_NAME").toString())
                url.set(property("POM_DEVELOPER_URL").toString())
            }
        }

        scm {
            url.set(property("POM_SCM_URL").toString())
            connection.set(property("POM_SCM_CONNECTION").toString())
            developerConnection.set(property("POM_SCM_DEV_CONNECTION").toString())
        }
    }
}
