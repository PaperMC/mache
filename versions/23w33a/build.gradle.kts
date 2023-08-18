plugins {
    id("mache")
}

mache {
    minecraftVersion = "23w33a"
}

dependencies {
    codebook("1.0.6")
    remapper(art("1.0.7"))
    decompiler(vineflower("1.9.2"))
    parchment("1.20.1", "2023.08.13")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}
