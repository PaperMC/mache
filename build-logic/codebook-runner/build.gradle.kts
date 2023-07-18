plugins {
    kotlin("jvm")
    alias(libs.plugins.spotless)
}

repositories {
    maven("https://repo.denwav.dev/repository/maven-public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "Sonatype"
        mavenContent {
            snapshotsOnly()
            includeGroupAndSubgroups("dev.denwav.hypo")
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.codebook)
}
