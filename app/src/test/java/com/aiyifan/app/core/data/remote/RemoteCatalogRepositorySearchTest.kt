package com.aiyifan.app.core.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLEncoder

class RemoteCatalogRepositorySearchTest {

    @Test
    fun `search falls back to home catalog when remote search returns business error`() = runBlocking {
        val repository = repository(
            searchResponse = """{"ret":202,"data":null}""",
            homeResponse = """
                {
                  "ret": 200,
                  "data": {
                    "list": [
                      {
                        "name": "首页",
                        "list": [
                          {
                            "mediaKey": "catalog-movie",
                            "title": "目录影片",
                            "coverImgUrl": "https://example.com/catalog.jpg",
                            "videoType": 1,
                            "mediaType": "电影",
                            "contentType": "剧情"
                          }
                        ]
                      }
                    ]
                  }
                }
            """.trimIndent(),
        )

        val results = repository.searchVideos("电影")

        assertEquals(listOf("catalog-movie"), results.map { it.mediaKey })
    }

    @Test
    fun `search prefers remote results when remote search succeeds`() = runBlocking {
        val repository = repository(
            searchResponse = """
                {
                  "ret": 200,
                  "data": [
                    {
                      "mediaKey": "remote-movie",
                      "title": "远程命中",
                      "coverImgUrl": "https://example.com/remote.jpg",
                      "videoType": 1,
                      "mediaType": "电影"
                    }
                  ]
                }
            """.trimIndent(),
        )

        val results = repository.searchVideos("远程")

        assertEquals(listOf("remote-movie"), results.map { it.mediaKey })
    }

    @Test
    fun `search falls back to mock catalog when home catalog is unavailable`() = runBlocking {
        val repository = repository(searchResponse = """{"ret":202,"data":null}""")

        val results = repository.searchVideos("迷城")

        assertEquals(listOf("movie-mc"), results.map { it.mediaKey })
    }

    private fun repository(
        searchResponse: String,
        homeResponse: String? = null,
    ): RemoteCatalogRepository {
        val responses = buildMap {
            put(CONFIG_URL, HttpResponse(200, """{"api":"$BASE_URL"}"""))
            put(searchUrl("电影"), HttpResponse(200, searchResponse))
            put(searchUrl("远程"), HttpResponse(200, searchResponse))
            put(searchUrl("迷城"), HttpResponse(200, searchResponse))
            homeResponse?.let { put(HOME_URL, HttpResponse(200, it)) }
        }
        val fetcher = FakeHttpFetcher(responses)
        return RemoteCatalogRepository(
            configResolver = RemoteConfigResolver(fetcher),
            fetcher = fetcher,
        )
    }

    private fun searchUrl(keyword: String): String =
        "$BASE_URL/api/Search/GetSearch?keyword=${URLEncoder.encode(keyword, Charsets.UTF_8.name())}&region=cn"

    private class FakeHttpFetcher(
        private val responses: Map<String, HttpResponse>,
    ) : HttpFetcher {
        override suspend fun get(url: String): HttpResponse =
            responses[url] ?: HttpResponse(404, "")
    }

    private companion object {
        const val CONFIG_URL = "https://dataexbbff.github.io/rawApp.json"
        const val BASE_URL = "https://api.tripdata.app"
        const val HOME_URL = "https://api.tripdata.app/api/List/Index"
    }
}
