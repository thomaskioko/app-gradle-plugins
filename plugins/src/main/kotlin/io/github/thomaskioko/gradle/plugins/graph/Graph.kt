/*
 * Copyright 2026 Thomas Kioko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thomaskioko.gradle.plugins.graph

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.Serializable
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

private val IGNORED_PROJECTS = Regex(".*:testing$")

private val SUPPORTED_CONFIGURATIONS = listOf("commonMainApi", "commonMainImplementation", "api", "implementation")

private const val MODULE_GRAPH_ATTRIBUTE = "io.github.thomaskioko.module-graph"

private fun normalizeConfiguration(name: String): String =
    if (name == "api" || name.endsWith("Api")) "api" else "implementation"

internal fun Project.configureGraphTasks() {
    if (path.matches(IGNORED_PROJECTS)) return

    val edges = provider {
        SUPPORTED_CONFIGURATIONS.mapNotNull { configurations.findByName(it) }
            .flatMap { configuration ->
                configuration.dependencies.withType(ProjectDependency::class.java)
                    .filterNot { it.path.matches(IGNORED_PROJECTS) }
                    .map { GraphEdge(normalizeConfiguration(configuration.name), it.path) }
            }
            .toSet()
    }

    val nodeTask = tasks.register("graphNode", GraphNodeTask::class.java) { task ->
        task.projectPath.set(path)
        task.pluginType.set(
            provider { PluginType.entries.firstOrNull { pluginManager.hasPlugin(it.id) } ?: PluginType.Unknown },
        )
        task.edges.set(edges)
        task.output.set(layout.buildDirectory.file("mermaid/node.txt"))
    }

    val graphDependencies = configurations.dependencyScope("moduleGraph") { scope ->
        scope.dependencies.addAllLater(
            edges.map { set -> set.map { dependencies.project(mapOf("path" to it.target)) } },
        )
    }
    configurations.consumable("moduleGraphElements") { elements ->
        elements.extendsFrom(graphDependencies.get())
        elements.attributes.moduleGraph(this)
        elements.outgoing.artifact(nodeTask.flatMap { it.output })
    }
    val graphClasspath = configurations.resolvable("moduleGraphClasspath") { classpath ->
        classpath.extendsFrom(graphDependencies.get())
        classpath.attributes.moduleGraph(this)
    }

    val dumpTask = tasks.register("graphDump", GraphDumpTask::class.java) { task ->
        task.projectPath.set(path)
        task.nodes.from(nodeTask.flatMap { it.output }, graphClasspath)
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

private fun AttributeContainer.moduleGraph(project: Project) {
    attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, MODULE_GRAPH_ATTRIBUTE))
    attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, MODULE_GRAPH_ATTRIBUTE))
}

internal data class GraphEdge(val configuration: String, val target: String) : Serializable

private class GraphNode(val path: String, val pluginType: PluginType, val edges: List<GraphEdge>)

private class Edge(val from: String, val configuration: String, val to: String)

@CacheableTask
internal abstract class GraphNodeTask : DefaultTask() {
    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val pluginType: Property<PluginType>

    @get:Input
    abstract val edges: SetProperty<GraphEdge>

    @get:OutputFile
    abstract val output: RegularFileProperty

    init {
        description = "Writes this module's plugin type and direct module dependencies for graphDump."
    }

    @TaskAction
    fun write() {
        output.get().asFile.writeText(
            buildString {
                appendLine("${pluginType.get().name}\t${projectPath.get()}")
                edges.get()
                    .sortedWith(compareBy({ it.target }, { it.configuration }))
                    .forEach { appendLine("${it.configuration}\t${it.target}") }
            },
        )
    }
}

private fun readNode(file: File): GraphNode {
    val lines = file.readLines()
    val (pluginType, path) = lines.first().split("\t")
    val edges = lines.drop(1).map { line ->
        val (configuration, target) = line.split("\t")
        GraphEdge(configuration, target)
    }
    return GraphNode(path, PluginType.valueOf(pluginType), edges)
}

@CacheableTask
internal abstract class GraphDumpTask : DefaultTask() {
    @get:Input
    abstract val projectPath: Property<String>

    @get:InputFiles
    @get:PathSensitive(NONE)
    abstract val nodes: ConfigurableFileCollection

    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:OutputFile
    abstract val legend: RegularFileProperty

    init {
        description = "Dumps this module's dependency graph to a Mermaid file."
    }

    @TaskAction
    fun dump() {
        val graphNodes = nodes.files.map(::readNode)
        val plugins = graphNodes.associate { it.path to it.pluginType }
        val edges = graphNodes
            .flatMap { node -> node.edges.map { Edge(node.path, it.configuration, it.target) } }
            .toSet()
        output.get().asFile.writeText(mermaid(plugins, edges))
        legend.get().asFile.writeText(legend())
    }

    private fun mermaid(plugins: Map<String, PluginType>, edges: Set<Edge>): String = buildString {
        fun String.alias(indent: Int): String =
            " ".repeat(indent) + this + "[" + substringAfterLast(":") + "]:::" + (plugins[this] ?: PluginType.Unknown).ref

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
        PluginType.entries
            .filterNot { it == PluginType.Unknown }
            .forEach { appendLine("  ${it.ref}[${it.ref}]:::${it.ref}") }
        appendLine()
        appendLine("  api[\"api dependency\"] --> implementation[\"implementation dependency\"]")
        appendLine()
        PluginType.entries.forEach { appendLine(it.classDef()) }
    }

    private fun Edge.link(indent: Int): String {
        val arrow = if (configuration == "api") "-->" else "-.->"
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
