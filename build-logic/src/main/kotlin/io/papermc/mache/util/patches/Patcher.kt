package io.papermc.mache.util.patches

import java.nio.file.Path

internal interface Patcher {

    fun applyPatches(baseDir: Path, patchDir: Path, outputDir: Path, failedDir: Path): PatchResult
}

internal sealed interface PatchResult {
    val patches: List<Path>
    val failures: List<PatchFailureDetails>
        get() = emptyList()

    fun fold(next: PatchResult): PatchResult {
        return when {
            this is PatchSuccess && next is PatchSuccess -> PatchSuccess(this.patches + next.patches)
            else -> PatchFailure(this.patches + next.patches, this.failures + next.failures)
        }
    }
}

internal sealed interface PatchSuccess : PatchResult {
    companion object : PatchSuccess {

        operator fun invoke(patches: List<Path> = emptyList()): PatchSuccess = PatchSuccessFile(patches)
        operator fun invoke(patch: Path): PatchSuccess = PatchSuccessFile(listOf(patch))

        override val patches: List<Path>
            get() = emptyList()
    }
}

private data class PatchSuccessFile(override val patches: List<Path>) : PatchSuccess

internal data class PatchFailure(override val patches: List<Path>, override val failures: List<PatchFailureDetails>) : PatchResult {
    constructor(patch: Path, details: String) : this(listOf(patch), listOf(PatchFailureDetails(patch, details)))
}

internal data class PatchFailureDetails(val patch: Path, val details: String)
