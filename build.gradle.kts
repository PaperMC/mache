import io.papermc.mache.constants.MC_MANIFEST
import io.papermc.mache.download

plugins {
    id("mache-lib")
}

val mcManifestUrl = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
val mcManifestFile: RegularFile = layout.dotGradleDirectory.file(MC_MANIFEST)
download.download(mcManifestUrl, mcManifestFile)
