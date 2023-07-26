plugins {
    id("mache")
}

mache {
    minecraftVersion = "1.20.1"
}

dependencies {
    codebook("1.0.1")
    remapper(tiny("0.8.7"))
    decompiler(vineflower("1.9.1"))
    yarn("1.20.1+build.10")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}

tasks {
    remapJar {
        logMissingLvtSuggestions = true
    }
}
