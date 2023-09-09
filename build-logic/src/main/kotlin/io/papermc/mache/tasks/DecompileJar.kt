package io.papermc.mache.tasks

import io.papermc.mache.constants.DECOMP_CFG
import io.papermc.mache.util.convertToPath
import io.papermc.mache.util.ensureClean
import io.papermc.mache.util.useZip
import javax.inject.Inject
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.writeText
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

@CacheableTask
abstract class DecompileJar : DefaultTask() {

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:Input
    abstract val decompilerArgs: ListProperty<String>

    @get:CompileClasspath
    abstract val minecraftClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val decompiler: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:Inject
    abstract val exec: ExecOperations

    @get:Inject
    abstract val layout: ProjectLayout

    @TaskAction
    fun run() {
        val out = outputJar.convertToPath().ensureClean()

        val cfgFile = layout.buildDirectory.file(DECOMP_CFG).convertToPath().ensureClean()
        val cfgText = buildString {
            for (file in minecraftClasspath.files) {
                append("-e=")
                append(file.toPath().absolutePathString())
                append(System.lineSeparator())
            }
        }
        cfgFile.writeText(cfgText)

        val logs = out.resolveSibling("${out.name}.log")

        logs.outputStream().buffered().use { log ->
            exec.javaexec {
                classpath(decompiler)
                mainClass.set("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler")

                maxHeapSize = "3G"

                args(decompilerArgs.get())
                args("-cfg", cfgFile.absolutePathString())

                args(inputJar.convertToPath().absolutePathString())
                args(out.absolutePathString())

                standardOutput = log
                errorOutput = log
            }
        }

        out.useZip { root ->
            root.resolve("META-INF").resolve("MANIFEST.MF").deleteIfExists()
        }
    }
}
