package io.papermc.mache.tasks

import io.papermc.mache.codebook.RunCodeBookWorker
import io.papermc.mache.util.convertToPath
import io.papermc.mache.util.ensureClean
import javax.inject.Inject
import kotlin.io.path.name
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor

@CacheableTask
abstract class RemapJar : DefaultTask() {

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val serverJar: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val serverMappings: RegularFileProperty

    @get:Classpath
    abstract val codebookClasspath: ConfigurableFileCollection

    @get:CompileClasspath
    abstract val minecraftClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val remapperClasspath: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val paramMappings: ConfigurableFileCollection

    @get:Classpath
    abstract val constants: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:OutputDirectory
    @get:Optional
    abstract val reportsDir: DirectoryProperty

    @get:Inject
    abstract val worker: WorkerExecutor

    @get:Inject
    abstract val layout: ProjectLayout

    @TaskAction
    fun run() {
        val out = outputJar.convertToPath().ensureClean()

        val queue = worker.processIsolation {
            classpath.from(codebookClasspath)
            forkOptions {
                maxHeapSize = "2G"
            }
        }

        val logFile = out.resolveSibling("${out.name}.log")

        queue.submit(RunCodeBookWorker::class) {
            tempDir.set(layout.buildDirectory.dir(".tmp_codebook"))
            serverJar.set(this@RemapJar.serverJar)
            classpath.from(this@RemapJar.minecraftClasspath)
            remapperClasspath.from(this@RemapJar.remapperClasspath)
            serverMappings.set(this@RemapJar.serverMappings)
            paramMappings.from(this@RemapJar.paramMappings)
            constants.from(this@RemapJar.constants)
            outputJar.set(this@RemapJar.outputJar)
            logs.set(logFile.toFile())
            reportsDir.set(this@RemapJar.reportsDir)
        }
    }
}
