package io.papermc.mache.tasks

import codechicken.diffpatch.cli.DiffOperation
import codechicken.diffpatch.util.archiver.ArchiveFormat
import io.papermc.mache.convertToPath
import io.papermc.mache.ensureClean
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.FileSystems
import java.util.logging.Level
import javax.inject.Inject
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
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
        val copied = layout.buildDirectory.file("tmp/copied.jar").convertToPath().ensureClean()
        patchDir.convertToPath().ensureClean()

        try {
            FileSystems.newFileSystem(copied, mapOf("create" to true)).use { fs ->
                val sourceRoot = sourceDir.convertToPath()
                val outputRoot = fs.getPath("/")

                for (path in sourceRoot.walk().filterNot { it.relativeTo(sourceRoot).first().toString() == ".git" }) {
                    val target = outputRoot.resolve(path.relativeTo(sourceRoot).toString())
                    target.parent.createDirectories()
                    path.copyTo(target)
                }
            }

            val logs = decompJar.convertToPath().resolveSibling("rebuildPatches.log")
            PrintStream(logs.outputStream().buffered()).use { ps ->
                val result = DiffOperation.builder()
                    .aPath(decompJar.convertToPath(), ArchiveFormat.ZIP)
                    .bPath(copied.convertToPath(), ArchiveFormat.ZIP)
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

                // DiffPatch is a bit weird. Successful runs will return 1 or 0, only -1 is returned for errors
                if (result.exit == -1) {
                    throw Exception("Failed to rebuild patches. See log file: ${logs.absolutePathString()}")
                }
            }
        } finally {
            copied.deleteIfExists()
        }
    }
}
