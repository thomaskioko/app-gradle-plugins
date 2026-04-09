package io.github.thomaskioko.gradle.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import java.io.File
import java.util.zip.ZipFile

/**
 * Copies .bundle resource directories from klib dependencies to an output directory.
 * This ensures moko-resources bundles are available at runtime for iOS test binaries,
 * not just for modules that directly apply the moko-resources plugin.
 */
internal object CopyMokoResourceBundlesTask {

    fun copyBundles(
        klibs: FileCollection,
        outputDir: File,
        tempDir: File,
        logger: Logger,
    ) {
        klibs.files
            .filter { it.exists() }
            .flatMap { findBundles(it, tempDir) }
            .forEach { bundle ->
                val destination = File(outputDir, bundle.name)
                bundle.copyRecursively(destination, overwrite = true)
                logger.info("Copied bundle {} to {}", bundle.name, destination)
            }
    }

    private fun findBundles(file: File, tempDir: File): List<File> {
        if (file.isFile && file.extension == "klib") {
            return findBundlesInPackedKlib(file, tempDir)
        }
        if (file.isDirectory) {
            val resourcesDir = File(file, "default/resources")
            if (resourcesDir.isDirectory) {
                return resourcesDir.listFiles()
                    ?.filter { it.isDirectory && it.extension == "bundle" }
                    ?: emptyList()
            }
        }
        return emptyList()
    }

    private fun findBundlesInPackedKlib(klibFile: File, tempDir: File): List<File> {
        val extractDir = File(tempDir, klibFile.nameWithoutExtension)

        ZipFile(klibFile).use { zf ->
            val hasResources =
                zf.entries().asSequence().any { it.name.startsWith("default/resources/") }
            if (!hasResources) return emptyList()

            extractDir.mkdirs()
            zf.entries().asSequence()
                .filter { it.name.startsWith("default/resources/") }
                .forEach { entry ->
                    val target = File(extractDir, entry.name.removePrefix("default/resources/"))
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile.mkdirs()
                        zf.getInputStream(entry).use { input ->
                            target.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
        }

        return extractDir.listFiles()
            ?.filter { it.isDirectory && it.extension == "bundle" }
            ?: emptyList()
    }
}
