plugins {
    id("io.github.thomaskioko.gradle.plugins.multiplatform")
}

scaffold {
    useMetro()
    useFeatureFlagCodegen()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}
