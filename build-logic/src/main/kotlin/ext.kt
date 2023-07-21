import io.papermc.mache.constants.GRADLE_DIR
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.codebook(version: String) {
    "codebook"("io.papermc.codebook:codebook:$version")
}

fun DependencyHandlerScope.yarn(version: String) {
    "paramMappings"("net.fabricmc:yarn:$version") {
        artifact {
            classifier = "mergedv2"
        }
    }
    "constants"("net.fabricmc:yarn:$version") {
        artifact {
            classifier = "constants"
        }
    }
}

fun tiny(version: String): String = "net.fabricmc:tiny-remapper:$version"
fun vineflower(version: String): String = "org.vineflower:vineflower:$version"

val ProjectLayout.dotGradleDirectory: Directory
    get() = projectDirectory.dir(GRADLE_DIR)
