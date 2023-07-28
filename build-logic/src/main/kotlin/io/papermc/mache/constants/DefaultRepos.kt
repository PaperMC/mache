package io.papermc.mache.constants

import io.papermc.mache.util.SimpleMacheRepo

object DefaultRepos {

    val DEFAULTS: List<SimpleMacheRepo> = listOf(
        SimpleMacheRepo("https://repo.papermc.io/repository/maven-public/", "PaperMC", listOf("io.papermc")),
        SimpleMacheRepo("https://maven.fabricmc.net/", "FabricMC", listOf("net.fabricmc")),
    )
}
