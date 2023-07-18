package io.papermc.mache.lib.data.api

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftJavaVersion(
    val majorVersion: Int,
)
