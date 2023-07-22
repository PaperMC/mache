package io.papermc.mache.tasks

import io.papermc.mache.lib.data.meta.MacheAdditionalDependencies
import io.papermc.mache.lib.data.meta.MacheDependencies
import io.papermc.mache.lib.data.meta.MacheMeta
import io.papermc.mache.lib.data.meta.MacheRepository
import io.papermc.mache.lib.json
import io.papermc.mache.util.GradleMavenArtifact
import io.papermc.mache.util.MacheRepo
import io.papermc.mache.util.convertToPath
import io.papermc.mache.util.ensureClean
import javax.inject.Inject
import kotlin.io.path.writeText
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateMacheMetadata : DefaultTask() {

    @get:Input
    abstract val version: Property<String>

    @get:Nested
    abstract val repos: NamedDomainObjectContainer<MacheRepo>

    @get:Nested
    abstract val codebookDeps: ListProperty<GradleMavenArtifact>

    @get:Nested
    abstract val paramMappingsDeps: ListProperty<GradleMavenArtifact>

    @get:Nested
    abstract val constantsDeps: ListProperty<GradleMavenArtifact>

    @get:Nested
    abstract val remapperDeps: ListProperty<GradleMavenArtifact>

    @get:Nested
    abstract val decompilerDeps: ListProperty<GradleMavenArtifact>

    @get:Nested
    abstract val compileOnlyDeps: ListProperty<GradleMavenArtifact>

    @get:Nested
    abstract val implementationDeps: ListProperty<GradleMavenArtifact>

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
        val codebook = codebookDeps.get().map { it.toMavenArtifact() }
        val paramMappings = paramMappingsDeps.get().map { it.toMavenArtifact() }
        val constants = constantsDeps.get().map { it.toMavenArtifact() }
        val remapper = remapperDeps.get().map { it.toMavenArtifact() }
        val decompiler = decompilerDeps.get().map { it.toMavenArtifact() }

        val meta = MacheMeta(
            version = version.get(),
            dependencies = MacheDependencies(codebook, paramMappings, constants, remapper, decompiler),
            repositories = repos.map { r ->
                MacheRepository(r.url.get(), r.name, r.includeGroups.get().takeIf { it.isNotEmpty() })
            },
            decompilerArgs = decompilerArgs.get(),
            additionalCompileDependencies = MacheAdditionalDependencies(
                compileOnly = compileOnlyDeps.get().map { it.toMavenArtifact() }.takeIf { it.isNotEmpty() },
                implementation = implementationDeps.get().map { it.toMavenArtifact() }.takeIf { it.isNotEmpty() },
            ).takeIf { it.compileOnly != null || it.implementation != null },
        )
        val metaJson = json.encodeToString(meta)

        outputFile.convertToPath().ensureClean().writeText(metaJson)
    }
}
