package com.aiyifan.app.core.data

import android.content.Context
import com.aiyifan.app.core.data.remote.RemoteCatalogRepository
import com.aiyifan.app.feature.video.VideoPlaybackController

object AppGraph {
    val catalogRepository: CatalogRepository by lazy { RemoteCatalogRepository() }

    private lateinit var applicationContext: Context

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    val videoPlaybackController: VideoPlaybackController by lazy {
        check(::applicationContext.isInitialized) {
            "AppGraph must be initialized from AiyifanApp before requesting the video controller."
        }
        VideoPlaybackController.create(applicationContext, catalogRepository)
    }
}
