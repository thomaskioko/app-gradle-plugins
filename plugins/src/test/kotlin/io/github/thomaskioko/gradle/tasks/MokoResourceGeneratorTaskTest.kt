package io.github.thomaskioko.gradle.tasks

import com.squareup.kotlinpoet.ClassName
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class MokoResourceGeneratorTaskTest {

    private fun createTask(): MokoResourceGeneratorTask {
        val project = ProjectBuilder.builder().build()
        return project.tasks.register("generateMokoStrings", MokoResourceGeneratorTask::class.java).get()
    }

    private fun loadResource(path: String): String =
        javaClass.classLoader.getResource(path)!!.readText()

    private fun loadResourceFile(path: String): File =
        File(javaClass.classLoader.getResource(path)!!.toURI())

    @Test
    fun `toPascalCase converts snake_case to PascalCase`() {
        val task = createTask()
        assertEquals("ButtonErrorRetry", task.toPascalCase("button_error_retry"))
        assertEquals("AppName", task.toPascalCase("app_name"))
        assertEquals("CdShowPoster", task.toPascalCase("cd_show_poster"))
    }

    @Test
    fun `readKeysFromMRFile extracts string and plural keys`() {
        val task = createTask()
        val mrFile = loadResourceFile("moko-generator/MR.kt")

        val (stringKeys, pluralKeys) = task.readKeysFromMRFile(mrFile)

        assertEquals(
            listOf("button_error_retry", "app_name", "label_discover_trending_today"),
            stringKeys,
        )
        assertEquals(listOf("episode_count", "season_count"), pluralKeys)
    }

    @Test
    fun `stringResourceKeyFileSpec matches expected output`() {
        val task = createTask()
        val packageName = "com.thomaskioko.tvmaniac.i18n"
        val mrClass = ClassName(packageName, "MR")
        val mrFile = loadResourceFile("moko-generator/MR.kt")
        val (stringKeys, _) = task.readKeysFromMRFile(mrFile)

        val fileSpec = task.stringResourceKeyFileSpec(
            packageName = packageName,
            stringKeys = stringKeys,
            mrClass = mrClass,
        )

        val expected = loadResource("moko-generator/expected/StringResourceKey.kt")
        assertEquals(expected, fileSpec.toString())
    }

    @Test
    fun `pluralsResourceKeyFileSpec matches expected output`() {
        val task = createTask()
        val packageName = "com.thomaskioko.tvmaniac.i18n"
        val mrClass = ClassName(packageName, "MR")
        val mrFile = loadResourceFile("moko-generator/MR.kt")
        val (_, pluralKeys) = task.readKeysFromMRFile(mrFile)

        val fileSpec = task.pluralsResourceKeyFileSpec(
            packageName = packageName,
            pluralKeys = pluralKeys,
            mrClass = mrClass,
        )

        val expected = loadResource("moko-generator/expected/PluralsResourceKey.kt")
        assertEquals(expected, fileSpec.toString())
    }
}
