package io.papermc.mache.util.patches

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import org.gradle.process.ExecOperations

internal open class NativePatcher(private val exec: ExecOperations, protected val patchExecutable: String) : Patcher {

    override fun applyPatches(baseDir: Path, patchDir: Path, outputDir: Path): PatchResult {
        baseDir.walk()
            .filter { it.name.endsWith(".java") }
            .forEach { original ->
                val relPath = original.relativeTo(baseDir)
                val patched = outputDir.resolve(relPath.toString())
                patched.parent.createDirectories()
                original.copyTo(patched)
            }

        val result = patchDir.walk()
            .fold<_, PatchResult>(PatchSuccess) { acc, value ->
                acc.fold(patch(outputDir, value))
            }

        return result
    }

    internal open fun commandLineArgs(patch: Path): List<String> {
        return listOf(patchExecutable, "-u", "-p1", "--merge=diff3", "-i", patch.absolutePathString())
    }

    private fun patch(out: Path, patch: Path): PatchResult {
        val baos = ByteArrayOutputStream()
        val res = exec.exec {
            commandLine(commandLineArgs(patch))
            workingDir(out)

            standardOutput = baos
            errorOutput = baos

            isIgnoreExitValue = true
        }
        if (res.exitValue == 0) {
            return PatchSuccess
        }

        return PatchFailure(patch, baos.toString(Charset.defaultCharset()))
    }
}
