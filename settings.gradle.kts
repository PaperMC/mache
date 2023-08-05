pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "mache"

file("versions").listFiles()
    ?.forEach { version ->
        if (version.resolve("build.gradle.kts").exists()) {
            include(":versions:${version.name}")
        }
    }
