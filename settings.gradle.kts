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

fun formatVersion(version: String): String {
    val replaced = version.replace(".", "_")
    return if (version.first().isLetter()) replaced else "v$replaced"
}

file("versions").listFiles()
    ?.forEach { version ->
        if (version.resolve("build.gradle.kts").exists()) {
            val projectPath = ":versions:${formatVersion(version.name)}"
            include(projectPath)
            findProject(projectPath)!!.projectDir = version
        }
    }
