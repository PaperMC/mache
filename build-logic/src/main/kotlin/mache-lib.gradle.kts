import io.papermc.mache.constants.MACHE_DIR
import io.papermc.mache.util.DownloadService

gradle.sharedServices.registerIfAbsent("download", DownloadService::class) {}

tasks.register("cleanMacheCache", Delete::class) {
    group = "mache"
    description = "Delete downloaded manifest and jar files from Mojang."
    delete(layout.dotGradleDirectory.dir(MACHE_DIR))
}
