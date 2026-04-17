import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
}

base {
    archivesName.set("codegen-processor")
}

kotlin {
    explicitApi()
    jvmToolchain(libs.versions.java.toolchain.get().toInt())
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.target.get()))
    }
}

java {
    targetCompatibility = JavaVersion.toVersion(libs.versions.java.target.get())
    sourceCompatibility = JavaVersion.toVersion(libs.versions.java.target.get())
}

dependencies {
    api(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (findProperty("maven.central.publish")?.toString() == "true") {
        signAllPublications()
    }

    pom {
        name.set(property("POM_NAME").toString())
        description.set("KSP processor for generating navigation destination bindings")
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
