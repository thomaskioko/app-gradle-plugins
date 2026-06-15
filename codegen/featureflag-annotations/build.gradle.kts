plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.publish)
}

group = property("GROUP").toString()
version = property("VERSION_NAME").toString()

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    explicitApi()
}

mavenPublishing {
    coordinates(artifactId = "codegen-featureflag-annotations")
    publishToMavenCentral(automaticRelease = true)
    if (findProperty("maven.central.publish")?.toString() == "true") {
        signAllPublications()
    }

    pom {
        name.set(property("POM_NAME").toString())
        description.set("Feature flag codegen annotations consumed by KMP modules")
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
