import io.papermc.mache.ConfigureVersionProject
import io.papermc.mache.constants.FULL_DECOMP_JAR
import io.papermc.mache.constants.PATCHED_JAR
import io.papermc.mache.tasks.ApplyPatches
import io.papermc.mache.tasks.RebuildPatches
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
    id("mache-lib")
}

val mache = extensions.create("mache", MacheExtension::class)

repositories {
    maven("https://libraries.minecraft.net/") {
        name = "Minecraft"
    }
    maven("https://repo.denwav.dev/repository/maven-public/")
    mavenCentral()
    maven("https://maven.fabricmc.net/")
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

val applyPatches by tasks.registering(ApplyPatches::class) {
    val patchesDir = layout.projectDirectory.dir("patches")
    if (patchesDir.asFile.exists()) {
        patchDir.set(patchesDir)
    }

    inputFile.set(layout.buildDirectory.file(FULL_DECOMP_JAR))
    outputFile.set(layout.buildDirectory.file(PATCHED_JAR))
}

val copySources by tasks.registering(Sync::class) {
    dependsOn(applyPatches)
    into(layout.projectDirectory.dir("src/main/java"))
    from(zipTree(applyPatches.map { it.outputFile })) {
        include("**/*.java")
    }
    includeEmptyDirs = false
}

val copyResources by tasks.registering(Sync::class) {
    dependsOn(applyPatches)
    into(layout.projectDirectory.dir("src/main/resources"))
    from(zipTree(applyPatches.map { it.outputFile })) {
        exclude("**/*.java")
    }
    includeEmptyDirs = false
}

tasks.register("setup") {
    dependsOn(copySources, copyResources)
}

tasks.register("rebuildPatches", RebuildPatches::class) {
    decompJar.set(layout.buildDirectory.file(FULL_DECOMP_JAR))

    sourceDir.set(layout.projectDirectory.dir("src/main/java"))
    resourcesDir.set(layout.projectDirectory.dir("src/main/resources"))

    patchDir.set(layout.projectDirectory.dir("patches"))
}

afterEvaluate {
    ConfigureVersionProject.configure(project, mache)
}
