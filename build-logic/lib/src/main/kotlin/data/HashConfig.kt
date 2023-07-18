package io.papermc.mache.lib.data

import kotlinx.serialization.Serializable

@Serializable
data class HashConfig(val serverHash: String, val outputHash: String)
