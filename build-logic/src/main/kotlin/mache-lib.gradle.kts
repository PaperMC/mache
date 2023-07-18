import io.papermc.mache.DownloadService

gradle.sharedServices.registerIfAbsent("download", DownloadService::class) {}
