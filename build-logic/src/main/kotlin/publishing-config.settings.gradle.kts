import java.util.Properties

val sharedPropsFile = settingsDir.resolve("../gradle/publishing.properties")
require(sharedPropsFile.isFile) {
    "Expected shared publishing properties at ${sharedPropsFile.absolutePath}"
}

val shared: Properties = Properties().apply {
    sharedPropsFile.inputStream().use { load(it) }
}

gradle.beforeProject {
    shared.forEach { key, value ->
        val k = key.toString()
        if (!hasProperty(k)) {
            extensions.extraProperties.set(k, value.toString())
        }
    }
}
