plugins {
    kotlin("jvm")
    alias(libs.plugins.spotless)
}

repositories {
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/") {
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
