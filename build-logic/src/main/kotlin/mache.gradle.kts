import io.papermc.mache.ConfigureVersionProject
import io.papermc.mache.constants.DECOMP_JAR
import io.papermc.mache.constants.DOWNLOAD_SERVER_JAR
import io.papermc.mache.constants.PATCHED_JAR
import io.papermc.mache.constants.REMAPPED_JAR
import io.papermc.mache.constants.SERVER_JAR
import io.papermc.mache.constants.SERVER_MAPPINGS
import io.papermc.mache.tasks.ApplyPatches
import io.papermc.mache.tasks.ApplyPatchesFuzzy
import io.papermc.mache.tasks.DecompileJar
import io.papermc.mache.tasks.ExtractServerJar
import io.papermc.mache.tasks.RebuildPatches
import io.papermc.mache.tasks.RemapJar
import io.papermc.mache.tasks.SetupSources
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
    id("mache-lib")
}

val mache = extensions.create("mache", MacheExtension::class)

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC"
        mavenContent {
            includeGroupAndSubgroups("io.papermc")
        }
    }
    maven("https://maven.fabricmc.net/") {
        name = "FabricMC"
        mavenContent {
            includeGroupAndSubgroups("net.fabricmc")
        }
    }
    maven("https://repo.denwav.dev/repository/maven-public/") {
        name = "DenWav"
        mavenContent {
            includeGroupAndSubgroups("org.vineflower")
        }
    }
    maven("https://libraries.minecraft.net/") {
        name = "Minecraft"
    }
    mavenCentral()
}

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

    inputFile.set(decompileJar.flatMap { it.outputJar })
    outputJar.set(layout.buildDirectory.file(PATCHED_JAR))
}

val setupSources by tasks.registering(SetupSources::class) {
    decompJar.set(decompileJar.flatMap { it.outputJar })
    // Don't use the output of applyPatches directly with a flatMap
    // That would tell Gradle that this task dependsOn applyPatches, so it
    // would no longer work as a finalizer task if applyPatches fails
    patchedJar.set(layout.buildDirectory.file(PATCHED_JAR))

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

afterEvaluate {
    ConfigureVersionProject.configure(project, mache)
}
