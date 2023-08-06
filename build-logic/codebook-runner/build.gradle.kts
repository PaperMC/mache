plugins {
    kotlin("jvm")
    alias(libs.plugins.spotless)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-releases/") {
        name = "PaperMC"
        mavenContent {
            includeGroupAndSubgroups("io.papermc")
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.codebook)
}
