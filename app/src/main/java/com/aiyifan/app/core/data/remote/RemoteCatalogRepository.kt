package com.aiyifan.app.core.data.remote

import com.aiyifan.app.core.data.CatalogRepository
import com.aiyifan.app.core.data.FakeCatalogRepository
import com.aiyifan.app.core.model.Category
import com.aiyifan.app.core.model.Comment
import com.aiyifan.app.core.model.Episode
import com.aiyifan.app.core.model.FavoriteVideo
import com.aiyifan.app.core.model.PlaybackLanguage
import com.aiyifan.app.core.model.PlaybackQuality
import com.aiyifan.app.core.model.SearchSuggestion
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

    override suspend fun refreshHome() {
        cacheLock.withLock {
            cachedSections = fetchHomeSections()
        }
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

    override suspend fun searchVideos(keyword: String): List<VideoSummary> {
        val query = keyword.trim()
        if (query.isEmpty()) return emptyList()
        return runCatching { searchRemoteVideos(query) }
            .getOrDefault(emptyList())
            .ifEmpty { searchCachedVideos(query) }
            .ifEmpty { fallback.searchVideos(query) }
    }

    override suspend fun searchSuggestions(keyword: String): List<SearchSuggestion> {
        val query = keyword.trim()
        if (query.isEmpty()) return emptyList()
        return try {
            val baseUrl = configResolver.resolveBaseUrl()
            val response = fetcher.get(buildUrl(baseUrl, "api/Home/GetKeyWord", mapOf("keyword" to query)))
            if (response.code !in 200..299) throw IllegalStateException("Suggest failed: ${response.code}")
            parseSearchSuggestions(response.body, query)
        } catch (_: Throwable) {
            localSuggestions(query)
        }
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
            fetchHomeSections().also { parsed ->
                cachedSections = parsed
            }
        }
    }

    private suspend fun fetchHomeSections(): List<TripDataHomeSection> {
        val baseUrl = configResolver.resolveBaseUrl()
        val response = fetcher.get("${baseUrl}api/List/Index")
        if (response.code !in 200..299) {
            throw IllegalStateException("Home request failed: ${response.code}")
        }
        return TripDataHomeParser.parse(response.body).also { sections ->
            check(sections.isNotEmpty()) { "Home response contains no sections" }
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

    private suspend fun searchRemoteVideos(query: String): List<VideoSummary> {
        val baseUrl = configResolver.resolveBaseUrl()
        val response = fetcher.get(
            buildUrl(
                baseUrl,
                "api/Search/GetSearch",
                linkedMapOf("keyword" to query, "region" to DEFAULT_REGION),
            ),
        )
        if (response.code !in 200..299) throw IllegalStateException("Search failed: ${response.code}")
        val payload = JSONObject(response.body)
        if (payload.optInt("ret", 200) != 200) throw IllegalStateException("Search service returned an error")
        return parseSearchVideos(payload)
    }

    private suspend fun searchCachedVideos(query: String): List<VideoSummary> =
        try {
            ensureSections()
                .flatMap { it.videos }
                .distinctBy { it.mediaKey }
                .filter { video -> video.matchesSearch(query) }
        } catch (_: Throwable) {
            emptyList()
        }

    private fun parseSearchVideos(payload: JSONObject): List<VideoSummary> {
        val data = payload.opt("data")
        val items = when (data) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("list")
                ?: data.optJSONArray("videoList")
                ?: data.optJSONArray("items")
                ?: JSONArray()
            else -> JSONArray()
        }
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val mediaKey = item.optString("mediaKey").trim().ifBlank { item.optString("videoKey").trim() }
                if (mediaKey.isBlank()) continue
                val publishTime = item.optString("publishTime").trim()
                add(
                    VideoSummary(
                        mediaKey = mediaKey,
                        episodeKey = item.optString("episodeKey").trim().ifBlank { null },
                        title = item.optString("title").trim(),
                        coverUrl = item.optString("coverImgUrl").trim(),
                        videoType = item.optInt("videoType", item.optInt("type", 0)),
                        contentType = item.optString("contentType").trim().ifBlank { item.optString("typeName").trim().ifBlank { null } },
                        mediaType = item.optString("mediaType").trim().ifBlank { null },
                        score = item.optString("score").trim().ifBlank { null },
                        playCount = item.optInt("playCount"),
                        updateStatus = item.optString("updateStatus").trim().ifBlank { null },
                        year = publishTime.takeIf { it.length >= 4 }?.take(4),
                        area = item.optString("regional").trim().ifBlank { null },
                        actor = item.optString("actor").trim().ifBlank { null },
                        director = item.optString("director").trim().ifBlank { null },
                        episodePreviews = parseEpisodePreviews(item.optJSONArray("episodes")),
                    ),
                )
            }
        }
    }

    private fun parseEpisodePreviews(items: JSONArray?): List<Episode> =
        parseEpisodes(items).take(6)

    private suspend fun localSuggestions(query: String): List<SearchSuggestion> =
        try {
            ensureSections()
                .flatMap { it.videos }
                .flatMap { video -> listOf(video.title, video.actor.orEmpty(), video.director.orEmpty()) }
                .filter { value -> value.contains(query, ignoreCase = true) }
                .distinct()
                .take(8)
                .map(::SearchSuggestion)
        } catch (_: Throwable) {
            fallback.searchSuggestions(query)
        }

    private fun parseSearchSuggestions(payload: String, query: String): List<SearchSuggestion> {
        val data = JSONObject(payload).opt("data")
        val items = when (data) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("list") ?: data.optJSONArray("items") ?: JSONArray()
            else -> JSONArray()
        }
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.opt(index)
                val value = when (item) {
                    is String -> item
                    is JSONObject -> item.optString("keyword")
                        .ifBlank { item.optString("name") }
                        .ifBlank { item.optString("title") }
                    else -> ""
                }.trim()
                if (value.isNotBlank()) add(SearchSuggestion(value))
            }
        }.distinctBy { it.keyword }.take(8).ifEmpty { localSuggestionFallback(query) }
    }

    private fun localSuggestionFallback(query: String): List<SearchSuggestion> =
        cachedSections.orEmpty()
            .flatMap { it.videos }
            .flatMap { video -> listOf(video.title, video.actor.orEmpty(), video.director.orEmpty()) }
            .filter { value -> value.contains(query, ignoreCase = true) }
            .distinct()
            .take(8)
            .map(::SearchSuggestion)

    private fun VideoSummary.matchesSearch(query: String): Boolean =
        title.contains(query, ignoreCase = true) ||
            contentType.orEmpty().contains(query, ignoreCase = true) ||
            mediaType.orEmpty().contains(query, ignoreCase = true) ||
            area.orEmpty().contains(query, ignoreCase = true) ||
            year.orEmpty().contains(query, ignoreCase = true) ||
            actor.orEmpty().contains(query, ignoreCase = true) ||
            director.orEmpty().contains(query, ignoreCase = true)

    private fun buildUrl(baseUrl: String, path: String, params: Map<String, String>): String {
        if (params.isEmpty()) return "$baseUrl$path"
        val query = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "$baseUrl$path?$query"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        const val DEFAULT_REGION = "cn"
    }
}
