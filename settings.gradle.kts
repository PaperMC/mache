pluginManagement {
    repositories {
        mavenCentral() {
            mavenContent { releasesOnly() }
        }
        maven("https://repo.papermc.io/repository/maven-releases/") {
            mavenContent { releasesOnly() }
        }
        maven("https://maven.neoforged.net/releases") {
            name = "Neoforged"
            mavenContent {
                releasesOnly()
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
