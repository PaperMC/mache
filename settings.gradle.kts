rootProject.name = "mache"

pluginManagement {
    repositories {
        maven("https://repo.papermc.io/repository/maven-snapshots/");
    }
}

file("versions").listFiles()
    ?.forEach { version ->
        if (version.resolve("build.gradle.kts").exists()) {
            include(":versions:${version.name}")
        }
    }
