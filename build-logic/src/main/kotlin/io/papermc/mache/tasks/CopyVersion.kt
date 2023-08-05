package io.papermc.mache.tasks

import io.papermc.mache.util.convertToPath
import javax.inject.Inject
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option

@UntrackedTask(because = "CLI command")
abstract class CopyVersion : DefaultTask() {

    @get:Option(option = "from-version", description = "The version to copy")
    @get:Input
    abstract val fromVersion: Property<String>

    @get:Option(option = "to-version", description = "The target version")
    @get:Input
    abstract val toVersion: Property<String>

    @get:Inject
    abstract val layout: ProjectLayout

    @TaskAction
    fun run() {
        val from = fromVersion.get()
        val to = toVersion.get()

        val projDir = layout.projectDirectory.convertToPath()
        val versionsDir = projDir.resolve("versions")
        val fromDir = versionsDir.resolve(from)
        val toDir = versionsDir.resolve(to)

        if (fromDir.notExists()) {
            throw Exception("--from-version directory does not exist: $from")
        }
        if (toDir.exists()) {
            throw Exception("Cannot migrate version, target already exists: $to")
        }

        toDir.createDirectories()
        fromDir.resolve("patches").copyToRecursively(toDir.resolve("patches"), followLinks = false, overwrite = false)
        fromDir.resolve("build.gradle.kts").copyTo(toDir.resolve("build.gradle.kts"))
    }
}
