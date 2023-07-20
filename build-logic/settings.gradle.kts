rootProject.name = "build-logic"

include("lib")
project(":lib").name = "build-logic-lib"

include("codebook-runner")
project(":codebook-runner").name = "build-logic-codebook-runner"
