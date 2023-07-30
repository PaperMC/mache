package io.papermc.mache.util.patches

import java.nio.file.Path
import kotlin.io.path.absolutePathString
import org.gradle.process.ExecOperations

internal class NativePatcherFuzzy(exec: ExecOperations, ex: String, private val maxFuzz: Int) : NativePatcher(exec, ex) {

    init {
        if (maxFuzz < 0) {
            throw IllegalArgumentException("max-fuzz argument must be a non-negative integer")
        }
    }

    override fun commandLineArgs(patch: Path): List<String> {
        return listOf(patchExecutable, "-u", "-p1", "--fuzz=$maxFuzz", "--merge=diff3", "-i", patch.absolutePathString())
    }
}
