plugins {
    id("mache")
}

mache {
    minecraftVersion = "1.20.2"
}

dependencies {
    codebook("1.0.6")
    remapper(art("1.0.7"))
    decompiler(vineflower("1.9.3"))
    parchment("1.20.1", "2023.09.03")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}
