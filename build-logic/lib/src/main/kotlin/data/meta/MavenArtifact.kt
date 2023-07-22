package io.papermc.mache.lib.data.meta

import kotlinx.serialization.Serializable

@Serializable
data class MavenArtifact(
    val group: String,
    val name: String,
    val version: String,
    val classifier: String? = null,
    val extension: String? = null,
)
