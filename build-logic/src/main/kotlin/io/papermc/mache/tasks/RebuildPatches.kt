package io.papermc.mache.tasks

import codechicken.diffpatch.cli.DiffOperation
import codechicken.diffpatch.util.archiver.ArchiveFormat
import io.papermc.mache.convertToPath
import io.papermc.mache.ensureClean
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.logging.Level
import javax.inject.Inject
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Always rebuild patches")
abstract class RebuildPatches : DefaultTask() {

    @get:InputFile
    abstract val decompJar: RegularFileProperty

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val patchDir: DirectoryProperty

    @get:Inject
    abstract val layout: ProjectLayout

    @TaskAction
    fun run() {
        patchDir.convertToPath().ensureClean()

        val logs = decompJar.convertToPath().resolveSibling("rebuildPatches.log")
        PrintStream(logs.outputStream().buffered()).use { ps ->
            val result = DiffOperation.builder()
                .aPath(decompJar.convertToPath(), ArchiveFormat.ZIP)
                .bPath(sourceDir.convertToPath(), null)
                .outputPath(patchDir.convertToPath(), null)
                .logTo(ps)
                .level(Level.FINE)
                .verbose(true)
                .summary(true)
                .build()
                .operate()

            val output = ByteArrayOutputStream()
            result.summary.print(PrintStream(output, true, Charsets.UTF_8), false)
            logger.lifecycle(output.toString(Charsets.UTF_8))

            if (result.exit != 0) {
                throw Exception("Failed to rebuild patches. See log file: ${logs.absolutePathString()}")
            }
        }
    }
}
