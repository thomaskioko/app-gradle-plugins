package io.github.thomaskioko.gradle.plugins.extensions

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import java.util.Properties

/**
 * Configures the generated `BuildConfig` class produced by the `buildconfig` plugin.
 *
 * The plugin generates a Kotlin `BuildConfig` object whose constants come from three sources:
 * literal values declared through [stringField], [booleanField], and [intField]; entries that
 * read from `local.properties` or environment variables through [buildConfigField]; and the
 * project's [packageName].
 *
 * ```kotlin
 * scaffold {
 *   buildConfig {
 *     packageName.set("com.example.app")
 *     stringField("BASE_URL", "https://api.example.com")
 *     booleanField("ANALYTICS_ENABLED", true)
 *     buildConfigField("TMDB_API_KEY")
 *   }
 * }
 * ```
 *
 * @property packageName Package name written into the generated `BuildConfig` class. Required.
 * @property stringFields String constants to emit. Keys are constant names; values are constant
 *   contents. Defaults to an empty map.
 * @property booleanFields Boolean constants to emit. Keys are constant names; values are
 *   constant contents. Defaults to an empty map.
 * @property intFields Int constants to emit. Keys are constant names; values are constant
 *   contents. Defaults to an empty map.
 */
@ScaffoldDsl
public abstract class BuildConfigExtension(private val project: Project) {

    public abstract val packageName: Property<String>

    public abstract val stringFields: MapProperty<String, String>

    public abstract val booleanFields: MapProperty<String, Boolean>

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
     * Adds a `String` constant to the generated `BuildConfig`.
     *
     * ```kotlin
     * scaffold {
     *   buildConfig {
     *     stringField("BASE_URL", "https://api.example.com")
     *   }
     * }
     * ```
     *
     * @param name Constant name written into `BuildConfig`. Conventionally upper snake case.
     * @param value Constant value emitted as a Kotlin `String` literal.
     */
    public fun stringField(name: String, value: String) {
        stringFields.put(name, value)
    }

    /**
     * Adds a `Boolean` constant to the generated `BuildConfig`.
     *
     * ```kotlin
     * scaffold {
     *   buildConfig {
     *     booleanField("ANALYTICS_ENABLED", true)
     *   }
     * }
     * ```
     *
     * @param name Constant name written into `BuildConfig`. Conventionally upper snake case.
     * @param value Constant value emitted as a Kotlin `Boolean` literal.
     */
    public fun booleanField(name: String, value: Boolean) {
        booleanFields.put(name, value)
    }

    /**
     * Adds an `Int` constant to the generated `BuildConfig`.
     *
     * ```kotlin
     * scaffold {
     *   buildConfig {
     *     intField("CACHE_TTL_SECONDS", 300)
     *   }
     * }
     * ```
     *
     * @param name Constant name written into `BuildConfig`. Conventionally upper snake case.
     * @param value Constant value emitted as a Kotlin `Int` literal.
     */
    public fun intField(name: String, value: Int) {
        intFields.put(name, value)
    }

    /**
     * Adds a `String` constant whose value comes from `local.properties` or an environment
     * variable.
     *
     * The lookup order is `local.properties` first (typically used during local development and
     * git-ignored), then environment variables (typically used on CI). The value is read at
     * configuration time and emitted as a `String` constant under [name].
     *
     * ```kotlin
     * scaffold {
     *   buildConfig {
     *     buildConfigField("TMDB_API_KEY")
     *   }
     * }
     * ```
     *
     * @param name Both the `BuildConfig` constant name and the lookup key in `local.properties`
     *   or the environment.
     * @throws IllegalStateException if [name] is not present in `local.properties` and is not
     *   set as an environment variable.
     */
    public fun buildConfigField(name: String) {
        val value = localProperties.getProperty(name) ?: System.getenv(name)
        requireNotNull(value) { "$name not found in local.properties or environment variables" }
        stringFields.put(name, value)
    }
}
