package com.aiyifan.app.core.data.remote

import com.aiyifan.app.core.data.FakeCatalogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCatalogRepositoryHomeRefreshTest {

    @Test
    fun `refresh home replaces cached sections with fresh response`() = runBlocking {
        val fetcher = SequencedHomeFetcher(
            homeResponses = listOf(homeResponse("Old title"), homeResponse("New title")),
        )
        val repository = repository(fetcher)

        assertEquals("Old title", repository.getHomeVideos("Recommended").single().title)

        repository.refreshHome()

        assertEquals("New title", repository.getHomeVideos("Recommended").single().title)
        assertEquals(2, fetcher.homeRequestCount)
    }

    @Test
    fun `failed refresh keeps previously cached sections`() = runBlocking {
        val fetcher = SequencedHomeFetcher(
            homeResponses = listOf(homeResponse("Cached title"), HttpResponse(500, "unavailable")),
        )
        val repository = repository(fetcher)

        assertEquals("Cached title", repository.getHomeVideos("Recommended").single().title)

        assertTrue(runCatching { repository.refreshHome() }.isFailure)

        assertEquals("Cached title", repository.getHomeVideos("Recommended").single().title)
        assertEquals(2, fetcher.homeRequestCount)
    }

    @Test
    fun `empty refresh response keeps previously cached sections`() = runBlocking {
        val fetcher = SequencedHomeFetcher(
            homeResponses = listOf(homeResponse("Cached title"), emptyHomeResponse()),
        )
        val repository = repository(fetcher)

        assertEquals("Cached title", repository.getHomeVideos("Recommended").single().title)

        assertTrue(runCatching { repository.refreshHome() }.isFailure)

        assertEquals("Cached title", repository.getHomeVideos("Recommended").single().title)
        assertEquals(2, fetcher.homeRequestCount)
    }

    private fun repository(fetcher: HttpFetcher) = RemoteCatalogRepository(
        fallback = FakeCatalogRepository(),
        configResolver = RemoteConfigResolver(fetcher),
        fetcher = fetcher,
    )

    private fun homeResponse(title: String) = HttpResponse(
        200,
        """
            {
              "data": {
                "list": [
                  {
                    "name": "Recommended",
                    "list": [
                      {
                        "mediaKey": "media-key",
                        "title": "$title",
                        "coverImgUrl": "https://images.example.com/$title.jpg",
                        "videoType": 1
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent(),
    )

    private fun emptyHomeResponse() = HttpResponse(
        200,
        """{"data":{"list":[]}}""",
    )

    private class SequencedHomeFetcher(
        private val homeResponses: List<HttpResponse>,
    ) : HttpFetcher {
        var homeRequestCount = 0
            private set

        override suspend fun get(url: String): HttpResponse = when {
            url == "https://dataexbbff.github.io/rawApp.json" ->
                HttpResponse(200, """{"api":"https://catalog.example"}""")
            url == "https://catalog.example/api/List/Index" -> {
                val response = homeResponses.getOrElse(homeRequestCount) { HttpResponse(500, "unavailable") }
                homeRequestCount += 1
                response
            }
            else -> HttpResponse(404, "missing")
        }
    }
}
