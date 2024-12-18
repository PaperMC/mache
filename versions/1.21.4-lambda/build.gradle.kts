import io.papermc.sculptor.shared.util.MinecraftJarType

plugins {
    id("io.papermc.sculptor.version") version "1.0.11"
}

val generateReportsProperty = providers.gradleProperty("generateReports")
mache {
    minecraftVersion = "1.21.4"
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

    decompilerArgs.set(
        listOf(
            // Treat some known structures as synthetic even when not explicitly set
            "--synthetic-not-set=true",
            // Fold branches of ternary expressions that have boolean true and false constants
            "--ternary-constant-simplification=true",
            // Give the decompiler information about the Java runtime
            "--include-runtime=current",
            // Decompile complex constant-dynamic expressions:
            // Some constant-dynamic expressions can't be converted to a single Java expression with
            // identical run-time behaviour. This decompiles them to a similar non-lazy expression,
            // marked with a comment
            "--decompile-complex-constant-dynamic=true",
            // Indent String
            "--indent-string=    ",
            // Process inner classes and add them to the decompiled output.
            "--decompile-inner=true", // default
            // Removes any methods that are marked as bridge from the decompiled output.
            "--remove-bridge=true", // default
            // Decompile generics in classes, methods, fields, and variables.
            "--decompile-generics=true", // default
            // Encode non-ASCII characters in string and character literals as Unicode escapes.
            "--ascii-strings=false", // default
            // Removes any methods and fields that are marked as synthetic from the decompiled output.
            "--remove-synthetic=true", // default
            // Give the decompiler information about every jar on the classpath.
            "--include-classpath=true",
            // Remove braces on simple, one line, lambda expressions.
            "--inline-simple-lambdas=false",
            // Ignore bytecode that is malformed.
            "--ignore-invalid-bytecode=false", // default
            // Map Bytecode to source lines.
            "--bytecode-source-mapping=true",
            // Dump line mappings to output archive zip entry extra data.
            "--dump-code-lines=true",
            // Display override annotations for methods known to the decompiler
            "--override-annotation=false",
            // Skip copying non-class files from the input folder or file to the output
            "--skip-extra-files=true",

        ),
    )
}

dependencies {
    codebook("1.0.12")
    remapper(art("2.0.5"))
    decompiler(vineflower("1.11.0-20241204.173358-53"))
    parchment("1.21.4", "2024.12.07")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
