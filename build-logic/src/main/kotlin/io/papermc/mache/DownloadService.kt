package io.papermc.mache

import java.io.InputStream
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.setLastModifiedTime
import kotlin.io.path.writeText
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class DownloadService : BuildService<BuildServiceParameters.None> {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMinutes(5))
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .cookieHandler(CookieManager())
        .build()

    fun download(source: Any, target: Any, hash: Hash? = null, downloadCallback: () -> Unit = {}) {
        download(source.convertToUri(), target.convertToPath(), hash, false, downloadCallback)
    }

    suspend fun downloadAsync(source: Any, target: Any, hash: Hash? = null, downloadCallback: () -> Unit = {}) = coroutineScope {
        async {
            download(source.convertToUri(), target.convertToPath(), hash, false, downloadCallback)
        }
    }

    private fun download(source: URI, target: Path, hash: Hash?, retry: Boolean, downloadCallback: () -> Unit) {
        download0(source, target, downloadCallback)
        if (hash == null) {
            return
        }
        val dlHash = target.hashFile(hash.algorithm).asHexString().lowercase(Locale.ENGLISH)
        if (dlHash == hash.valueLower) {
            return
        }
        LOGGER.warn(
            "{} hash of downloaded file '{}' does not match what was expected! (expected: '{}', got: '{}')",
            hash.algorithm.name,
            target,
            hash.valueLower,
            dlHash,
        )
        if (retry) {
            throw Exception("Failed to download file '$target' from '$source'.")
        }
        LOGGER.warn("Re-attempting download once before giving up.")
        target.deleteIfExists()
        download(source, target, hash, true, downloadCallback)
    }

    private fun download0(source: URI, target: Path, downloadCallback: () -> Unit) {
        target.parent.createDirectories()

        val etagFile = target.resolveSibling(target.name + ".etag")
        val etag = if (etagFile.exists()) etagFile.readText() else null

        val request = HttpRequest.newBuilder()
            .timeout(Duration.ofMinutes(5))
            .GET()
            .uri(source)

        val time = if (target.exists()) target.getLastModifiedTime().toInstant() else Instant.EPOCH

        if (target.exists()) {
            if (time != Instant.EPOCH) {
                val value = DateTimeFormatter.RFC_1123_DATE_TIME.format(time.atZone(ZoneOffset.UTC))
                request.setHeader("If-Modified-Since", value)
            }
            if (etag != null) {
                request.setHeader("If-None-Match", etag)
            }
        }

        val response = httpClient.send(request.build(), BodyHandlers.ofInputStream())
        val code = response.statusCode()

        if (code !in 200..299 && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
            throw Exception("Download failed, HTTP code: $code; URL: $source")
        }

        val lastModified = handleResponse(response, target, downloadCallback)
        saveEtag(response, lastModified, target, etagFile)
    }

    private fun handleResponse(response: HttpResponse<InputStream>, target: Path, downloadCallback: () -> Unit): Instant {
        val lastModified = response.headers().firstValue("Last-Modified").orElse(null)?.let {
            for (formatter in FORMATTERS) {
                try {
                    return@let Instant.from(formatter.parse(it))
                } catch (ignored: DateTimeParseException) {}
            }
            null
        } ?: Instant.now()

        if (response.statusCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            return lastModified
        }

        downloadCallback()

        FileChannel.open(target).use { output ->
            Channels.newChannel(response.body()).use { input ->
                output.transferFrom(input, 0, Long.MAX_VALUE)
            }
        }

        return lastModified
    }

    private fun saveEtag(response: HttpResponse<*>, lastModified: Instant, target: Path, etagFile: Path) {
        if (lastModified != Instant.EPOCH) {
            target.setLastModifiedTime(FileTime.from(lastModified))
        }

        val etag = response.headers().firstValue("ETag").orElse(null) ?: return

        etagFile.writeText(etag)
    }

    companion object {
        private val LOGGER: Logger = Logging.getLogger(DownloadService::class.java)

        private const val PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz"
        private val FORMATTER_RFC1036 = DateTimeFormatterBuilder()
            .parseLenient()
            .parseCaseInsensitive()
            .appendPattern(PATTERN_RFC1036)
            .toFormatter(Locale.ENGLISH)

        private const val PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy"
        private val FORMATTER_ASCTIME = DateTimeFormatterBuilder()
            .parseLenient()
            .parseCaseInsensitive()
            .appendPattern(PATTERN_ASCTIME)
            .toFormatter(Locale.ENGLISH)

        private val FORMATTERS = listOf(DateTimeFormatter.RFC_1123_DATE_TIME, FORMATTER_RFC1036, FORMATTER_ASCTIME)
    }
}
