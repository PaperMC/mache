plugins {
    id("mache")
}

mache {
    minecraftVersion = "23w44a"
    repositories.register("sonatype snapshots") {
        url.set("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        includeGroups.set(listOf("org.vineflower"))
    }
}

dependencies {
    codebook("1.0.7")
    remapper(art("1.0.7"))
    decompiler(vineflower("1.10.0-SNAPSHOT"))
    parchment("1.20.2", "2023.10.22")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}
