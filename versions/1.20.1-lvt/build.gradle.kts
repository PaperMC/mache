plugins {
    id("mache")
}

mache {
    minecraftVersion = "1.20.1"

    decompilerArgs.set(listOf(
        "-nns=true",
        "-tcs=true",
        "-ovr=false",
        "-vvm=true",
        "-iec=true",
        "-jrt=current",
        "-ind=    ",
        "-jvn=false",
        "-dcc=true",
        "-sef=true",
        "-nls=1",
    ))
}

dependencies {
    codebook("1.0.1")
    remapper(tiny("0.8.7"))
    decompiler(vineflower("1.9.2-PAPER-SNAPSHOT"))
    yarn("1.20.1+build.10")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}
