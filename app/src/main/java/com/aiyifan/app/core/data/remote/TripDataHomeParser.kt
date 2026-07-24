package com.aiyifan.app.core.data.remote

import com.aiyifan.app.core.model.VideoSummary
import org.json.JSONArray
import org.json.JSONObject

data class TripDataHomeSection(
    val name: String,
    val videos: List<VideoSummary>,
)

object TripDataHomeParser {
    fun parse(payload: String): List<TripDataHomeSection> {
        val sections = JSONObject(payload)
            .optJSONObject("data")
            ?.optJSONArray("list")
            ?: JSONArray()
        return buildList {
            for (index in 0 until sections.length()) {
                val section = sections.optJSONObject(index) ?: continue
                val name = section.optString("name").trim()
                val videos = parseVideos(section.optJSONArray("list"))
                if (name.isNotBlank() && videos.isNotEmpty()) {
                    add(TripDataHomeSection(name = name, videos = videos))
                }
            }
        }
    }

    private fun parseVideos(items: JSONArray?): List<VideoSummary> =
        buildList {
            if (items == null) return@buildList
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val mediaKey = item.optString("mediaKey").trim()
                if (mediaKey.isBlank()) continue
                val videoType = item.optInt("videoType", item.optInt("type", 0))
                if (videoType == 3) continue
                val publishTime = item.optString("publishTime").trim()
                add(
                    VideoSummary(
                        mediaKey = mediaKey,
                        episodeKey = item.optString("episodeKey").trim().ifBlank { null },
                        title = item.optString("title").trim(),
                        coverUrl = item.optString("coverImgUrl").trim(),
                        videoType = videoType,
                        contentType = item.optString("contentType").trim().ifBlank { null },
                        mediaType = item.optString("mediaType").trim().ifBlank { null },
                        score = item.optString("score").trim().ifBlank { null },
                        playCount = item.optInt("playCount"),
                        updateStatus = item.optString("updateStatus").trim().ifBlank { null },
                        duration = item.optString("duration").trim().ifBlank { null },
                        year = publishTime.takeIf { it.length >= 4 }?.substring(0, 4),
                        area = item.optString("regional").trim().ifBlank { null },
                        actor = item.optString("actor").trim().ifBlank { null },
                        director = item.optString("director").trim().ifBlank { null },
                    ),
                )
            }
        }
}
