package io.github.thomaskioko.gradle.plugins.properties

import io.github.thomaskioko.gradle.plugins.utils.booleanProperty
import io.github.thomaskioko.gradle.plugins.utils.stringProperty
import org.gradle.api.Project
import org.gradle.api.provider.Provider

public class ScaffoldProperties internal constructor(project: Project) {
    public val composeMetrics: Provider<Boolean> =
        project.booleanProperty(PropertyKeys.COMPOSE_METRICS, false)

    public val composeReports: Provider<Boolean> =
        project.booleanProperty(PropertyKeys.COMPOSE_REPORTS, false)

    public val composeCompilerReports: Provider<String> =
        project.stringProperty(PropertyKeys.COMPOSE_COMPILER_REPORTS)

    public val debugOnly: Provider<Boolean> =
        project.booleanProperty(PropertyKeys.APP_DEBUG_ONLY, false)

    public val enableIos: Provider<Boolean> =
        project.booleanProperty(PropertyKeys.APP_ENABLE_IOS, false)

    public val javaToolchainsStrict: Provider<Boolean> =
        project.booleanProperty(PropertyKeys.JAVA_TOOLCHAINS_STRICT, false)

    public val appVersionSuffix: Provider<String> =
        project.stringProperty(PropertyKeys.APP_VERSION_SUFFIX).orElse("-beta")

    public val packageName: Provider<String> =
        project.stringProperty(PropertyKeys.PACKAGE_NAME)

    public val releaseType: Provider<String> =
        project.stringProperty(PropertyKeys.RELEASE_TYPE).orElse("minor")

    public val releaseBeta: Provider<Boolean> =
        project.stringProperty(PropertyKeys.RELEASE_BETA).map { true }.orElse(false)

    public val releaseInteractive: Provider<Boolean> =
        project.stringProperty(PropertyKeys.RELEASE_INTERACTIVE).map { true }.orElse(false)

    public val releaseDryRun: Provider<Boolean> =
        project.stringProperty(PropertyKeys.RELEASE_DRY_RUN).map { true }.orElse(false)

    public val releaseStoreFile: Provider<String> =
        project.stringProperty(PropertyKeys.RELEASE_STORE_FILE)

    public val releaseStorePassword: Provider<String> =
        project.stringProperty(PropertyKeys.RELEASE_STORE_PASSWORD)

    public val releaseKeyAlias: Provider<String> =
        project.stringProperty(PropertyKeys.RELEASE_KEY_ALIAS)

    public val releaseKeyPassword: Provider<String> =
        project.stringProperty(PropertyKeys.RELEASE_KEY_PASSWORD)
}

internal fun Project.scaffoldProperties(): ScaffoldProperties {
    val existing = extensions.findByType(ScaffoldProperties::class.java)
    if (existing != null) return existing
    val instance = ScaffoldProperties(this)
    extensions.add(ScaffoldProperties::class.java, "scaffoldProperties", instance)
    return instance
}
