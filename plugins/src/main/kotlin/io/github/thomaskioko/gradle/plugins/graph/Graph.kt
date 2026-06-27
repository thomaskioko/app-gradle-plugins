package io.github.thomaskioko.gradle.plugins.graph

import io.github.thomaskioko.gradle.plugins.extensions.ModuleGraphExtension
import java.io.Serializable
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.api.tasks.TaskAction
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * Maps a convention-plugin id to the Mermaid class and node style used for modules applying it.
 * Declaration order is match priority: an application module also applies the android plugin, so the
 * application entry must come first.
 */
internal enum class PluginType(val id: String, val ref: String, val style: String) {
    Application(
        id = "io.github.thomaskioko.gradle.plugins.app",
        ref = "application",
        style = "fill:#CAFFBF,stroke:#000,stroke-width:2px,color:#000",
    ),
    Multiplatform(
        id = "io.github.thomaskioko.gradle.plugins.multiplatform",
        ref = "multiplatform",
        style = "fill:#FFD6A5,stroke:#000,stroke-width:2px,color:#000",
    ),
    AndroidLibrary(
        id = "io.github.thomaskioko.gradle.plugins.android",
        ref = "android-library",
        style = "fill:#9BF6FF,stroke:#000,stroke-width:2px,color:#000",
    ),
    Jvm(
        id = "io.github.thomaskioko.gradle.plugins.jvm",
        ref = "jvm-library",
        style = "fill:#BDB2FF,stroke:#000,stroke-width:2px,color:#000",
    ),
    Unknown(
        id = "?",
        ref = "unknown",
        style = "fill:#FFADAD,stroke:#000,stroke-width:2px,color:#000",
    ),
}

private fun normalizeConfiguration(name: String): String = when {
    name == "api" || name.endsWith("Api") -> "api"
    name == "implementation" || name.endsWith("Implementation") -> "implementation"
    else -> name
}

private class ModuleGraph(
    private val root: Project,
    private val supportedConfigurations: Set<String>,
    private val ignoredRegex: Regex,
    private val ignoredProjects: Set<String>,
    private val dependencies: MutableMap<String, Set<GraphEdge>> = mutableMapOf(),
    private val plugins: MutableMap<String, PluginType> = mutableMapOf(),
    private val seen: MutableSet<String> = mutableSetOf(),
) {
    operator fun invoke(project: Project = root): ModuleGraph {
        if (project.path in seen) return this
        seen += project.path
        plugins.putIfAbsent(
            project.path,
            PluginType.entries.firstOrNull { project.pluginManager.hasPlugin(it.id) } ?: PluginType.Unknown,
        )
        dependencies.putIfAbsent(project.path, emptySet())
        project.configurations
            .matching { it.name in supportedConfigurations }
            .forEach { configuration ->
                configuration.dependencies.withType(ProjectDependency::class.java)
                    .map { project.project(it.path) }
                    .filter { !it.path.isIgnored() }
                    .forEach { dependency ->
                        dependencies.compute(project.path) { _, edges ->
                            edges.orEmpty() + GraphEdge(normalizeConfiguration(configuration.name), dependency.path)
                        }
                        invoke(dependency)
                    }
            }
        return this
    }

    private fun String.isIgnored(): Boolean = matches(ignoredRegex) || this in ignoredProjects

    fun dependencies(): Map<String, Set<GraphEdge>> = dependencies

    fun plugins(): Map<String, PluginType> = plugins
}

internal fun Project.configureGraphTasks() {
    if (!buildFile.exists()) return
    // Read per subproject; the root build script (any `moduleGraph {}` block) is evaluated before
    // subprojects, so these values are final here.
    val extension = rootProject.extensions.getByType(ModuleGraphExtension::class.java)
    val ignoredRegex = extension.ignoredProjectsRegex.get().toRegex()
    val ignoredProjects = extension.ignoredProjects.get()
    if (path.matches(ignoredRegex) || path in ignoredProjects) return
    val supportedConfigurations = extension.supportedConfigurations.get()

    val dumpTask = tasks.register("graphDump", GraphDumpTask::class.java) { task ->
        val graph = ModuleGraph(this, supportedConfigurations, ignoredRegex, ignoredProjects).invoke()
        task.projectPath.set(path)
        task.dependencies.set(graph.dependencies())
        task.plugins.set(graph.plugins())
        task.output.set(layout.buildDirectory.file("mermaid/graph.txt"))
        task.legend.set(layout.buildDirectory.file("mermaid/legend.txt"))
    }
    tasks.register("graphUpdate", GraphUpdateTask::class.java) { task ->
        task.projectPath.set(path)
        task.input.set(dumpTask.flatMap { it.output })
        task.legend.set(dumpTask.flatMap { it.legend })
        task.output.set(layout.projectDirectory.file("README.md"))
    }
}

internal data class GraphEdge(val configuration: String, val target: String) : Serializable

private class Edge(val from: String, val configuration: String, val to: String)

@CacheableTask
internal abstract class GraphDumpTask : DefaultTask() {
    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val dependencies: MapProperty<String, Set<GraphEdge>>

    @get:Input
    abstract val plugins: MapProperty<String, PluginType>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:OutputFile
    abstract val legend: RegularFileProperty

    init {
        description = "Dumps this module's dependency graph to a Mermaid file."
    }

    @TaskAction
    fun dump() {
        output.get().asFile.writeText(mermaid())
        legend.get().asFile.writeText(legend())
    }

    private fun pluginOf(path: String): PluginType = plugins.get()[path] ?: PluginType.Unknown

    private fun mermaid(): String = buildString {
        val edges = dependencies.get()
            .flatMap { (from, links) -> links.map { Edge(from, it.configuration, it.target) } }
            .toSet()
        appendLine("graph TB")
        val nodes = edges.flatMap { listOf(it.from, it.to) }.toMutableSet().plus(projectPath.get())
        nodes.groupBy { it.substringBeforeLast(":", missingDelimiterValue = "") }
            .toSortedMap()
            .forEach { (group, members) ->
                if (group.isNotEmpty()) {
                    appendLine("  subgraph $group")
                    appendLine("    direction TB")
                    members.sorted().forEach { appendLine(it.alias(indent = 4)) }
                    appendLine("  end")
                } else {
                    members.sorted().forEach { appendLine(it.alias(indent = 2)) }
                }
            }
        if (edges.isNotEmpty()) appendLine()
        edges.sortedWith(compareBy({ it.from }, { it.to }, { it.configuration }))
            .forEach { appendLine(it.link(indent = 2)) }
        appendLine()
        PluginType.entries.forEach { appendLine(it.classDef()) }
    }

    private fun legend(): String = buildString {
        appendLine("graph TB")
        listOf(
            "application" to PluginType.Application,
            "multiplatform" to PluginType.Multiplatform,
            "android-library" to PluginType.AndroidLibrary,
            "jvm-library" to PluginType.Jvm,
        ).forEach { (name, type) -> appendLine("  $name[$name]:::${type.ref}") }
        appendLine()
        appendLine("  api[\"api dependency\"] --> implementation[\"implementation dependency\"]")
        appendLine()
        PluginType.entries.forEach { appendLine(it.classDef()) }
    }

    private fun String.alias(indent: Int): String =
        " ".repeat(indent) + this + "[" + substringAfterLast(":") + "]:::" + pluginOf(this).ref

    private fun Edge.link(indent: Int): String {
        val arrow = when (configuration) {
            "api" -> "-->"
            "implementation" -> "-.->"
            else -> "-.->|$configuration|"
        }
        return " ".repeat(indent) + "$from $arrow $to"
    }

    private fun PluginType.classDef(): String = "classDef $ref $style;"
}

@CacheableTask
internal abstract class GraphUpdateTask : DefaultTask() {
    @get:Input
    abstract val projectPath: Property<String>

    @get:InputFile
    @get:PathSensitive(NONE)
    abstract val input: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(NONE)
    abstract val legend: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    init {
        description = "Writes this module's dependency graph into its README.md."
    }

    @TaskAction
    fun update() {
        val readme = output.get().asFile
        if (!readme.exists()) {
            readme.writeText(
                """
                # `${projectPath.get()}`

                ## Module dependency graph

                <!--region graph--> <!--endregion-->
                """.trimIndent() + "\n",
            )
        }
        val graph = input.get().asFile.readText().trim()
        val legendGraph = legend.get().asFile.readText().trim()
        val region = """(<!--region graph-->)(.*?)(<!--endregion-->)""".toRegex(DOT_MATCHES_ALL)
        val updated = readme.readText().replace(region) { match ->
            val (start, _, end) = match.destructured
            """
            |$start
            |```mermaid
            |$graph
            |```
            |
            |<details><summary>Graph legend</summary>
            |
            |```mermaid
            |$legendGraph
            |```
            |
            |</details>
            |$end
            """.trimMargin()
        }
        readme.writeText(updated)
    }
}
