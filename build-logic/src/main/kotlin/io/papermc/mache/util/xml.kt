package io.papermc.mache.util

import io.papermc.mache.lib.xml
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.decodeFromString

inline fun <reified T> HttpClient.getXml(url: String): T {
    return xml.decodeFromString(getText(url))
}

fun HttpClient.getText(url: String): String {
    val request = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create(url))
        .header("Cache-Control", "no-cache, max-age=0")
        .build()

    val response = send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) {
        if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw NotFoundException()
        }
        throw Exception("Failed to download file: $url")
    }

    return response.body()
}

class NotFoundException : Exception()
