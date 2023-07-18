package io.papermc.mache.lib.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MinecraftVersionDownloads(
    val server: MinecraftDownload,
    @SerialName("server_mappings") val serverMappings: MinecraftDownload,
)
