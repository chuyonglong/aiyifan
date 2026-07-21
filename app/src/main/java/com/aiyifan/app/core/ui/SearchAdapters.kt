package com.aiyifan.app.core.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aiyifan.app.core.model.SearchSuggestion
import com.aiyifan.app.core.model.VideoSummary
import com.aiyifan.app.databinding.ItemSearchResultBinding
import com.aiyifan.app.databinding.ItemSearchSuggestionBinding
import com.bumptech.glide.Glide

class SearchSuggestionAdapter(
    private val onClick: (SearchSuggestion) -> Unit,
) : RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder>() {
    private val items = mutableListOf<SearchSuggestion>()

    fun submitList(suggestions: List<SearchSuggestion>) {
        items.clear()
        items.addAll(suggestions)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder =
        SuggestionViewHolder(ItemSearchSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false), onClick)

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class SuggestionViewHolder(
        private val binding: ItemSearchSuggestionBinding,
        private val onClick: (SearchSuggestion) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SearchSuggestion) {
            binding.suggestionText.text = item.keyword
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

class SearchResultAdapter(
    private val onClick: (VideoSummary) -> Unit,
) : RecyclerView.Adapter<SearchResultAdapter.ResultViewHolder>() {
    private val items = mutableListOf<VideoSummary>()

    fun submitList(results: List<VideoSummary>) {
        items.clear()
        items.addAll(results)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder =
        ResultViewHolder(ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false), onClick)

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class ResultViewHolder(
        private val binding: ItemSearchResultBinding,
        private val onClick: (VideoSummary) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoSummary) {
            binding.title.text = item.title
            binding.meta.text = listOfNotNull(item.year, item.contentType, item.area, item.updateStatus).joinToString(" / ")
            binding.credits.text = listOfNotNull(
                item.director?.let { "导演：$it" },
                item.actor?.let { "主演：$it" },
            ).joinToString("\n")
            if (item.coverUrl.isBlank()) {
                binding.poster.setImageDrawable(null)
            } else {
                Glide.with(binding.poster).load(item.coverUrl).centerCrop().into(binding.poster)
            }
            binding.root.setOnClickListener { onClick(item) }
            binding.playButton.setOnClickListener { onClick(item) }
            binding.viewAllButton.setOnClickListener { onClick(item) }
        }
    }
}
