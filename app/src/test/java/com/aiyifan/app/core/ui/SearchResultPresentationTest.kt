package com.aiyifan.app.core.ui

import com.aiyifan.app.core.model.Episode
import com.aiyifan.app.core.model.VideoSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultPresentationTest {

    @Test
    fun `success hides message while empty and failure show messages`() {
        assertTrue(SearchPageState.Success.showResults)
        assertFalse(SearchPageState.Success.showMessage)
        assertTrue(SearchPageState.Empty.showMessage)
        assertTrue(SearchPageState.Failure.showRetry)
    }

    @Test
    fun `presentation groups metadata and limits episode previews`() {
        val result = SearchResultPresentation.from(
            VideoSummary(
                mediaKey = "demo",
                title = "示例剧集",
                coverUrl = "",
                videoType = 1,
                year = "2026",
                mediaType = "电视剧",
                contentType = "剧情",
                area = "中国大陆",
                updateStatus = "更新至12集",
                actor = "主演甲",
                director = "导演乙",
                episodePreviews = (1..8).map { Episode("ep-$it", "$it", it, null) },
            ),
        )

        assertEquals("2026 / 电视剧", result.primaryMeta)
        assertEquals("剧情 / 中国大陆 / 更新至12集", result.secondaryMeta)
        assertEquals("导演：导演乙\n主演：主演甲", result.credits)
        assertEquals(listOf("1", "2", "3", "4", "5", "6"), result.episodeLabels)
        assertTrue(result.showEpisodePreviews)
    }

    @Test
    fun `presentation uses content type as the primary type fallback`() {
        val result = SearchResultPresentation.from(
            VideoSummary(
                mediaKey = "fallback",
                title = "示例",
                coverUrl = "",
                videoType = 1,
                year = "2026",
                contentType = "电视剧",
                area = "中国大陆",
                updateStatus = "更新至12集",
            ),
        )

        assertEquals("2026 / 电视剧", result.primaryMeta)
        assertEquals("中国大陆 / 更新至12集", result.secondaryMeta)
    }

    @Test
    fun `presentation shows episode previews only when an episode is available`() {
        val result = SearchResultPresentation.from(
            VideoSummary(mediaKey = "demo", title = "示例", coverUrl = "", videoType = 1),
        )

        assertEquals("", result.primaryMeta)
        assertEquals("", result.secondaryMeta)
        assertEquals("", result.credits)
        assertTrue(result.episodeLabels.isEmpty())
        assertFalse(result.showEpisodePreviews)

        val oneEpisodeResult = SearchResultPresentation.from(
            VideoSummary(
                mediaKey = "demo",
                title = "example",
                coverUrl = "",
                videoType = 1,
                episodePreviews = listOf(Episode("ep-1", "1", 1, null)),
            ),
        )

        assertEquals(listOf("1"), oneEpisodeResult.episodeLabels)
        assertTrue(oneEpisodeResult.showEpisodePreviews)
    }

}
