rootProject.name = "build-logic"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include("lib")
project(":lib").name = "build-logic-lib"

include("codebook-runner")
project(":codebook-runner").name = "build-logic-codebook-runner"
