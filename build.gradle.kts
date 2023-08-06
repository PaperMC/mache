import io.papermc.mache.constants.MC_MANIFEST
import io.papermc.mache.constants.REPO_URL
import io.papermc.mache.tasks.CopyVersion
import io.papermc.mache.tasks.MigrateVersion
import io.papermc.mache.tasks.OpenVersion
import io.papermc.mache.util.download

plugins {
    id("mache-lib")
}

val mcManifestUrl = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
val mcManifestFile: RegularFile = layout.dotGradleDirectory.file(MC_MANIFEST)
download.download(mcManifestUrl, mcManifestFile)

tasks.register("openVersion", OpenVersion::class) {
    repoUrl.set(REPO_URL)
}

tasks.register("migrate", MigrateVersion::class)
tasks.register("copy", CopyVersion::class)
