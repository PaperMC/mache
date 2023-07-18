import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.coroutines)
    implementation(libs.serialize)

    implementation(project(":build-logic-lib"))
    implementation(project(":build-logic-codebook-runner"))
}

tasks.spotlessApply {
    subprojects {
        dependsOn(tasks.spotlessApply)
    }
}

subprojects {
    afterEvaluate {
        repositories {
            mavenCentral()
        }

        dependencies {
            implementation(kotlin("stdlib"))
        }
    }
}

allprojects {
    afterEvaluate {
        java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(17)
            }
        }

        kotlin {
            target {
                jvmToolchain(java.toolchain.languageVersion.get().asInt())
            }
        }

        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions {
                languageVersion = "1.8"
                jvmTarget = java.toolchain.languageVersion.get().asInt().toString()
            }
        }

        spotless {
            kotlin {
                ktlint(libs.versions.ktlint.get())
                    .editorConfigOverride(mapOf(
                        "indent_size" to "4",
                        "indent_style" to "space",
                        "max_line_length" to "150",
                        "ktlint_code_style" to "ktlint_official",
                        "ktlint_standard_filename" to "disabled",
                        "ij_kotlin_imports_layout" to "*,|,^"
                    ))
            }
        }
    }
}
