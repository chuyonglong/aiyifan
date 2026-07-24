package com.aiyifan.app.core.ui

import com.aiyifan.app.core.model.Episode
import com.aiyifan.app.core.model.VideoSummary

enum class SearchPageState(
    val showResults: Boolean,
    val showMessage: Boolean,
    val showRetry: Boolean,
) {
    Loading(showResults = true, showMessage = true, showRetry = false),
    Success(showResults = true, showMessage = false, showRetry = false),
    Empty(showResults = true, showMessage = true, showRetry = false),
    Failure(showResults = true, showMessage = true, showRetry = true),
}

data class SearchResultPresentation(
    val primaryMeta: String,
    val secondaryMeta: String,
    val credits: String,
    val episodeLabels: List<String>,
    val showEpisodePreviews: Boolean,
) {
    companion object {
        fun from(video: VideoSummary): SearchResultPresentation =
            SearchResultPresentation(
                primaryMeta = listOfNotNull(video.year, video.mediaType).joinToString(" / "),
                secondaryMeta = listOfNotNull(video.contentType, video.area, video.updateStatus).joinToString(" / "),
                credits = listOfNotNull(
                    video.director?.takeIf(String::isNotBlank)?.let { "导演：$it" },
                    video.actor?.takeIf(String::isNotBlank)?.let { "主演：$it" },
                ).joinToString("\n"),
                episodeLabels = video.episodePreviews.take(6).map(Episode::episodeTitle),
                showEpisodePreviews = video.episodePreviews.isNotEmpty(),
            )
    }
}
