package io.papermc.mache.lib.data.meta

import kotlinx.serialization.Serializable

@Serializable
data class MacheDependencies(
    val codebook: List<MavenArtifact>,
    val paramMappings: List<MavenArtifact>,
    val constants: List<MavenArtifact>,
    val remapper: List<MavenArtifact>,
    val decompiler: List<MavenArtifact>,
)
