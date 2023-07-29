import io.papermc.mache.constants.GRADLE_DIR
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.codebook(version: String) {
    "codebook"("io.papermc.codebook:codebook:$version")
}

fun DependencyHandlerScope.parchment(mcVersion: String, version: String) {
    "paramMappings"("org.parchmentmc.data:parchment-$mcVersion:$version") {
        artifact {
            extension = "zip"
        }
    }
}

fun art(version: String): String = "net.neoforged:AutoRenamingTool:$version"
fun vineflower(version: String): String = "org.vineflower:vineflower:$version"

val ProjectLayout.dotGradleDirectory: Directory
    get() = projectDirectory.dir(GRADLE_DIR)
