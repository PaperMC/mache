package io.papermc.mache.util

import io.papermc.mache.lib.data.meta.MavenArtifact
import java.io.File
import java.lang.Exception
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.newInstance

internal val whitespace = Regex("\\s+")

@Suppress("UNCHECKED_CAST")
val Project.download: DownloadService
    get() = (gradle.sharedServices.registrations.getByName("download").service as Provider<DownloadService>).get()

internal fun Any.convertToUri(): URI {
    return when (this) {
        is URI -> this
        is URL -> this.toURI()
        is String -> URI.create(this)
        is Provider<*> -> this.get().convertToUri()
        else -> throw Exception("Unknown URL type: ${this.javaClass.name}")
    }
}

internal fun Any.convertToPath(): Path {
    return when (this) {
        is Path -> this
        is File -> this.toPath()
        is FileSystemLocation -> this.asFile.toPath()
        is Provider<*> -> this.get().convertToPath()
        else -> throw Exception("Unknown type representing a file: ${this.javaClass.name}")
    }
}

internal fun Path.ensureClean(): Path {
    deleteRecursively()
    parent.createDirectories()
    return this
}

inline fun <R> Path.useZip(create: Boolean = false, func: (Path) -> R): R {
    val fs = if (create) {
        FileSystems.newFileSystem(this, mapOf("create" to true))
    } else {
        FileSystems.newFileSystem(this)
    }

    return fs.use { f ->
        val root = f.getPath("/")
        func(root)
    }
}

abstract class GradleMavenArtifact {
    @get:Input
    abstract val group: Property<String>

    @get:Input
    abstract val name: Property<String>

    @get:Input
    abstract val version: Property<String>

    @get:Input
    @get:Optional
    abstract val classifier: Property<String>

    @get:Input
    @get:Optional
    abstract val extension: Property<String>

    fun toMavenArtifact(): MavenArtifact {
        return MavenArtifact(group.get(), name.get(), version.get(), classifier.orNull, extension.orNull)
    }
}

fun Project.asGradleMavenArtifacts(conf: Configuration): List<GradleMavenArtifact> {
    return conf.dependencies.map { dep ->
        val group = dep.group ?: throw IllegalStateException("Dependency without group: $dep")
        val name = dep.name
        val version = dep.version ?: throw IllegalStateException("Dependency without version: $dep")
        val classifier = (dep as? ExternalDependency)?.artifacts?.firstNotNullOfOrNull { it.classifier }
        val extension = (dep as? ExternalDependency)?.artifacts?.firstNotNullOfOrNull { it.extension }?.takeIf { it != "jar" }

        return@map objects.newInstance(GradleMavenArtifact::class).apply Maven@{
            this@Maven.group.set(group)
            this@Maven.name.set(name)
            this@Maven.version.set(version)
            if (classifier != null) {
                this@Maven.classifier.set(classifier)
            }
            if (extension != null) {
                this@Maven.extension.set(extension)
            }
        }
    }
}
