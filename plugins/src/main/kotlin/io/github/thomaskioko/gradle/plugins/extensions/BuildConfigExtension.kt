package io.github.thomaskioko.gradle.plugins.extensions

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import java.util.Properties

public abstract class BuildConfigExtension(private val project: Project) {

    /**
     * The package name for the generated BuildConfig class.
     * Example: "com.thomaskioko.tvmaniac.core.base"
     */
    public abstract val packageName: Property<String>

    /**
     * Custom string fields to add to BuildConfig.
     * Key = constant name, Value = constant value
     */
    public abstract val stringFields: MapProperty<String, String>

    /**
     * Custom boolean fields to add to BuildConfig.
     * Key = constant name, Value = constant value
     */
    public abstract val booleanFields: MapProperty<String, Boolean>

    /**
     * Custom int fields to add to BuildConfig.
     */
    public abstract val intFields: MapProperty<String, Int>

    private val localProperties: Properties by lazy {
        val props = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { props.load(it) }
        }
        props
    }

    init {
        stringFields.convention(emptyMap())
        booleanFields.convention(emptyMap())
        intFields.convention(emptyMap())
    }

    /**
     * Add a custom string constant to BuildConfig.
     */
    public fun stringField(name: String, value: String) {
        stringFields.put(name, value)
    }

    /**
     * Add a custom boolean constant to BuildConfig.
     */
    public fun booleanField(name: String, value: Boolean) {
        booleanFields.put(name, value)
    }

    /**
     * Add a custom int constant to BuildConfig.
     */
    public fun intField(name: String, value: Int) {
        intFields.put(name, value)
    }

    /**
     * Add a string field that reads from local.properties or environment variables.
     *
     * This method automatically looks for the value in:
     * 1. local.properties file (git-ignored, for local development)
     * 2. Environment variables (for CI/CD)
     *
     * @param name The name of the BuildConfig constant and property key (e.g., "TMDB_API_KEY")
     * @throws IllegalStateException if the value is not found in either location
     */
    public fun buildConfigField(name: String) {
        val value = localProperties.getProperty(name) ?: System.getenv(name)
        requireNotNull(value) { "$name not found in local.properties or environment variables" }
        stringFields.put(name, value)
    }
}
