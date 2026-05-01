package io.github.thomaskioko.gradle.plugins.properties

internal object PropertyKeys {
    const val APP_DEBUG_ONLY: String = "app.debugOnly"
    const val APP_ENABLE_IOS: String = "app.enableIos"
    const val APP_VERSION_SUFFIX: String = "app.versionSuffix"

    const val COMPOSE_METRICS: String = "compose.enableCompilerMetrics"
    const val COMPOSE_REPORTS: String = "compose.enableCompilerReports"
    const val COMPOSE_COMPILER_REPORTS: String = "compose.enableComposeCompilerReports"

    const val PACKAGE_NAME: String = "package.name"

    const val JAVA_TOOLCHAINS_STRICT: String = "java.toolchains.strict"

    const val RELEASE_TYPE: String = "type"
    const val RELEASE_BETA: String = "beta"
    const val RELEASE_INTERACTIVE: String = "i"
    const val RELEASE_DRY_RUN: String = "dryRun"

    const val RELEASE_STORE_FILE: String = "releaseStoreFile"
    const val RELEASE_STORE_PASSWORD: String = "releaseStorePassword"
    const val RELEASE_KEY_ALIAS: String = "releaseKeyAlias"
    const val RELEASE_KEY_PASSWORD: String = "releaseKeyPassword"
}
