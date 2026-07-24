package com.aiyifan.app.core.data.remote

import java.net.InetSocketAddress
import java.net.Proxy

data class LocalProxyEndpoint(
    val host: String,
    val port: Int,
)

object ProxyConnectionPolicy {
    fun select(endpoint: LocalProxyEndpoint?): Proxy? =
        endpoint?.let { Proxy(Proxy.Type.SOCKS, InetSocketAddress(it.host, it.port)) }
}
