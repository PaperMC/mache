package io.papermc.mache

import MacheExtension
import io.papermc.mache.codebook.RunCodeBookWorker
import io.papermc.mache.constants.DECOMP_CFG
import io.papermc.mache.constants.DECOMP_JAR
import io.papermc.mache.constants.DOWNLOAD_SERVER_JAR
import io.papermc.mache.constants.MC_MANIFEST
import io.papermc.mache.constants.MC_VERSION
import io.papermc.mache.constants.REMAPPED_JAR
import io.papermc.mache.constants.SERVER_JAR
import io.papermc.mache.constants.SERVER_LIBRARIES_LIST
import io.papermc.mache.constants.SERVER_MAPPINGS
import io.papermc.mache.lib.data.HashConfig
import io.papermc.mache.lib.data.LibrariesList
import io.papermc.mache.lib.data.api.MinecraftManifest
import io.papermc.mache.lib.data.api.MinecraftVersionManifest
import io.papermc.mache.lib.json
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.outputStream
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.useLines
import kotlin.io.path.writeText
import kotlin.math.log
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.resources.TextResource
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkerExecutor

object ConfigureVersionProject {

    fun configure(target: Project, mache: MacheExtension) {
        return target.configure0(mache)
    }

    private fun Project.configure0(mache: MacheExtension) {
        val mcManifestFile: Provider<RegularFile> = rootProject.layout.buildDirectory.file(MC_MANIFEST)
        val mcManifest = json.decodeFromString<MinecraftManifest>(resources.text.fromFile(mcManifestFile).asString())

        val mcVersionManifestFile: Provider<RegularFile> = layout.buildDirectory.file(MC_VERSION)
        val mcVersion = mcManifest.versions.first { it.id == mache.minecraftVersion.get() }
        download.download(mcVersion.url, mcVersionManifestFile, Hash(mcVersion.sha1, HashingAlgorithm.SHA1))

        val manifestResource: TextResource = resources.text.fromFile(mcVersionManifestFile)
        val mcVersionManifest = json.decodeFromString<MinecraftVersionManifest>(manifestResource.asString())

        // must be configured now before the value of the property is read later
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(mcVersionManifest.javaVersion.majorVersion))
            }
        }

        val downloadServerJarFile = layout.buildDirectory.file(DOWNLOAD_SERVER_JAR)
        val serverMappingsFile = layout.buildDirectory.file(SERVER_MAPPINGS)
        downloadServerFiles(download, mcVersionManifest, downloadServerJarFile, serverMappingsFile)

        val serverHash = downloadServerJarFile.convertToPath().hashFile(HashingAlgorithm.SHA256).asHexString()

        val librariesFile = layout.buildDirectory.file(SERVER_LIBRARIES_LIST)
        val libraries = determineLibraries(downloadServerJarFile, serverHash, librariesFile)

        dependencies {
            for (library in libraries) {
                "minecraft"(library)
            }
        }

        val serverJar = layout.buildDirectory.file(SERVER_JAR)
        extractServerJar(downloadServerJarFile, serverHash, serverJar)

        val remappedJar = layout.buildDirectory.file(REMAPPED_JAR)
        remapJar(serverJar, serverHash, serverMappingsFile, remappedJar)

        val decompJar = layout.buildDirectory.file(DECOMP_JAR)
        decompileJar(mache, remappedJar, serverHash, decompJar)
    }

    private fun Project.downloadServerFiles(
        download: DownloadService,
        manifest: MinecraftVersionManifest,
        serverJar: Any,
        serverMappings: Any,
    ) {
        runBlocking {
            awaitAll(
                download.downloadAsync(
                    manifest.downloads.server.url,
                    serverJar,
                    Hash(manifest.downloads.server.sha1, HashingAlgorithm.SHA1),
                ),
                download.downloadAsync(
                    manifest.downloads.serverMappings.url,
                    serverMappings,
                    Hash(manifest.downloads.serverMappings.sha1, HashingAlgorithm.SHA1),
                ) {
                    log("Downloading server jar")
                },
            )
        }
    }

    private val whitespace = Regex("\\s+")
    private fun Project.determineLibraries(jar: Any, serverHash: String, libraries: Any): List<String> {
        val librariesJson = libraries.convertToPath()
        val libs = if (librariesJson.exists()) {
            json.decodeFromString<LibrariesList>(resources.text.fromFile(libraries).asString())
        } else {
            null
        }

        val serverJar = jar.convertToPath()
        if (libs != null) {
            if (serverHash == libs.sha256) {
                return libs.libraries
            }
        }

        val result = FileSystems.newFileSystem(serverJar).use { fs ->
            val librariesList = fs.getPath("/", "META-INF", "libraries.list")

            return@use librariesList.useLines { lines ->
                return@useLines lines.map { line ->
                    val parts = line.split(whitespace)
                    if (parts.size != 3) {
                        throw Exception("libraries.list file is invalid")
                    }
                    return@map parts[1]
                }.toList()
            }
        }

        val resultList = json.encodeToString(LibrariesList(serverHash, result))
        librariesJson.writeText(resultList)
        return result
    }

    private fun extractServerJar(downloadJar: Any, serverHash: String, serverJar: Any) {
        val jar = downloadJar.convertToPath()
        val outputJar = serverJar.convertToPath()

        val (config, match) = outputJar.checkHashConfig(serverHash)
        if (match) {
            return
        }

        outputJar.deleteIfExists()
        config.deleteIfExists()

        val serverJarHash: String
        FileSystems.newFileSystem(jar).use { fs ->
            val versionsList = fs.getPath("/", "META-INF", "versions.list")
            if (versionsList.notExists()) {
                throw Exception("Could not find versions.list")
            }

            val lines = versionsList.readLines()
            if (lines.size != 1) {
                throw Exception("versions.list is invalid")
            }

            val line = lines.first()
            val parts = line.split(whitespace)
            if (parts.size != 3) {
                throw Exception("versions.list line is invalid")
            }

            serverJarHash = parts[0]
            val serverJarInJar = fs.getPath("/", "META-INF", "versions", parts[2])
            if (serverJarInJar.notExists()) {
                throw Exception("Could not find version jar")
            }

            serverJarInJar.copyTo(outputJar)
        }

        val hashConfig = json.encodeToString(HashConfig(serverHash, serverJarHash))
        config.writeText(hashConfig)
    }

    private fun Project.remapJar(
        serverJar: Provider<RegularFile>,
        serverHash: String,
        serverMappings: Provider<RegularFile>,
        remappedJar: Provider<RegularFile>,
    ) {
        val outputJar = remappedJar.convertToPath()
        val (config, match) = outputJar.checkHashConfig(serverHash)
        if (match) {
            return
        }

        outputJar.deleteIfExists()
        config.deleteIfExists()

        log("Remapping jar")

        val workerExecutor = serviceOf<WorkerExecutor>()
        val queue = workerExecutor.processIsolation {
            classpath.from(configurations.named("codebook"))
            forkOptions {
                maxHeapSize = "2G"
            }
        }

        val logFile = outputJar.resolveSibling("${outputJar.name}.log")

        queue.submit(RunCodeBookWorker::class) {
            this.tempDir.set(layout.buildDirectory.dir(".tmp_codebook"))
            this.serverJar.set(serverJar)
            classpath.from(configurations.named("minecraft"))
            remapperClasspath.from(configurations.named("remapper"))
            this.serverMappings.set(serverMappings)
            paramMappings.from(configurations.named("paramMappings"))
            constants.from(configurations.named("constants"))
            this.outputJar.set(remappedJar)
            logs.set(logFile.toFile())
        }

        queue.await()

        val remappedJarHash = outputJar.hashFile(HashingAlgorithm.SHA256).asHexString()
        val hashConfig = json.encodeToString(HashConfig(serverHash, remappedJarHash))
        config.writeText(hashConfig)
    }

    private fun Project.decompileJar(
        mache: MacheExtension,
        inputJar: Any,
        serverHash: String,
        decompJar: Any,
    ) {
        val outputJar = decompJar.convertToPath()
        val (config, match) = outputJar.checkHashConfig(serverHash)
        if (match) {
            return
        }

        outputJar.deleteIfExists()
        config.deleteIfExists()

        val libFiles = configurations.named("minecraft").get().resolve()
        val cfgFile = layout.buildDirectory.file(DECOMP_CFG).get().asFile.toPath()
        cfgFile.deleteIfExists()
        val cfgText = buildString {
            for (libFile in libFiles) {
                append("-e=")
                append(libFile.toPath().absolute())
                append(System.lineSeparator())
            }
        }
        cfgFile.writeText(cfgText)

        log("Decompiling jar")

        outputJar.resolveSibling("${outputJar.normalize()}.log")

        val exec = serviceOf<ExecOperations>()
        outputJar.outputStream().buffered().use { log ->
            exec.javaexec {
                classpath(configurations.named("decompiler"))
                mainClass.set("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler")

                maxHeapSize = "2G"

                args(mache.decompilerArgs.get())
                args("-cfg", cfgFile.absolutePathString())

                args(inputJar.convertToPath().absolutePathString())
                args(outputJar.absolutePathString())

                standardOutput = log
                errorOutput = log
            }
        }

        val decompJarHash = outputJar.hashFile(HashingAlgorithm.SHA256).asHexString()
        val hashConfig = json.encodeToString(HashConfig(serverHash, decompJarHash))
        config.writeText(hashConfig)
    }

    private fun Path.checkHashConfig(serverHash: String): Pair<Path, Boolean> {
        val config = resolveSibling("$name.config.json")
        if (notExists() || config.notExists()) {
            return config to false
        }

        val hashConfig = json.decodeFromString<HashConfig>(config.readText())
        if (hashConfig.serverHash != serverHash) {
            return config to false
        }

        return config to (hashConfig.outputHash == hashFile(HashingAlgorithm.SHA256).asHexString())
    }

    private fun Project.log(msg: String) {
        println("$path:$msg")
    }
}
