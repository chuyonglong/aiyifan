package com.aiyifan.app.feature.proxy

import com.aiyifan.app.core.data.remote.HttpFetcher

class HttpSubscriptionContentLoader(
    private val fetcher: HttpFetcher,
) : SubscriptionContentLoader {
    override suspend fun loadDirect(url: String): String {
        val response = fetcher.getDirect(url)
        check(response.code in 200..299) { "Subscription request failed" }
        return response.body
    }
}
