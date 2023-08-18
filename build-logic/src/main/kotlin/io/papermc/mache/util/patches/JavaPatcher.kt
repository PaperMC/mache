package io.papermc.mache.util.patches

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch
import com.github.difflib.patch.PatchFailedException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.readLines
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeLines

internal open class JavaPatcher : Patcher {

    override fun applyPatches(baseDir: Path, patchDir: Path, outputDir: Path, failedDir: Path): PatchResult {
        var result = baseDir.walk()
            .filter { it.name.endsWith(".java") }
            .map { original ->
                val relPath = original.relativeTo(baseDir)
                val patchPath = relPath.resolveSibling("${relPath.name}.patch").toString()
                val patch = patchDir.resolve(patchPath)
                val patched = outputDir.resolve(relPath.toString())
                PatchTask(patch, original, patched)
            }
            .fold<_, PatchResult>(PatchSuccess) { acc, value ->
                acc.fold(applyPatch(value))
            }

        result = patchDir.walk()
            .filter { it.name.endsWith(".patch") }
            .filterNot { result.patches.contains(it) }
            .map { patch ->
                // all patches here did not have matching files
                // this results in a patch failure too
                val relPath = patch.relativeTo(patchDir)
                PatchFailure(patch, "No matching file found for patch: " + relPath.pathString)
            }
            .fold(result) { acc, value ->
                acc.fold(value)
            }

        return result
    }

    internal data class PatchTask(val patch: Path, val original: Path, val patched: Path)

    internal open fun applyPatch(task: PatchTask): PatchResult {
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
            val patchedLines = applyPatch(parsedPatch, javaLines)
            patched.writeLines(
                patchedLines,
                Charsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
            )
            PatchSuccess(patch)
        } catch (e: PatchFailedException) {
            // patch failed, so copy the file over without the patch applied
            original.copyTo(patched, overwrite = true)
            PatchFailure(patch, e.message ?: "unknown")
        }
    }

    @Throws(PatchFailedException::class)
    internal open fun applyPatch(patch: Patch<String>, lines: List<String>): List<String> {
        return DiffUtils.patch(lines, patch)
    }
}
