package io.github.thomaskioko.codegen.processor

import org.junit.Assert.assertEquals
import java.io.File

/**
 * Compares generated Kotlin source against a golden file in `src/test/resources/golden/`.
 * Set `-Dgolden.update=true` (or env var `GOLDEN_UPDATE=true`) to write the actual output back
 * to the golden file instead of failing.
 */
internal object GoldenFileAssert {

    private val updateGoldens: Boolean
        get() = System.getProperty("golden.update")?.toBooleanStrictOrNull() == true ||
            System.getenv("GOLDEN_UPDATE")?.toBooleanStrictOrNull() == true

    fun assertMatches(shape: String, fileName: String, actual: String) {
        val resourcePath = "golden/$shape/$fileName"
        val resourceFile = locateResource(resourcePath)

        if (updateGoldens) {
            resourceFile.parentFile.mkdirs()
            resourceFile.writeText(actual)
            return
        }

        if (!resourceFile.exists()) {
            error(
                "Missing golden file at ${resourceFile.absolutePath}. " +
                    "Re-run tests with -Dgolden.update=true to create it.",
            )
        }

        val expected = resourceFile.readText()
        assertEquals(
            "Generated file $fileName does not match golden $resourcePath",
            expected.normalize(),
            actual.normalize(),
        )
    }

    private fun String.normalize(): String =
        lines()
            .joinToString("\n") { it.trimEnd() }
            .trim()

    private fun locateResource(path: String): File {
        val testResources = locateTestResourcesDir()
        return File(testResources, path)
    }

    private fun locateTestResourcesDir(): File {
        var current: File? = File(".").canonicalFile
        while (current != null) {
            val candidate = File(current, "src/test/resources")
            if (candidate.isDirectory) return candidate
            val codegenChild = File(current, "codegen/processor-test/src/test/resources")
            if (codegenChild.isDirectory) return codegenChild
            current = current.parentFile
        }
        error("Could not find src/test/resources from working directory ${File(".").canonicalPath}")
    }
}
