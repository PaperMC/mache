plugins {
    id("mache")
}

mache {
    minecraftVersion = "1.20.1"
}

dependencies {
    codebook("1.0.5")
    remapper(art("1.0.5"))
    decompiler(vineflower("1.9.1"))
    parchment("1.20.1", "2023.07.23")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}
