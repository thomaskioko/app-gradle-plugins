package io.github.thomaskioko.gradle.plugins.functional

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KmpPluginFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `applying multiplatform plugin configures jvm and ios targets`() {
        val project = Fixtures.extract("kmp-android-ios", tempFolder.newFolder("project"))

        val result = project.runner(":feature:help", "--task", ":feature:compileKotlinJvm").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":feature:help")?.outcome)
    }

    @Test
    fun `applying multiplatform plugin wires variant test tasks into root ciTest`() {
        val project = Fixtures.extract("kmp-android-ios", tempFolder.newFolder("project"))

        val result = project.runner("help", "--task", "ciTest").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }

    @Test
    fun `applying multiplatform plugin wires ios test tasks into root iosTest`() {
        val project = Fixtures.extract("kmp-android-ios", tempFolder.newFolder("project"))

        val result = project.runner("help", "--task", "iosTest").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }
}
