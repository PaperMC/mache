package io.papermc.mache.tasks

import codechicken.diffpatch.cli.DiffOperation
import codechicken.diffpatch.util.archiver.ArchiveFormat
import io.papermc.mache.convertToPath
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.Exception
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import javax.inject.Inject
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream
import kotlin.io.path.pathString
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

    @get:InputDirectory
    abstract val resourcesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val patchDir: DirectoryProperty

    @get:Inject
    abstract val layout: ProjectLayout

    @TaskAction
    fun run() {
        val copied = layout.buildDirectory.file("tmp/tmp_copied.zip").get().asFile.toPath()

        try {
            copied.deleteIfExists()
            copied.parent.createDirectories()

            FileSystems.newFileSystem(copied, mapOf("create" to true)).use { fs ->
                val root = fs.getPath("/")
                val walkFunc: (Path) -> (Path) -> Unit = { rootPath ->
                    { p ->
                        val target = root.resolve(p.relativeTo(rootPath).pathString)
                        if (p.isDirectory()) {
                            target.createDirectories()
                        } else {
                            p.copyTo(target)
                        }
                    }
                }
                sourceDir.convertToPath().let { src ->
                    Files.walk(src).use { it.forEach(walkFunc(src)) }
                }
                resourcesDir.convertToPath().let { rsc ->
                    Files.walk(rsc).use { it.forEach(walkFunc(rsc)) }
                }
            }

            patchDir.convertToPath().createDirectories()

            val logs = decompJar.convertToPath().resolveSibling("rebuildPatches.log")
            PrintStream(logs.outputStream().buffered()).use { ps ->
                val result = DiffOperation.builder()
                    .aPath(decompJar.convertToPath(), ArchiveFormat.ZIP)
                    .bPath(copied, ArchiveFormat.ZIP)
                    .outputPath(patchDir.convertToPath(), null)
                    .logTo(ps)
                    .level(Level.FINE)
                    .verbose(true)
                    .summary(true)
                    .build()
                    .operate()
                if (result.exit == -1) {
                    throw Exception("Failed to rebuild patches. See log file: ${logs.absolutePathString()}")
                }

                val output = ByteArrayOutputStream()
                result.summary.print(PrintStream(output, true, Charsets.UTF_8), false)
                logger.lifecycle(output.toString(Charsets.UTF_8))
            }
        } finally {
            copied.deleteIfExists()
        }
    }
}
