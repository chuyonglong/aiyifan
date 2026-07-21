package com.aiyifan.app.core.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
        val country = fetchCountryCode()
        val candidateUrls = buildList {
            if (!country.isNullOrBlank()) {
                add("https://dataexbbff.github.io/rawApp${country}.json")
            }
            add(DEFAULT_CONFIG_URL)
        }
        for (url in candidateUrls) {
            val api = fetchConfigApi(url)
            if (!api.isNullOrBlank()) {
                return ensureTrailingSlash(api)
            }
        }
        return DEFAULT_BASE_URL
    }

    private suspend fun fetchCountryCode(): String? {
        val response = fetcher.get(COUNTRY_API_URL)
        if (response.code !in 200..299) return null
        return runCatching {
            JSONObject(response.body).optString("country").trim().uppercase()
        }.getOrNull().takeUnless { it.isNullOrBlank() }
    }

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
        private const val COUNTRY_API_URL = "https://api.country.is/"
        private const val DEFAULT_CONFIG_URL = "https://dataexbbff.github.io/rawApp.json"
        private const val DEFAULT_BASE_URL = "https://api.tripdata.app/"
    }
}
