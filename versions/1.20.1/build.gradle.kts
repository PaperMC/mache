plugins {
    id("mache")
}

mache {
    minecraftVersion = "1.20.1"
}

dependencies {
    codebook("1.0.0-SNAPSHOT")
    remapper(tiny("0.8.7"))
    decompiler(vineflower("1.9.1"))
    yarn("1.20.1+build.9")
}
