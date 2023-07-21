package io.papermc.mache.tasks

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.PatchFailedException
import io.papermc.mache.convertToPath
import io.papermc.mache.ensureClean
import io.papermc.mache.useZip
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import javax.inject.Inject
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.readLines
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeLines
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Always apply patches")
abstract class ApplyPatches : DefaultTask() {

    @get:Optional
    @get:InputDirectory
    abstract val patchDir: DirectoryProperty

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

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

        val out = outputJar.convertToPath().ensureClean()

        if (!patchesPresent) {
            inputFile.convertToPath().copyTo(out)
            return
        }

        inputFile.convertToPath().useZip { inputRoot ->
            out.useZip(create = true) { outputRoot ->
                val patchRoot = patchDir.convertToPath()

                val result = inputRoot.walk()
                    .filter { it.name.endsWith(".java") }
                    .map { original ->
                        val relPath = original.relativeTo(inputRoot)
                        val patchPath = relPath.resolveSibling("${relPath.name}.patch").toString()
                        val patch = patchRoot.resolve(patchPath)
                        val patched = outputRoot.resolve(original.toString())
                        PatchTask(patch, original, patched)
                    }
                    .fold<_, PatchResult>(PatchSuccess) { acc, value ->
                        acc.fold(applyPatch(value))
                    }

                if (result is PatchFailure) {
                    result.thrown
                        .map { "Patch failed: ${it.patch.relativeTo(patchRoot)}: ${it.thrown.message}" }
                        .forEach { logger.error(it) }
                    throw Exception("Failed to apply patches")
                }
            }
        }
    }

    private data class PatchTask(val patch: Path, val original: Path, val patched: Path)

    private sealed interface PatchResult {
        val thrown: List<PatchFailureDetails>
            get() = emptyList()

        fun fold(next: PatchResult): PatchResult {
            return when (this) {
                PatchSuccess -> next
                is PatchFailure -> PatchFailure(this.thrown + next.thrown)
            }
        }
    }

    private object PatchSuccess : PatchResult
    private data class PatchFailure(override val thrown: List<PatchFailureDetails>) : PatchResult {
        constructor(patch: Path, e: PatchFailedException) : this(listOf(PatchFailureDetails(patch, e)))
    }
    private data class PatchFailureDetails(val patch: Path, val thrown: PatchFailedException)

    private fun applyPatch(task: PatchTask): PatchResult {
        val (patch, original, patched) = task

        patched.parent.createDirectories()
        if (patch.notExists()) {
            original.copyTo(patched)
            return PatchSuccess
        }

        val patchLines = patch.readLines(Charsets.UTF_8)
        val parsedPatch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
        val javaLines = original.readLines(Charsets.UTF_8)

        return try {
            val patchedLines = DiffUtils.patch(javaLines, parsedPatch)
            patched.writeLines(patchedLines, Charsets.UTF_8, CREATE_NEW, WRITE, TRUNCATE_EXISTING)
            PatchSuccess
        } catch (e: PatchFailedException) {
            PatchFailure(patch, e)
        }
    }
}
