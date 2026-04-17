// Loads gradle/publishing.properties and injects each entry as an extra property
// on every project, so `findProperty("VERSION_NAME")` resolves the same value in
// every composite. Composite-local gradle.properties wins — already-set keys are
// skipped so POM_NAME / POM_DESCRIPTION per composite still override.

import java.util.Properties

val sharedPropsFile = file("../gradle/publishing.properties")
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
