package com.aiyifan.app.feature.video

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.aiyifan.app.core.data.AppGraph
import com.aiyifan.app.core.model.Episode
import com.aiyifan.app.core.model.VideoDetail
import com.aiyifan.app.core.ui.CommentAdapter
import com.aiyifan.app.core.ui.EpisodeAdapter
import com.aiyifan.app.core.ui.VideoListAdapter
import com.aiyifan.app.core.ui.applySystemBarsPadding
import com.aiyifan.app.core.ui.setupEdgeToEdge
import com.aiyifan.app.databinding.ActivityVideoPlayerBinding
import kotlinx.coroutines.launch

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null
    private var detail: VideoDetail? = null
    private var selectedEpisode: Episode? = null
    private var playingEpisode: Episode? = null
    private lateinit var episodeAdapter: EpisodeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(lightSystemBars = false)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(left = true, right = true, bottom = true)
        binding.playerContainer.applySystemBarsPadding(top = true, growHeight = true)

        binding.backButton.setOnClickListener { finish() }
        setupStaticLists()
        loadDetail(intent.getStringExtra(EXTRA_MEDIA_KEY).orEmpty())
    }

    private fun setupStaticLists() {
        episodeAdapter = EpisodeAdapter { episode ->
            selectedEpisode = episode
            detail?.let { currentDetail ->
                episodeAdapter.submitList(currentDetail.episodes, episode)
                loadEpisodePlayback(currentDetail, episode)
            }
        }
        binding.episodeRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.episodeRecycler.adapter = episodeAdapter

        binding.relatedRecycler.layoutManager = LinearLayoutManager(this)
        binding.commentRecycler.layoutManager = LinearLayoutManager(this)
    }

    private fun loadDetail(mediaKey: String) {
        if (mediaKey.isBlank()) {
            Toast.makeText(this, "Video not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.title.text = "Loading..."
        lifecycleScope.launch {
            runCatching { AppGraph.catalogRepository.getVideoDetail(mediaKey) }
                .onSuccess { loadedDetail ->
                    detail = loadedDetail
                    selectedEpisode = loadedDetail.defaultEpisode
                    renderDetail(loadedDetail)
                    loadedDetail.defaultEpisode?.let { episode ->
                        episodeAdapter.submitList(loadedDetail.episodes, episode)
                        loadEpisodePlayback(loadedDetail, episode)
                    }
                }
                .onFailure {
                    Toast.makeText(this@VideoPlayerActivity, "Failed to load detail", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun renderDetail(detail: VideoDetail) {
        binding.title.text = detail.title
        binding.meta.text = listOfNotNull(
            detail.typeName,
            detail.publishTime,
            detail.updateMsg,
            "Plays ${detail.playCount}",
        ).joinToString(" / ")
        binding.intro.text = buildString {
            append("Director: ")
            append(detail.director.orEmpty())
            append('\n')
            append("Cast: ")
            append(detail.actor.orEmpty())
            append('\n')
            append(detail.introduce.orEmpty())
        }
        binding.favoriteButton.text = if (AppGraph.catalogRepository.isFavorite(detail.mediaKey)) "Favorited" else "Favorite"
        binding.favoriteButton.setOnClickListener {
            val isFavorite = AppGraph.catalogRepository.toggleFavorite(detail)
            binding.favoriteButton.text = if (isFavorite) "Favorited" else "Favorite"
            Toast.makeText(this, if (isFavorite) "Added to favorites" else "Removed from favorites", Toast.LENGTH_SHORT).show()
        }
        binding.shareButton.setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, detail.title)
            }, "Share"))
        }

        val relatedAdapter = VideoListAdapter { video ->
            startActivity(intent(this, video.mediaKey))
        }
        binding.relatedRecycler.adapter = relatedAdapter
        relatedAdapter.submitList(detail.related)

        val commentAdapter = CommentAdapter()
        binding.commentRecycler.adapter = commentAdapter
        commentAdapter.submitList(AppGraph.catalogRepository.getComments(detail.mediaKey))
    }

    private fun loadEpisodePlayback(detail: VideoDetail, episode: Episode) {
        binding.meta.text = listOfNotNull(
            detail.typeName,
            detail.publishTime,
            detail.updateMsg,
            "Plays ${detail.playCount}",
            "Loading stream",
        ).joinToString(" / ")
        lifecycleScope.launch {
            runCatching { AppGraph.catalogRepository.resolvePlayback(detail, episode) }
                .onSuccess { playableEpisode ->
                    playingEpisode = playableEpisode
                    setupPlayer(playableEpisode)
                    binding.meta.text = listOfNotNull(
                        detail.typeName,
                        detail.publishTime,
                        detail.updateMsg,
                        "Plays ${detail.playCount}",
                        playableEpisode.resolution?.let { "${it}P" },
                    ).joinToString(" / ")
                }
                .onFailure {
                    Toast.makeText(this@VideoPlayerActivity, "Failed to load stream", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupPlayer(episode: Episode) {
        val mediaUrl = episode.mediaUrl
        if (mediaUrl.isNullOrBlank()) {
            Toast.makeText(this, "No playable stream", Toast.LENGTH_SHORT).show()
            return
        }
        val newPlayer = player ?: ExoPlayer.Builder(this).build().also {
            player = it
            binding.playerView.player = it
        }
        newPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)))
        newPlayer.prepare()
        newPlayer.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        val activeDetail = detail
        val episode = playingEpisode ?: selectedEpisode
        val activePlayer = player
        if (activeDetail != null && episode != null && activePlayer != null) {
            AppGraph.catalogRepository.saveHistory(
                detail = activeDetail,
                episode = episode,
                progressMs = activePlayer.currentPosition.coerceAtLeast(0L),
                durationMs = activePlayer.duration.coerceAtLeast(0L),
            )
        }
    }

    override fun onDestroy() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_MEDIA_KEY = "mediaKey"

        fun intent(context: Context, mediaKey: String): Intent =
            Intent(context, VideoPlayerActivity::class.java).putExtra(EXTRA_MEDIA_KEY, mediaKey)
    }
}
