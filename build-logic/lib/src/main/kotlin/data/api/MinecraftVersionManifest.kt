package io.papermc.mache.lib.data.api

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftVersionManifest(
    val downloads: MinecraftVersionDownloads,
    val javaVersion: MinecraftJavaVersion,
)
