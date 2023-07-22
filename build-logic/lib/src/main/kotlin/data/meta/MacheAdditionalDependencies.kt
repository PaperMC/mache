package io.papermc.mache.lib.data.meta

import kotlinx.serialization.Serializable

@Serializable
data class MacheAdditionalDependencies(
    val compileOnly: List<MavenArtifact>? = null,
    val implementation: List<MavenArtifact>? = null,
)
