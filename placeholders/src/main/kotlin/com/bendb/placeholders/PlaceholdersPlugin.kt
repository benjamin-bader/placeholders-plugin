package com.bendb.placeholders

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.ide.common.res2.FileStatus
import com.android.utils.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class PlaceholdersPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val plugin = project.plugins.findPlugin(AppPlugin::class.java)
        if (plugin != null) {
            val ext = project.extensions.findByType(AppExtension::class.java)!!
            ext.applicationVariants.all { variant ->
                val mergedResourcesDir = variant.mergeResources.outputDir

                variant.outputs.all { output ->
                    val intermediatePath = Paths.get(
                            project.buildDir.absolutePath,
                            variant.name,
                            output.name,
                            "res-placeholders").toFile()

                    // TODO: Use TaskContainer here.
                    val task = ReplacePlaceholdersTask(
                            emptyMap(),
                            mergedResourcesDir,
                            intermediatePath)

                    task.dependsOn(variant.mergeResources)

                    output.processResources.let {
                        replaceInputResourcesDir(it, task.outputFiles)
                        it.dependsOn(task)
                    }
                }
            }
        }
    }
}

fun replaceInputResourcesDir(target: ProcessAndroidResources, replacement: FileCollection) {
    val field = ProcessAndroidResources::class.java.getField("inputResourcesDir")
    field.isAccessible = true
    field.set(target, replacement)
}

@ParallelizableTask
@CacheableTask
open class ReplacePlaceholdersTask(
        @get:Input val placeholders: Map<String, String>,
        @get:InputDirectory val mergedResourceDir: File,
        @get:OutputDirectory val outputDirectory: File
): DefaultTask() {
    private val dirsKnownToExist = mutableSetOf<Path>()

    @get:OutputFiles
    val outputFiles: FileCollection = project.files(outputDirectory).builtBy(this)

    @TaskAction
    open fun replacePlaceholdersInResources(inputs: IncrementalTaskInputs) {
        if (inputs.isIncremental) {
            val mergedResourcePath = mergedResourceDir.toPath()
            val changes = mutableMapOf<Path, FileStatus>()
            inputs.outOfDate { details ->
                val state = if (details.isAdded) FileStatus.NEW else FileStatus.CHANGED
                val path = details.file.toPath()
                val relativePath = mergedResourcePath.relativize(path)

                changes[relativePath] = state
            }

            inputs.removed { details ->
                val path = details.file.toPath()
                val relativePath = mergedResourcePath.relativize(path)

                changes[relativePath] = FileStatus.REMOVED
            }

            processIncrementalResourceFiles(changes)
        } else {
            processAllResourceFiles()
        }
    }

    private fun processAllResourceFiles() {
        FileUtils.cleanOutputDir(outputDirectory)

        val inputDirectoryPath = mergedResourceDir.toPath()
        val outputDirectoryPath = outputDirectory.toPath()

        Files.walkFileTree(inputDirectoryPath, object : SimpleFileVisitor<Path>() {
            override fun visitFile(inputFile: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativePath = inputDirectoryPath.relativize(inputFile)
                val outputFile = outputDirectoryPath.resolve(relativePath)

                processSingleResource(inputFile, outputFile)

                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun processIncrementalResourceFiles(changes: Map<Path, FileStatus>) {
        val inputPath = mergedResourceDir.toPath()
        val outputPath = outputDirectory.toPath()
        changes.forEach { path, changeType ->
            val changedResourcePath = outputPath.resolve(path)

            when (changeType) {
                FileStatus.REMOVED -> Files.delete(changedResourcePath)
                else ->
                        processSingleResource(
                                inputPath.resolve(changedResourcePath),
                                changedResourcePath)
            }
        }
    }

    private fun processSingleResource(inputPath: Path, outputPath: Path) {
        if (dirsKnownToExist.add(outputPath.parent)) {
            Files.createDirectories(outputPath.parent)
        }

        if (inputPath.endsWith(".xml")) {
            Files.newBufferedReader(inputPath, Charsets.UTF_8).use { reader ->
                Files.newBufferedWriter(
                        outputPath,
                        Charsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE).use { writer ->

                    reader.lineSequence().forEach { line ->
                        writer.write(processSingleLine(line))
                    }

                    writer.flush()
                }
            }
        } else {
            Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun processSingleLine(line: String): String {
        return placeholders.asIterable().fold(line) { line, (placeholder, replacement) ->
            if (placeholder in line) {
                line.replace(placeholder, replacement)
            } else {
                line
            }
        }
    }
}