package io.papermc.mache.util.patches

import java.nio.file.Path

internal interface Patcher {

    fun applyPatches(baseDir: Path, patchDir: Path, outputDir: Path): PatchResult
}

internal sealed interface PatchResult {
    val failures: List<PatchFailureDetails>
        get() = emptyList()

    fun fold(next: PatchResult): PatchResult {
        return when (this) {
            PatchSuccess -> next
            is PatchFailure -> PatchFailure(this.failures + next.failures)
        }
    }
}

internal object PatchSuccess : PatchResult
internal data class PatchFailure(override val failures: List<PatchFailureDetails>) : PatchResult {
    constructor(patch: Path, details: String) : this(listOf(PatchFailureDetails(patch, details)))
}
internal data class PatchFailureDetails(val patch: Path, val details: String)
