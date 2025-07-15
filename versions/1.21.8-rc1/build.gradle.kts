import io.papermc.sculptor.shared.util.MinecraftJarType

plugins {
    id("io.papermc.sculptor.version") version "1.0.11"
}

val generateReportsProperty = providers.gradleProperty("generateReports")
mache {
    minecraftVersion = "1.21.8-rc1"
    minecraftJarType = MinecraftJarType.SERVER

    val args = mutableListOf(
        "--temp-dir={tempDir}",
        "--remapper-file={remapperFile}",
        "--mappings-file={mappingsFile}",
        "--params-file={paramsFile}",
        // "--constants-file={constantsFile}",
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

    remapperArgs.set(args)
}

dependencies {
    codebook("1.0.14")
    remapper(art("2.0.5"))
    decompiler(vineflower("1.11.1"))
    paramMappings("io.papermc.parchment.data:parchment-25w21a:2025.05.28")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.checkerframework:checker-qual:3.49.0")
}
