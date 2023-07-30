package io.papermc.mache.util

import io.papermc.mache.lib.data.meta.MavenArtifact
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
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

inline fun <R> Path.useZip(func: (Path) -> R): R {
    return FileSystems.newFileSystem(this).use { f ->
        val root = f.getPath("/")
        func(root)
    }
}

inline fun Path.readZip(func: (ZipInputStream, ZipEntry) -> Unit) {
    ZipInputStream(this.inputStream().buffered()).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            func(zis, entry)
            entry = zis.nextEntry
        }
    }
}

inline fun Path.writeZip(func: (ZipOutputStream) -> Unit) {
    ZipOutputStream(this.outputStream().buffered()).use(func)
}

fun copyEntry(input: InputStream, output: ZipOutputStream, entry: ZipEntry) {
    val newEntry = ZipEntry(entry)
    output.putNextEntry(newEntry)
    try {
        input.copyTo(output)
    } finally {
        output.closeEntry()
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

internal fun isNativeDiffAvailable(): Boolean {
    val diffPresent = runCatching {
        ProcessBuilder("diff", "--version")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor() == 0
    }.getOrNull() ?: false
    val patchPresent = runCatching {
        ProcessBuilder("patch", "--version")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor() == 0
    }.getOrNull() ?: false

    return diffPresent && patchPresent
}
