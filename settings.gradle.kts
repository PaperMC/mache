pluginManagement {
    repositories {
        maven("https://maven.neoforged.net/releases/") {
            name = "NeoForged"
        }
    }
    includeBuild("build-logic")
}

rootProject.name = "mache"

file("versions").listFiles()
    ?.forEach { version ->
        include(":versions:${version.name}")
    }
