package io.github.thomaskioko.gradle.plugins.functional

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JvmPluginFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `applying jvm plugin registers aggregate ciTest task`() {
        val project = Fixtures.extract("jvm-library", tempFolder.newFolder("project"))

        val result = project.runner("help", "--task", "ciTest").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }

    @Test
    fun `applying jvm plugin registers test task linked into ciTest`() {
        val project = Fixtures.extract("jvm-library", tempFolder.newFolder("project"))

        val result = project.runner("help", "--task", "test").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }

    @Test
    fun `applying jvm plugin configures spotlessCheck task from base plugin`() {
        val project = Fixtures.extract("jvm-library", tempFolder.newFolder("project"))

        val result = project.runner("help", "--task", "spotlessCheck").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }
}
