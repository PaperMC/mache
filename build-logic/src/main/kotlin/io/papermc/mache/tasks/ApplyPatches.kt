package io.papermc.mache.tasks

import codechicken.diffpatch.cli.PatchOperation
import codechicken.diffpatch.util.PatchMode
import codechicken.diffpatch.util.archiver.ArchiveFormat
import io.papermc.mache.constants.SERVER_DIR
import io.papermc.mache.convertToPath
import io.papermc.mache.ensureClean
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.logging.Level
import javax.inject.Inject
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Always apply patches")
abstract class ApplyPatches : DefaultTask() {

    @get:Optional
    @get:InputDirectory
    abstract val patchDir: DirectoryProperty

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val files: FileOperations

    @get:Inject
    abstract val layout: ProjectLayout

    @TaskAction
    fun run() {
        val patchesPresent = patchDir.isPresent && run {
            val patches = patchDir.convertToPath()
            patches.exists() && patches.listDirectoryEntries().isNotEmpty()
        }

        if (!patchesPresent) {
            outputDir.convertToPath().ensureClean()
            files.sync {
                from(files.zipTree(inputFile))
                into(outputDir)
                includeEmptyDirs = false
            }
            return
        }

        val logs = layout.buildDirectory.file("$SERVER_DIR/applyPatches.log").convertToPath()

        PrintStream(logs.outputStream().buffered()).use { ps ->
            val result = PatchOperation.builder()
                .patchesPath(patchDir.convertToPath(), null)
                .basePath(inputFile.convertToPath(), ArchiveFormat.ZIP)
                .outputPath(outputDir.convertToPath(), null)
                .mode(PatchMode.EXACT)
                .level(Level.FINE)
                .verbose(true)
                .summary(true)
                .logTo(ps)
                .build()
                .operate()

            val output = ByteArrayOutputStream()
            result.summary.print(PrintStream(output, true, Charsets.UTF_8), false)
            logger.lifecycle(output.toString(Charsets.UTF_8))

            if (result.exit != 0) {
                throw Exception("Failed to apply patches (code: ${result.exit}). See log file: ${logs.absolutePathString()}")
            }
        }
    }
}
