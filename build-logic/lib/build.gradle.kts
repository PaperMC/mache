import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.8.21"
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(libs.serialize)
}
