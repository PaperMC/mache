plugins {
    kotlin("jvm")
    alias(libs.plugins.spotless)
}

repositories {
    maven("https://repo.denwav.dev/repository/maven-public/")
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.codebook)
}
