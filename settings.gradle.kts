pluginManagement {
    repositories {
        maven("https://repo.papermc.io/repository/maven-releases/")
        mavenCentral()
        maven("https://maven.neoforged.net/releases") {
            name = "Neoforged"
            mavenContent {
                includeGroupAndSubgroups("codechicken")
                includeGroupAndSubgroups("net.covers1624")
            }
        }
    }
}

rootProject.name = "mache"

file("versions").listFiles()
    ?.forEach { version ->
        if (version.resolve("build.gradle.kts").exists()) {
            include(":versions:${version.name}")
        }
    }
