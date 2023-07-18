package io.papermc.mache.lib.data

import kotlinx.serialization.Serializable

@Serializable
data class LibrariesList(
    val sha256: String,
    val libraries: List<String>,
)
