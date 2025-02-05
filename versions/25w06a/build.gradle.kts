import io.papermc.sculptor.shared.util.MinecraftJarType

plugins {
    id("io.papermc.sculptor.version") version "1.0.11"
}

val generateReportsProperty = providers.gradleProperty("generateReports")
mache {
    minecraftVersion = "25w06a"
    minecraftJarType = MinecraftJarType.SERVER

    repositories.register("sonatype snapshots") {
        url = "https://repo.papermc.io/repository/maven-public/"
        includeGroups.add("org.vineflower")
    }

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
    codebook("1.0.13")
    remapper(art("2.0.5"))
    decompiler(vineflower("1.11.0-20241204.173358-53"))
    parchment("1.21.4", "2024.12.07")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
