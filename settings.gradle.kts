pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "mache"

file("versions").listFiles()
    ?.forEach { version ->
        include(":versions:${version.name}")
    }
