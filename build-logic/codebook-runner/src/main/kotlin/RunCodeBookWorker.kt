package io.papermc.mache.codebook

import io.papermc.codebook.CodeBook
import io.papermc.codebook.config.CodeBookClasspathResource
import io.papermc.codebook.config.CodeBookContext
import io.papermc.codebook.config.CodeBookFileResource
import io.papermc.codebook.config.CodeBookJarInput
import java.io.PrintStream
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

abstract class RunCodeBookWorker : WorkAction<RunCodeBookWorker.RunCodebookParameters> {
    interface RunCodebookParameters : WorkParameters {
        val tempDir: DirectoryProperty
        val serverJar: RegularFileProperty
        val classpath: ConfigurableFileCollection
        val remapperClasspath: ConfigurableFileCollection
        val serverMappings: RegularFileProperty
        val paramMappings: ConfigurableFileCollection
        val constants: ConfigurableFileCollection
        val outputJar: RegularFileProperty
        val logs: RegularFileProperty
    }

    override fun execute() {
        val tempDir = parameters.tempDir.get().asFile.toPath()
        if (tempDir.exists()) {
            tempDir.toFile().deleteRecursively()
        }
        tempDir.createDirectories()

        val logs = parameters.logs.get().asFile.toPath()
        logs.deleteIfExists()
        PrintStream(logs.outputStream()).use { output ->
            val oldOut = System.out
            val oldErr = System.err
            System.setOut(output)
            System.setErr(output)

            try {
                run(tempDir)
            } finally {
                System.setOut(oldOut)
                System.setErr(oldErr)
            }
        }
    }

    private fun run(tempDir: Path) {
        try {
            val ctx = CodeBookContext(
                parameters.tempDir.get().asFile.toPath().absolute(),
                null,
                CodeBookClasspathResource(parameters.remapperClasspath.files.mapTo(ArrayList()) { it.toPath().absolute() }),
                CodeBookFileResource(parameters.serverMappings.get().asFile.toPath().absolute()),
                CodeBookFileResource(parameters.paramMappings.singleFile.toPath().absolute()),
                CodeBookFileResource(parameters.constants.singleFile.toPath().absolute()),
                parameters.outputJar.get().asFile.toPath().absolute(),
                false,
                CodeBookJarInput(
                    parameters.serverJar.get().asFile.toPath().absolute(),
                    parameters.classpath.files.mapTo(ArrayList()) { it.toPath().absolute() },
                ),
            )
            CodeBook(ctx).exec()
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
