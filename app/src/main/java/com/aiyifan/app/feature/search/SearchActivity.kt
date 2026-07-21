package com.aiyifan.app.feature.search

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aiyifan.app.core.data.AppGraph
import com.aiyifan.app.core.ui.VideoListAdapter
import com.aiyifan.app.core.ui.applySystemBarsPadding
import com.aiyifan.app.core.ui.setupEdgeToEdge
import com.aiyifan.app.databinding.ActivitySearchBinding
import com.aiyifan.app.feature.video.VideoPlayerActivity
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: VideoListAdapter
    private val historyPrefs by lazy { getSharedPreferences("search_history", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(left = true, right = true, bottom = true)
        binding.topBar.applySystemBarsPadding(top = true, growHeight = true)

        adapter = VideoListAdapter { video ->
            startActivity(VideoPlayerActivity.intent(this, video.mediaKey))
        }
        binding.resultRecycler.layoutManager = LinearLayoutManager(this)
        binding.resultRecycler.adapter = adapter
        binding.backButton.setOnClickListener { finish() }
        binding.searchButton.setOnClickListener { executeSearch() }
        binding.searchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                executeSearch()
                true
            } else {
                false
            }
        }
        renderHistoryHint()
    }

    private fun executeSearch() {
        val keyword = binding.searchEdit.text.toString().trim()
        if (keyword.isEmpty()) {
            Toast.makeText(this, "Enter a keyword", Toast.LENGTH_SHORT).show()
            return
        }
        saveKeyword(keyword)
        binding.searchHint.text = "Searching..."
        lifecycleScope.launch {
            runCatching { AppGraph.catalogRepository.searchVideos(keyword) }
                .onSuccess { results ->
                    adapter.submitList(results)
                    binding.searchHint.text = if (results.isEmpty()) {
                        "No matching videos"
                    } else {
                        "Found ${results.size} videos"
                    }
                }
                .onFailure {
                    adapter.submitList(emptyList())
                    binding.searchHint.text = "Search failed"
                }
        }
    }

    private fun saveKeyword(keyword: String) {
        val old = historyPrefs.getStringSet("keywords", emptySet()).orEmpty()
        historyPrefs.edit().putStringSet("keywords", (listOf(keyword) + old).take(20).toSet()).apply()
    }

    private fun renderHistoryHint() {
        val words = historyPrefs.getStringSet("keywords", emptySet()).orEmpty().take(3)
        binding.searchHint.text = if (words.isEmpty()) {
            "Try movie, anime, variety"
        } else {
            "History: ${words.joinToString(", ")}"
        }
    }
}
