import io.papermc.sculptor.shared.util.MinecraftJarType

plugins {
    id("io.papermc.sculptor.version") version "2.0.0-SNAPSHOT"
}

val generateReportsProperty = providers.gradleProperty("generateReports")
mache {
    minecraftVersion = "25w45a"
    minecraftJarType = MinecraftJarType.SERVER

    val args = mutableListOf(
        "--temp-dir={tempDir}",
        "--unpick-file={constantsFile}",
        "--output={output}",
        "--input={input}",
        "--input-classpath={inputClasspath}",
        "--hypo-parallelism=1",
    )
    if (generateReportsProperty.getOrElse("false").toBooleanStrict()) {
        args.addAll(listOf(
            "--reports-dir={reportsDir}",
            "--all-reports",
        ))
    }

    codebookArgs = args
    serverJarOverrideUrl = "https://piston-data.mojang.com/v1/objects/2d3568cd561daad8e80fdc605e64270ae8dd8aba/server.jar"
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

dependencies {
    codebook("2.0.0-SNAPSHOT")
    decompiler(vineflower("1.11.2"))
    constants("io.papermc.parchment.data:parchment:25w45a+build.4")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.checkerframework:checker-qual:3.49.0")
}
