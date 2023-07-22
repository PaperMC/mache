package io.papermc.mache.tasks

import io.papermc.mache.constants.DefaultRepos
import io.papermc.mache.lib.data.maven.MavenMetadata
import io.papermc.mache.lib.data.meta.MacheMeta
import io.papermc.mache.lib.data.meta.MavenArtifact
import io.papermc.mache.lib.json
import io.papermc.mache.lib.xml
import io.papermc.mache.util.HashingAlgorithm
import io.papermc.mache.util.asHexString
import io.papermc.mache.util.convertToPath
import io.papermc.mache.util.ensureClean
import io.papermc.mache.util.hashFile
import io.papermc.mache.util.useZip
import java.lang.IllegalStateException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.decodeFromString
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class OpenVersion : DefaultTask() {

    @get:Input
    @get:Option(option = "version-name", description = "The version to open.")
    abstract val versionName: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "dir-name", description = "Optional name for the directory to create in the versions/ directory.")
    abstract val directoryName: Property<String>

    @get:Input
    abstract val repoUrl: Property<String>

    @get:Inject
    abstract val layout: ProjectLayout

    @TaskAction
    fun run() {
        val version = versionName.get()

        val client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build()

        val exactVersion = determineExactVersion(client, version)
        val fullUrl = repoUrl.get().removeSuffix("/") + "/io/papermc/mache/$exactVersion/mache-$exactVersion.zip"

        val tempFile = layout.buildDirectory.file("tmp/mache-$exactVersion.zip").convertToPath().ensureClean()

        val request = HttpRequest.newBuilder().GET().uri(URI.create(fullUrl)).build()
        val response = client.send(request, BodyHandlers.ofFile(tempFile))
        if (response.statusCode() !in 200..299) {
            throw Exception("Failed to successfully download file from: $fullUrl")
        }

        val resultFile = response.body()
        checkHash(client, fullUrl, resultFile)

        resultFile.useZip { root ->
            createVersionDirectory(resultFile, root)
        }
    }

    private fun createVersionDirectory(zip: Path, root: Path) {
        val macheJson = root.resolve("mache.json")
        if (macheJson.notExists()) {
            throw Exception("Malformed mache artifact: $zip")
        }

        val macheMeta = json.decodeFromString<MacheMeta>(macheJson.readText())
        val versionsDirectory = layout.projectDirectory.dir("versions").convertToPath()

        val thisVersionName = if (directoryName.isPresent) {
            directoryName.get()
        } else {
            macheMeta.version
        }

        val outputDir = versionsDirectory.resolve(thisVersionName)
        if (outputDir.exists()) {
            throw Exception(
                "Cannot create a new version directory of path: $outputDir, already exists. " +
                    "Specify a different directory name with --dir-name.",
            )
        }

        outputDir.createDirectories()
        root.resolve("patches").copyToRecursively(outputDir.resolve("patches"), followLinks = false, overwrite = false)

        val buildGradleText = writeBuildGradle(macheMeta).replace("\n", System.lineSeparator())
        outputDir.resolve("build.gradle.kts").writeText(buildGradleText)
    }

    private fun determineExactVersion(client: HttpClient, version: String): String {
        if (version.contains("+")) {
            // appears to contain build descriptor
            // assume this is the exact version
            return version
        }

        val metaUrl = repoUrl.get().removeSuffix("/") + "/io/papermc/mache/maven-metadata.xml"
        val meta = client.getXml(metaUrl)

        val buildIdentifier = "+build."
        val maxVersion = meta.versioning.versions.asSequence()
            // our version scheme always includes build identifiers
            .filter { it.contains(buildIdentifier) }
            // don't naively assume it.startWith() would work - pre-release versions would
            // also match
            .filter { it.substringBefore(buildIdentifier) == version }
            .maxByOrNull { it.substringAfter(buildIdentifier).toIntOrNull() ?: -1 }

        return maxVersion ?: throw IllegalStateException("Could not find matching version from: $version")
    }

    private fun writeBuildGradle(meta: MacheMeta): String = buildString {
        appendLine(
            """
            plugins {
                id("mache")
            }
            """.trimIndent(),
        )
        appendLine()

        appendLine("mache {")
        append(indent(1)).appendLine("minecraftVersion = \"${meta.version}\"")

        // in most cases, repos probably won't be needed
        val defaultUrls = DefaultRepos.DEFAULTS.mapTo(HashSet()) { it.url }
        if (meta.repositories.any { it.url !in defaultUrls }) {
            appendLine()
        }

        for (rep in meta.repositories) {
            if (rep.url in defaultUrls) {
                continue
            }

            append(indent(1)).appendLine("repositories.register(\"${rep.name}\") {")
            append(indent(2)).appendLine("url = \"${rep.url}\"")

            rep.groups?.forEach { group ->
                append(indent(2)).appendLine("includeGroups.add(\"$group\")")
            }
            append(indent(1)).appendLine("}")
        }
        appendLine("}")
        appendLine()

        // first set of dependencies is the codebook-related dependencies
        appendLine("dependencies {")

        for (dep in meta.dependencies.codebook) {
            if (dep.matches("io.papermc.codebook:codebook")) {
                append(indent(1)).appendLine("codebook(\"${dep.version}\")")
            } else {
                appendMavenDep(dep, "codebook", quoteConfiguration = true)
            }
        }

        for (dep in meta.dependencies.remapper) {
            if (dep.matches("net.fabricmc:tiny-remapper")) {
                append(indent(1)).appendLine("remapper(tiny(\"${dep.version}\"))")
            } else {
                appendMavenDep(dep, "remapper")
            }
        }

        for (dep in meta.dependencies.decompiler) {
            if (dep.matches("org.vineflower:vineflower")) {
                append(indent(1)).appendLine("decompiler(vineflower(\"${dep.version}\"))")
            } else {
                appendMavenDep(dep, "decompiler")
            }
        }

        val param = meta.dependencies.paramMappings.find { it.matches("net.fabricmc:yarn", classifier = "mergedv2") }
        val const = meta.dependencies.constants.find { it.matches("net.fabricmc:yarn", classifier = "constants") }
        val yarnShorthand = param != null && const != null && param.version == const.version
        if (yarnShorthand) {
            append(indent(1)).appendLine("yarn(\"${param!!.version}\")")
        }
        for (paramMapping in meta.dependencies.paramMappings) {
            if (!yarnShorthand || paramMapping != param) {
                appendMavenDep(paramMapping, "paramMappings")
            }
        }
        for (constant in meta.dependencies.constants) {
            if (!yarnShorthand || constant != const) {
                appendMavenDep(constant, "constants")
            }
        }

        appendLine("}")

        // we place additional dependencies in a separate block
        if (meta.additionalCompileDependencies?.compileOnly?.isNotEmpty() == true ||
            meta.additionalCompileDependencies?.implementation?.isNotEmpty() == true
        ) {
            appendLine()
            appendLine("dependencies {")

            meta.additionalCompileDependencies?.compileOnly?.forEach { dep ->
                appendMavenDep(dep, "compileOnly")
            }
            meta.additionalCompileDependencies?.implementation?.forEach { dep ->
                appendMavenDep(dep, "implementation")
            }

            appendLine("}")
        }
    }

    private fun StringBuilder.appendMavenDep(dep: MavenArtifact, configuration: String, quoteConfiguration: Boolean = false) {
        append(indent(1))
        if (quoteConfiguration) {
            append('"').append(configuration).append('"')
        } else {
            append(configuration)
        }
        append("""("${dep.group}:${dep.name}:${dep.version}")""")
        if (dep.isSimple()) {
            appendLine()
        } else {
            appendLine(" {")
            append(indent(2)).appendLine("artifact {")
            if (dep.classifier != null) {
                append(indent(3)).appendLine("classifier = \"${dep.classifier}\"")
            }
            if (dep.extension != null) {
                append(indent(3)).appendLine("extension = \"${dep.extension}\"")
            }
            append(indent(2)).appendLine("}")
            append(indent(1)).appendLine("}")
        }
    }

    private fun indent(n: Int): String {
        return "    ".repeat(n)
    }

    private fun MavenArtifact.matches(module: String, classifier: String? = null, extension: String? = null): Boolean {
        return group == module.substringBefore(':') &&
            name == module.substringAfter(':') &&
            classifier == this.classifier &&
            extension == this.extension
    }

    private fun MavenArtifact.isSimple(): Boolean {
        return classifier == null && extension == null
    }

    private fun HttpClient.getXml(url: String): MavenMetadata {
        return xml.decodeFromString(getText(url))
    }

    private fun HttpClient.getText(url: String): String {
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .header("Cache-Control", "no-cache, max-age=0")
            .build()

        val response = send(request, BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw Exception("Failed to download file: $url")
        }

        return response.body()
    }

    private fun checkHash(client: HttpClient, url: String, path: Path) {
        val expectedHash = client.getText("$url.sha256").lowercase()

        val realHash = path.hashFile(HashingAlgorithm.SHA256).asHexString().lowercase()
        if (expectedHash != realHash) {
            throw Exception("Hash does not match for downloaded file from: $url, file: $path")
        }
    }
}
