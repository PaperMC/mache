import io.papermc.mache.ConfigureVersionProject
import io.papermc.mache.constants.DECOMP_JAR
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
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "Sonatype"
        mavenContent {
            snapshotsOnly()
            includeGroupAndSubgroups("dev.denwav.hypo")
        }
    }
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

val setupSources by tasks.registering(Sync::class) {
    into(layout.projectDirectory.dir("src/main/java"))
    include("**/*.java")
    from(zipTree(layout.buildDirectory.file(DECOMP_JAR)))
}
val setupResources by tasks.registering(Sync::class) {
    into(layout.projectDirectory.dir("src/main/resources"))
    include("**/*.java")
    from(zipTree(layout.buildDirectory.file(DECOMP_JAR)))
}
tasks.register("setup") {
    dependsOn(setupSources, setupResources)
}

afterEvaluate {
    ConfigureVersionProject.configure(project, mache)
}
