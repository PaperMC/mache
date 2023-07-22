plugins {
    id("mache")
}

mache {
    minecraftVersion = "1.20.1"
}

dependencies {
    codebook("1.0.0")
    remapper(tiny("0.8.7"))
    decompiler(vineflower("1.9.2-PAPER-SNAPSHOT"))
    yarn("1.20.1+build.9")
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.1")
}
