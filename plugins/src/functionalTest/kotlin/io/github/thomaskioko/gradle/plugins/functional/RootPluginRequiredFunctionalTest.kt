package io.github.thomaskioko.gradle.plugins.functional

import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RootPluginRequiredFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `applying a subproject plugin without root plugin fails with remediation message`() {
        val project = Fixtures.extract("missing-root-plugin", tempFolder.newFolder("project"))

        val result = project.runner("help").buildAndFail()

        assertTrue(
            "Expected GradleException remediation message; got:\n${result.output}",
            result.output.contains("io.github.thomaskioko.gradle.plugins.root must be applied to the root project"),
        )
    }
}
