plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.8.21"
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(libs.serialize.core)
    implementation(libs.serialize.json)

    implementation(libs.xml.core)
    implementation(libs.xml.serialize) {
        isTransitive = false
    }
}
