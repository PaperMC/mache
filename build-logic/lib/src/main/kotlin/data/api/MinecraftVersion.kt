package io.papermc.mache.lib.data.api

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftVersion(
    val id: String,
    val type: String,
    val time: String,
    val releaseTime: String,
    val url: String,
    val sha1: String,
)
