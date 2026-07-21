package com.aiyifan.app.core.data

import com.aiyifan.app.core.data.remote.RemoteCatalogRepository

object AppGraph {
    val catalogRepository: CatalogRepository by lazy { RemoteCatalogRepository() }
}
