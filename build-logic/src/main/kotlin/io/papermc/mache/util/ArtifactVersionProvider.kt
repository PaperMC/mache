package io.papermc.mache.util

import io.papermc.mache.lib.data.maven.MavenMetadata
import java.net.http.HttpClient
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

abstract class ArtifactVersionProvider : ValueSource<String, ArtifactVersionProvider.BuildIdParameters> {
    interface BuildIdParameters : ValueSourceParameters {
        val repoUrl: Property<String>
        val mcVersion: Property<String>
        val ci: Property<String>
    }

    override fun obtain(): String {
        return parameters.mcVersion.get() + "+build." + buildVersion()
    }

    private fun buildVersion(): String {
        if (parameters.ci.get() != "true") {
            return "local-SNAPSHOT"
        }

        val client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build()

        val metaUrl = parameters.repoUrl.get().removeSuffix("/") + "/io/papermc/mache/maven-metadata.xml"
        val meta = try {
            client.getXml<MavenMetadata>(metaUrl)
        } catch (ignored: NotFoundException) {
            return 1.toString()
        }

        val version = parameters.mcVersion.get()

        val buildIdentifier = "+build."
        val currentMaxVersion = meta.versioning.versions.asSequence()
            .filter { it.contains(buildIdentifier) }
            .filter { it.substringBefore(buildIdentifier) == version }
            .mapNotNull { it.substringAfter(buildIdentifier).toIntOrNull() }
            .maxOrNull() ?: 0

        return (currentMaxVersion + 1).toString()
    }
}
