import io.papermc.mache.ConfigureVersionProject
import io.papermc.mache.MacheExtension
import io.papermc.mache.constants.*
import io.papermc.mache.tasks.ApplyPatches
import io.papermc.mache.tasks.ApplyPatchesFuzzy
import io.papermc.mache.tasks.DecompileJar
import io.papermc.mache.tasks.ExtractServerJar
import io.papermc.mache.tasks.GenerateMacheMetadata
import io.papermc.mache.tasks.RebuildPatches
import io.papermc.mache.tasks.RemapJar
import io.papermc.mache.tasks.SetupSources
import io.papermc.mache.util.ArtifactVersionProvider
import io.papermc.mache.util.asGradleMavenArtifacts
import io.papermc.mache.util.isNativeDiffAvailable
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
    `maven-publish`
    id("mache-lib")
}

val mache = extensions.create("mache", MacheExtension::class)

val libs: LibrariesForLibs by extensions

val codebook by configurations.registering
val remapper by configurations.registering
val decompiler by configurations.registering
val paramMappings by configurations.registering
val constants by configurations.registering

val minecraft by configurations.registering
configurations.implementation {
    extendsFrom(minecraft.get())
    extendsFrom(constants.get())
}

val extractServerJar by tasks.registering(ExtractServerJar::class) {
    downloadedJar.set(layout.dotGradleDirectory.file(DOWNLOAD_SERVER_JAR))
    serverJar.set(layout.dotGradleDirectory.file(SERVER_JAR))
}

val remapJar by tasks.registering(RemapJar::class) {
    serverJar.set(extractServerJar.flatMap { it.serverJar })
    serverMappings.set(layout.dotGradleDirectory.file(SERVER_MAPPINGS))

    codebookClasspath.from(codebook)
    minecraftClasspath.from(minecraft)
    remapperClasspath.from(remapper)
    paramMappings.from(configurations.named("paramMappings"))
    constants.from(configurations.named("constants"))

    reportsDir.set(layout.dotGradleDirectory.dir(REPORTS_DIR))

    outputJar.set(layout.buildDirectory.file(REMAPPED_JAR))
}

val decompileJar by tasks.registering(DecompileJar::class) {
    inputJar.set(remapJar.flatMap { it.outputJar })
    decompilerArgs.set(mache.decompilerArgs)

    minecraftClasspath.from(minecraft)
    decompiler.from(configurations.named("decompiler"))

    outputJar.set(layout.buildDirectory.file(DECOMP_JAR))
}

val applyPatches by tasks.registering(ApplyPatches::class) {
    group = "mache"
    description = "Apply decompilation patches to the source."

    val patchesDir = layout.projectDirectory.dir("patches")
    if (patchesDir.asFile.exists()) {
        patchDir.set(patchesDir)
    }

    useNativeDiff.set(providers.gradleProperty("useNativeDiff").map { it.toBoolean() }.orElse(isNativeDiffAvailable()))
    providers.gradleProperty("patchExecutable").let { ex ->
        if (ex.isPresent) {
            patchExecutable.set(ex)
        }
    }

    inputFile.set(decompileJar.flatMap { it.outputJar })
    outputJar.set(layout.buildDirectory.file(PATCHED_JAR))
    failedPatchesJar.set(layout.buildDirectory.file(FAILED_PATCH_JAR))
}

val setupSources by tasks.registering(SetupSources::class) {
    decompJar.set(decompileJar.flatMap { it.outputJar })
    // Don't use the output of applyPatches directly with a flatMap
    // That would tell Gradle that this task dependsOn applyPatches, so it
    // would no longer work as a finalizer task if applyPatches fails
    patchedJar.set(layout.buildDirectory.file(PATCHED_JAR))
    failedPatchJar.set(layout.buildDirectory.file(FAILED_PATCH_JAR))

    sourceDir.set(layout.projectDirectory.dir("src/main/java"))
}

applyPatches.configure {
    finalizedBy(setupSources)
}

val applyPatchesFuzzy by tasks.registering(ApplyPatchesFuzzy::class) {
    finalizedBy(setupSources)

    group = "mache"
    description = "Attempt to apply patches with a fuzzy factor specified by --max-fuzz=<non-negative-int>. " +
        "This is not intended for normal use."

    patchDir.set(layout.projectDirectory.dir("patches"))

    inputFile.set(decompileJar.flatMap { it.outputJar })
    outputJar.set(layout.buildDirectory.file(PATCHED_JAR))
}

val copyResources by tasks.registering(Sync::class) {
    into(layout.projectDirectory.dir("src/main/resources"))
    from(zipTree(extractServerJar.flatMap { it.serverJar })) {
        exclude("**/*.class", "META-INF/**")
    }
    includeEmptyDirs = false
}

tasks.register("setup") {
    group = "mache"
    description = "Set up the full project included patched sources and resources."
    dependsOn(applyPatches, copyResources)
}

tasks.register("rebuildPatches", RebuildPatches::class) {
    group = "mache"
    description = "Rebuild decompilation patches from the current source set."
    decompJar.set(decompileJar.flatMap { it.outputJar })
    sourceDir.set(layout.projectDirectory.dir("src/main/java"))
    patchDir.set(layout.projectDirectory.dir("patches"))
}

tasks.register("runServer", JavaExec::class) {
    group = "mache"
    description = "Runs the minecraft server"
    doNotTrackState("Run server")

    val path = objects.fileCollection()
    path.from(sourceSets.main.map { it.output })
    path.from(configurations.runtimeClasspath)
    classpath = path

    mainClass = "net.minecraft.server.Main"

    args("--nogui")

    standardInput = System.`in`

    workingDir(layout.projectDirectory.dir("run"))
    doFirst {
        workingDir.mkdirs()
    }
}

val generateMacheMetadata by tasks.registering(GenerateMacheMetadata::class) {
    version.set(mache.minecraftVersion)
    repos.addAll(mache.repositories)

    decompilerArgs.set(mache.decompilerArgs)
}

afterEvaluate {
    generateMacheMetadata.configure {
        codebookDeps.set(asGradleMavenArtifacts(codebook.get()))
        paramMappingsDeps.set(asGradleMavenArtifacts(paramMappings.get()))
        constantsDeps.set(asGradleMavenArtifacts(constants.get()))
        remapperDeps.set(asGradleMavenArtifacts(remapper.get()))
        decompilerDeps.set(asGradleMavenArtifacts(decompiler.get()))

        compileOnlyDeps.set(asGradleMavenArtifacts(configurations.compileOnly.get()))
        implementationDeps.set(asGradleMavenArtifacts(configurations.implementation.get()))
    }
}

val artifactVersionProvider = providers.of(ArtifactVersionProvider::class) {
    parameters {
        repoUrl.set(REPO_URL)
        mcVersion.set(mache.minecraftVersion)
        ci.set(providers.environmentVariable("CI").orElse("false"))
    }
}

val createMacheArtifact by tasks.registering(Zip::class) {
    group = "mache"
    description = "Create the mache metadata artifact for publishing."

    from(generateMacheMetadata) {
        rename { "mache.json" }
    }
    into("patches") {
        from(layout.projectDirectory.dir("patches"))
    }

    archiveBaseName.set("mache")
    archiveVersion.set(artifactVersionProvider)
    archiveExtension.set("zip")
}

val archive = artifacts.archives(createMacheArtifact)

afterEvaluate {
    repositories {
        for (repository in mache.repositories) {
            maven(repository.url) {
                name = repository.name
                mavenContent {
                    for (group in repository.includeGroups.get()) {
                        includeGroupAndSubgroups(group)
                    }
                }
            }
        }

        maven("https://libraries.minecraft.net/") {
            name = "Minecraft"
        }
        mavenCentral()
    }

    ConfigureVersionProject.configure(project, mache)
}

publishing {
    afterEvaluate {
        publications {
            register<MavenPublication>("mache") {
                groupId = "io.papermc"
                artifactId = "mache"
                version = artifactVersionProvider.get()

                artifact(archive)
            }
        }
    }

    repositories {
        maven(REPO_URL) {
            name = "PaperMC"
            credentials(PasswordCredentials::class)
        }
    }
}
