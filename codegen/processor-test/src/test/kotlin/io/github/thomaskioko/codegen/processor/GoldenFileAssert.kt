package io.github.thomaskioko.codegen.processor

import org.junit.Assert.assertEquals
import java.io.File

/**
 * Compares generated Kotlin source against a checked in golden file.
 *
 * Each generator output has a corresponding `.kt` file under
 * `codegen/processor-test/src/test/resources/golden/<variant>/<file>.kt` that records the
 * expected output. [assertMatches] reads the golden, normalises both expected and actual by
 * trimming trailing whitespace per line and trimming the file as a whole, then compares them
 * with `assertEquals`. Trailing newline drift therefore does not cause flakes.
 *
 * ## Updating goldens
 *
 * Set the system property `golden.update=true` or the environment variable `GOLDEN_UPDATE=true`
 * and run the suite. [assertMatches] writes the actual output back to the golden file instead of
 * failing. The repo wraps this in the `/update-golden` skill, which sets the property, runs the
 * suite, and surfaces the diff so the change is reviewable before commit. Always read the diff.
 * Goldens are the contract a contributor is committing to; an unreviewed bulk update masks
 * regressions.
 */
internal object GoldenFileAssert {

    private val updateGoldens: Boolean
        get() = System.getProperty("golden.update")?.toBooleanStrictOrNull() == true ||
            System.getenv("GOLDEN_UPDATE")?.toBooleanStrictOrNull() == true

    /**
     * Asserts that [actual] matches the golden file for the given variant and file name.
     *
     * @param variant The variant directory under `golden/` (`simple`, `parameterized`, `tab`,
     *   `screen-ui`, or `sheet-ui`).
     * @param fileName The simple name of the generated file (e.g. `DebugScreenGraph.kt`).
     * @param actual The generated source as the processor produced it.
     */
    fun assertMatches(variant: String, fileName: String, actual: String) {
        val resourcePath = "golden/$variant/$fileName"
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
