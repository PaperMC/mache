package io.papermc.mache.tasks

import io.papermc.mache.MacheRepo
import io.papermc.mache.convertToPath
import io.papermc.mache.ensureClean
import io.papermc.mache.lib.data.meta.MacheDependencies
import io.papermc.mache.lib.data.meta.MacheMeta
import io.papermc.mache.lib.data.meta.MacheRepository
import io.papermc.mache.lib.data.meta.MavenArtifact
import io.papermc.mache.lib.json
import javax.inject.Inject
import kotlin.io.path.writeText
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateMacheMetadata : DefaultTask() {

    @get:Nested
    abstract val repos: NamedDomainObjectContainer<MacheRepo>

    @get:Classpath
    abstract val codebookConfiguration: Property<Configuration>

    @get:Classpath
    abstract val paramMappingsConfiguration: Property<Configuration>

    @get:Classpath
    abstract val constantsConfiguration: Property<Configuration>

    @get:Classpath
    abstract val remapperConfiguration: Property<Configuration>

    @get:Classpath
    abstract val decompilerConfiguration: Property<Configuration>

    @get:Input
    abstract val decompilerArgs: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Inject
    abstract val layout: ProjectLayout

    init {
        run {
            outputFile.convention(layout.buildDirectory.dir(name).map { it.file("$name.json") })
        }
    }

    @TaskAction
    fun run() {
        val codebook = codebookConfiguration.get().asMavenArtifacts()
        val paramMappings = paramMappingsConfiguration.get().asMavenArtifacts()
        val constants = constantsConfiguration.get().asMavenArtifacts()
        val remapper = remapperConfiguration.get().asMavenArtifacts()
        val decompiler = decompilerConfiguration.get().asMavenArtifacts()

        val meta = MacheMeta(
            dependencies = MacheDependencies(codebook, paramMappings, constants, remapper, decompiler),
            repositories = repos.map { r ->
                MacheRepository(r.url.get(), r.name, r.includeGroups.get().takeIf { it.isNotEmpty() })
            },
            decompilerArgs = decompilerArgs.get(),
        )
        val metaJson = json.encodeToString(meta)

        outputFile.convertToPath().ensureClean().writeText(metaJson)
    }

    private fun Configuration.asMavenArtifacts(): List<MavenArtifact> {
        return dependencies.map { dep ->
            val group = dep.group ?: throw IllegalStateException("Dependency without group: $dep")
            val version = dep.version ?: throw IllegalStateException("Dependency without version: $dep")
            val classifier = (dep as? ExternalDependency)?.artifacts?.firstNotNullOfOrNull { it.classifier }
            val extension = (dep as? ExternalDependency)?.artifacts?.firstNotNullOfOrNull { it.extension }?.takeIf { it != "jar" }
            MavenArtifact(group, dep.name, version, classifier, extension)
        }
    }
}
