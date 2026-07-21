package com.aiyifan.app.core.data.remote

import com.aiyifan.app.core.data.CatalogRepository
import com.aiyifan.app.core.data.FakeCatalogRepository
import com.aiyifan.app.core.model.Category
import com.aiyifan.app.core.model.Comment
import com.aiyifan.app.core.model.Episode
import com.aiyifan.app.core.model.FavoriteVideo
import com.aiyifan.app.core.model.PlaybackLanguage
import com.aiyifan.app.core.model.PlaybackQuality
import com.aiyifan.app.core.model.VideoDetail
import com.aiyifan.app.core.model.VideoSummary
import com.aiyifan.app.core.model.WatchHistory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class RemoteCatalogRepository(
    private val fallback: FakeCatalogRepository = FakeCatalogRepository(),
    private val configResolver: RemoteConfigResolver = RemoteConfigResolver(UrlConnectionHttpFetcher()),
    private val fetcher: HttpFetcher = UrlConnectionHttpFetcher(),
) : CatalogRepository {
    private val cacheLock = Mutex()
    private val detailLock = Mutex()

    @Volatile
    private var cachedSections: List<TripDataHomeSection>? = null

    private val detailCache = linkedMapOf<String, VideoDetail>()

    override suspend fun getCategories(): List<Category> =
        try {
            ensureSections().mapIndexed { index, section ->
                Category(
                    id = section.name,
                    name = section.name,
                    type = index,
                    styleType = 0,
                )
            }
        } catch (_: Throwable) {
            fallback.getCategories()
        }

    override suspend fun getHomeVideos(categoryId: String): List<VideoSummary> =
        try {
            val sections = ensureSections()
            sections.firstOrNull { it.name == categoryId }?.videos
                ?: sections.firstOrNull()?.videos
                ?: emptyList()
        } catch (_: Throwable) {
            fallback.getHomeVideos(categoryId)
        }

    override suspend fun getHotVideos(): List<VideoSummary> =
        try {
            ensureSections()
                .flatMap { it.videos }
                .distinctBy { it.mediaKey }
                .sortedByDescending { it.playCount }
        } catch (_: Throwable) {
            fallback.getHotVideos()
        }

    override suspend fun searchVideos(keyword: String): List<VideoSummary> =
        try {
            val query = keyword.trim()
            if (query.isEmpty()) {
                emptyList()
            } else {
                ensureSections()
                    .flatMap { it.videos }
                    .distinctBy { it.mediaKey }
                    .filter { video ->
                        video.title.contains(query, ignoreCase = true) ||
                            video.actor.orEmpty().contains(query, ignoreCase = true) ||
                            video.director.orEmpty().contains(query, ignoreCase = true)
                    }
            }
        } catch (_: Throwable) {
            fallback.searchVideos(keyword)
        }

    override suspend fun getVideoDetail(mediaKey: String): VideoDetail {
        detailCache[mediaKey]?.let { return it }
        return detailLock.withLock {
            detailCache[mediaKey]?.let { return@withLock it }
            val detail = try {
                val baseUrl = configResolver.resolveBaseUrl()
                val response = fetcher.get(
                    buildUrl(
                        baseUrl,
                        "api/Video/VideoDetails",
                        mapOf("mediaKey" to mediaKey),
                    ),
                )
                if (response.code !in 200..299) {
                    throw IllegalStateException("VideoDetails failed: ${response.code}")
                }
                parseVideoDetail(response.body)
            } catch (_: Throwable) {
                fallback.getVideoDetail(mediaKey)
            }
            detailCache[mediaKey] = detail
            detail
        }
    }

    override suspend fun resolvePlayback(detail: VideoDetail, episode: Episode): Episode {
        if (!episode.mediaUrl.isNullOrBlank()) {
            return episode
        }
        return try {
            val baseUrl = configResolver.resolveBaseUrl()
            val response = fetcher.get(
                buildUrl(
                    baseUrl,
                    "api/Video/getPlayData",
                    linkedMapOf(
                        "mediaKey" to detail.mediaKey,
                        "videoId" to episode.uniqueId.toString(),
                        "resolution" to episode.resolution.orEmpty(),
                        "liveLine" to "",
                        "subtitlePrefer" to "",
                        "videoType" to detail.videoType.toString(),
                    ),
                ),
            )
            if (response.code !in 200..299) {
                throw IllegalStateException("getPlayData failed: ${response.code}")
            }
            parsePlayableEpisode(response.body, episode)
        } catch (_: Throwable) {
            episode
        }
    }

    override fun getComments(mediaKey: String): List<Comment> = fallback.getComments(mediaKey)

    override fun saveHistory(detail: VideoDetail, episode: Episode, progressMs: Long, durationMs: Long) {
        fallback.saveHistory(detail, episode, progressMs, durationMs)
    }

    override fun getHistory(): List<WatchHistory> = fallback.getHistory()

    override fun clearHistory() {
        fallback.clearHistory()
    }

    override fun toggleFavorite(detail: VideoDetail): Boolean = fallback.toggleFavorite(detail)

    override fun isFavorite(mediaKey: String): Boolean = fallback.isFavorite(mediaKey)

    override fun getFavorites(): List<FavoriteVideo> = fallback.getFavorites()

    private suspend fun ensureSections(): List<TripDataHomeSection> {
        cachedSections?.let { return it }
        return cacheLock.withLock {
            cachedSections?.let { return@withLock it }
            val baseUrl = configResolver.resolveBaseUrl()
            val response = fetcher.get("${baseUrl}api/List/Index")
            if (response.code !in 200..299) {
                throw IllegalStateException("Home request failed: ${response.code}")
            }
            TripDataHomeParser.parse(response.body).also { parsed ->
                cachedSections = parsed
            }
        }
    }

    private fun parseVideoDetail(payload: String): VideoDetail {
        val detailInfo = JSONObject(payload)
            .optJSONObject("data")
            ?.optJSONObject("detailInfo")
            ?: throw IllegalStateException("Missing detailInfo")
        val episodes = parseEpisodes(detailInfo.optJSONArray("episodes"))
        val related = cachedSections
            ?.flatMap { it.videos }
            ?.filterNot { it.mediaKey == detailInfo.optString("mediaKey").trim() }
            ?.distinctBy { it.mediaKey }
            ?.take(12)
            .orEmpty()
        val qualities = episodes
            .mapNotNull { it.resolution }
            .distinct()
            .map { resolution ->
                PlaybackQuality(
                    resolution = resolution,
                    description = "${resolution}P",
                    mediaUrl = "",
                    isDefault = resolution == detailInfo.optString("resolution").trim(),
                )
            }
        val languages = buildList {
            val languageList = JSONObject(payload)
                .optJSONObject("data")
                ?.optJSONArray("languageList")
                ?: JSONArray()
            for (index in 0 until languageList.length()) {
                val item = languageList.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                val mediaKey = item.optString("mediaKey").trim()
                if (name.isNotBlank() && mediaKey.isNotBlank()) {
                    add(PlaybackLanguage(mediaKey = mediaKey, name = name))
                }
            }
        }
        return VideoDetail(
            mediaKey = detailInfo.optString("mediaKey").trim(),
            title = detailInfo.optString("title").trim(),
            coverUrl = detailInfo.optString("coverImgUrl").trim(),
            videoType = detailInfo.optInt("videoType"),
            typeName = detailInfo.optString("typeName").trim().ifBlank { null },
            director = detailInfo.optString("director").trim().ifBlank { null },
            actor = detailInfo.optString("actor").trim().ifBlank { null },
            introduce = detailInfo.optString("introduce").trim().ifBlank { null },
            playCount = detailInfo.optInt("playCount"),
            comments = detailInfo.optInt("comments"),
            shareCount = detailInfo.optInt("shareCount"),
            publishTime = detailInfo.optString("publishTime").trim().ifBlank { null },
            updateMsg = detailInfo.optString("updateStatus").trim().ifBlank { null },
            commentEnabled = detailInfo.optInt("commentStatus", 0) == 0,
            episodes = episodes,
            qualities = qualities,
            languages = languages,
            related = related,
        )
    }

    private fun parseEpisodes(items: JSONArray?): List<Episode> =
        buildList {
            if (items == null) return@buildList
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val episodeKey = item.optString("episodeKey").trim()
                if (episodeKey.isBlank()) continue
                add(
                    Episode(
                        episodeKey = episodeKey,
                        episodeTitle = item.optString("episodeTitle").trim().ifBlank { "${index + 1}" },
                        uniqueId = item.optInt("episodeId"),
                        mediaUrl = item.optString("mediaUrl").trim().ifBlank { null },
                        resolution = item.optString("resolution").trim().ifBlank { null },
                        lang = item.optString("lang").trim().ifBlank { null },
                        duration = null,
                    ),
                )
            }
        }

    private fun parsePlayableEpisode(payload: String, fallbackEpisode: Episode): Episode {
        val items = JSONObject(payload)
            .optJSONObject("data")
            ?.optJSONArray("list")
            ?: JSONArray()
        var best: JSONObject? = null
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val mediaUrl = item.optString("mediaUrl").trim()
            if (mediaUrl.isBlank()) continue
            if (item.optBoolean("isDefault")) {
                best = item
                break
            }
            if (best == null) {
                best = item
            }
        }
        val chosen = best ?: return fallbackEpisode
        return fallbackEpisode.copy(
            episodeKey = chosen.optString("episodeKey").trim().ifBlank { fallbackEpisode.episodeKey },
            uniqueId = chosen.optInt("episodeId", fallbackEpisode.uniqueId),
            mediaUrl = chosen.optString("mediaUrl").trim().ifBlank { fallbackEpisode.mediaUrl },
            resolution = chosen.optString("resolution").trim().ifBlank { fallbackEpisode.resolution },
            lang = chosen.optString("lang").trim().ifBlank { fallbackEpisode.lang },
        )
    }

    private fun buildUrl(baseUrl: String, path: String, params: Map<String, String>): String {
        if (params.isEmpty()) return "$baseUrl$path"
        val query = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "$baseUrl$path?$query"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
