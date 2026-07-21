package com.aiyifan.app.core.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val DEFAULT_REGION = "cn"

data class HttpResponse(
    val code: Int,
    val body: String,
)

interface HttpFetcher {
    suspend fun get(url: String): HttpResponse
}

class UrlConnectionHttpFetcher : HttpFetcher {
    override suspend fun get(url: String): HttpResponse =
        withContext(Dispatchers.IO) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Aiyifan/1.0")
                setRequestProperty("Accept", "application/json,text/plain,*/*")
                setRequestProperty("Accept-Language", "zh-CN")
                setRequestProperty("X-Region", DEFAULT_REGION)
            }
            try {
                val stream = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val body = stream?.use { input ->
                    BufferedReader(InputStreamReader(input)).readText()
                }.orEmpty()
                HttpResponse(connection.responseCode, body)
            } finally {
                connection.disconnect()
            }
        }
}

class RemoteConfigResolver(
    private val fetcher: HttpFetcher,
) {
    suspend fun resolveBaseUrl(): String {
        // V1 serves the mainland China catalog by default. The public config currently
        // has no region-specific cn file, so the default config is the cn source.
        for (url in listOf(configUrlFor(DEFAULT_REGION))) {
            val api = fetchConfigApi(url)
            if (!api.isNullOrBlank()) {
                return ensureTrailingSlash(api)
            }
        }
        return DEFAULT_BASE_URL
    }

    private fun configUrlFor(region: String): String =
        if (region.equals(DEFAULT_REGION, ignoreCase = true)) DEFAULT_CONFIG_URL
        else "https://dataexbbff.github.io/rawApp${region.uppercase()}.json"

    private suspend fun fetchConfigApi(url: String): String? {
        val response = fetcher.get(url)
        if (response.code !in 200..299) return null
        return runCatching {
            JSONObject(response.body).optString("api").trim()
        }.getOrNull().takeUnless { it.isNullOrBlank() }
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    companion object {
        private const val DEFAULT_CONFIG_URL = "https://dataexbbff.github.io/rawApp.json"
        private const val DEFAULT_BASE_URL = "https://api.tripdata.app/"
    }
}
