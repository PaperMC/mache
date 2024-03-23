pluginManagement {
    repositories {
        maven("https://repo.papermc.io/repository/maven-snapshots/")
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "mache"

file("versions").listFiles()
    ?.forEach { version ->
        if (version.resolve("build.gradle.kts").exists()) {
            include(":versions:${version.name}")
        }
    }
