package io.papermc.mache.tasks

import io.papermc.mache.util.convertToPath
import io.papermc.mache.util.copyEntry
import io.papermc.mache.util.ensureClean
import io.papermc.mache.util.patches.JavaPatcher
import io.papermc.mache.util.patches.NativePatcher
import io.papermc.mache.util.patches.PatchFailure
import io.papermc.mache.util.patches.Patcher
import io.papermc.mache.util.readZip
import io.papermc.mache.util.writeZip
import javax.inject.Inject
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.relativeTo
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations

@UntrackedTask(because = "Always apply patches")
abstract class ApplyPatches : DefaultTask() {

    @get:Optional
    @get:InputDirectory
    abstract val patchDir: DirectoryProperty

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:Internal
    abstract val useNativeDiff: Property<Boolean>

    @get:Internal
    abstract val patchExecutable: Property<String>

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:OutputFile
    abstract val failedPatchesJar: RegularFileProperty

    @get:Inject
    abstract val exec: ExecOperations

    @get:Inject
    abstract val files: FileOperations

    @get:Inject
    abstract val layout: ProjectLayout

    init {
        run {
            useNativeDiff.convention(false)
            patchExecutable.convention("patch")
        }
    }

    @TaskAction
    fun run() {
        val patchesPresent = patchDir.isPresent && run {
            val patches = patchDir.convertToPath()
            patches.exists() && patches.listDirectoryEntries().isNotEmpty()
        }

        val out = outputJar.convertToPath().ensureClean()
        val failed = failedPatchesJar.convertToPath().ensureClean()

        if (!patchesPresent) {
            inputFile.convertToPath().copyTo(out)
            failed.writeZip { }
            return
        }

        val tempInDir = out.resolveSibling(".tmp_applyPatches_input").ensureClean()
        tempInDir.createDirectory()
        val tempOutDir = out.resolveSibling(".tmp_applyPatches_output").ensureClean()
        tempOutDir.createDirectory()
        val tempFailedPatchDir = out.resolveSibling(".tmp_applyPatches_failed").ensureClean()
        tempFailedPatchDir.createDirectory()

        try {
            files.sync {
                from(files.zipTree(inputFile))
                into(tempInDir)
            }

            val result = createPatcher().applyPatches(tempInDir, patchDir.convertToPath(), tempOutDir, tempFailedPatchDir)

            out.writeZip { zos ->
                failed.writeZip { failedZos ->
                    inputFile.convertToPath().readZip { zis, zipEntry ->
                        if (!zipEntry.name.endsWith(".java")) {
                            copyEntry(zis, zos, zipEntry)
                        } else {
                            val patchedFile = tempOutDir.resolve(zipEntry.name)
                            if (patchedFile.exists()) {
                                patchedFile.inputStream().buffered().use { input ->
                                    copyEntry(input, zos, zipEntry)
                                }
                            }
                            val failedPatch = tempFailedPatchDir.resolve(zipEntry.name)
                            if (failedPatch.exists()) {
                                failedPatch.inputStream().buffered().use { input ->
                                    copyEntry(input, failedZos, zipEntry)
                                }
                            }
                        }
                    }
                }
            }

            val patchRoot = patchDir.convertToPath()
            if (result is PatchFailure) {
                result.failures
                    .map { "Patch failed: ${it.patch.relativeTo(patchRoot)}: ${it.details}" }
                    .forEach { logger.error(it) }
                throw Exception("Failed to apply ${result.failures.size} patches")
            }
        } finally {
            tempInDir.deleteRecursively()
            tempOutDir.deleteRecursively()
            tempFailedPatchDir.deleteRecursively()
        }
    }

    internal open fun createPatcher(): Patcher {
        return if (useNativeDiff.get()) {
            NativePatcher(exec, patchExecutable.get())
        } else {
            JavaPatcher()
        }
    }
}
