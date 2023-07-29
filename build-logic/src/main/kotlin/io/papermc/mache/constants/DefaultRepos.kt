package io.papermc.mache.constants

import io.papermc.mache.util.SimpleMacheRepo

object DefaultRepos {

    val DEFAULTS: List<SimpleMacheRepo> = listOf(
        // codebook
        SimpleMacheRepo("https://repo.papermc.io/repository/maven-public/", "PaperMC", listOf("io.papermc")),
        // parchment mappings
        SimpleMacheRepo("https://maven.parchmentmc.org/", "ParchmentMC", listOf("org.parchmentmc")),
        // remapper
        SimpleMacheRepo("https://maven.neoforged.net/releases/", "NeoForged", listOf("net.neoforged", "net.minecraftforge")),
        // unpick
        SimpleMacheRepo("https://maven.fabricmc.net/", "FabricMC", listOf("net.fabricmc")),
    )
}
