package io.papermc.mache

import java.io.File
import java.lang.Exception
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import org.gradle.api.Project
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider

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
