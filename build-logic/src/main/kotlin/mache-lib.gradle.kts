import io.papermc.mache.DownloadService
import io.papermc.mache.constants.MACHE_DIR

gradle.sharedServices.registerIfAbsent("download", DownloadService::class) {}

tasks.register("cleanMacheCache", Delete::class) {
    group = "mache"
    delete(layout.dotGradleDirectory.dir(MACHE_DIR))
}
