package io.github.thomaskioko.gradle.plugins.functional

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppPluginFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `applying app plugin registers bumpVersion task on the app subproject`() {
        val project = Fixtures.extract("app-only", tempFolder.newFolder("project"))

        val result = project.runner(":app:help", "--task", ":app:bumpVersion").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:help")?.outcome)
    }

    @Test
    fun `applying app plugin registers release task on the app subproject`() {
        val project = Fixtures.extract("app-only", tempFolder.newFolder("project"))

        val result = project.runner(":app:help", "--task", ":app:release").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:help")?.outcome)
    }

    @Test
    fun `applying app plugin wires android variant test tasks into root ciTest`() {
        val project = Fixtures.extract("app-only", tempFolder.newFolder("project"))

        val result = project.runner("help", "--task", "ciTest").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }
}
