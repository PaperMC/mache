package io.papermc.mache.tasks

import codechicken.diffpatch.cli.PatchOperation
import codechicken.diffpatch.util.PatchMode
import codechicken.diffpatch.util.archiver.ArchiveFormat
import io.papermc.mache.convertToPath
import java.io.PrintStream
import java.util.logging.Level
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.outputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class ApplyPatches : DefaultTask() {

    @get:Optional
    @get:InputDirectory
    abstract val patchDir: DirectoryProperty

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val patchesPresent = patchDir.isPresent && run {
            val patches = patchDir.convertToPath()
            patches.exists() && patches.listDirectoryEntries().isNotEmpty()
        }

        if (!patchesPresent) {
            inputFile.convertToPath().copyTo(outputFile.convertToPath(), overwrite = true)
            return
        }

        val output = outputFile.convertToPath()
        val logs = output.resolveSibling("${output.name}.log")

        PrintStream(logs.outputStream().buffered()).use { ps ->
            val result = PatchOperation.builder()
                .patchesPath(patchDir.convertToPath(), null)
                .basePath(inputFile.convertToPath(), ArchiveFormat.ZIP)
                .outputPath(outputFile.convertToPath(), ArchiveFormat.ZIP)
                .mode(PatchMode.EXACT)
                .level(Level.FINE)
                .verbose(true)
                .summary(true)
                .logTo(ps)
                .build()
                .operate()
            if (result.exit != 0) {
                throw Exception("Failed to apply patches (code: ${result.exit}). See log file: ${logs.absolutePathString()}")
            }
        }
    }
}
