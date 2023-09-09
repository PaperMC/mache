plugins {
    id("mache")
}

mache {
    minecraftVersion = "1.20.2-pre2"
}

repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    codebook("1.0.6")
    remapper(art("1.0.7"))
    decompiler(vineflower("1.10.0-SNAPSHOT"))
    parchment("1.20.1", "2023.09.03")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}
