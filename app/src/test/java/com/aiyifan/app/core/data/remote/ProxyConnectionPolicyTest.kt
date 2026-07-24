package com.aiyifan.app.core.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.io.ByteArrayInputStream

class ProxyConnectionPolicyTest {

    @Test
    fun `uses SOCKS proxy only when a local endpoint is active`() {
        val endpoint = LocalProxyEndpoint(host = "127.0.0.1", port = 2080)

        val proxy = ProxyConnectionPolicy.select(endpoint)

        assertEquals(java.net.Proxy.Type.SOCKS, proxy?.type())
        assertEquals("127.0.0.1", (proxy?.address() as java.net.InetSocketAddress).hostString)
        assertEquals(2080, (proxy.address() as java.net.InetSocketAddress).port)
    }

    @Test
    fun `uses direct connection when no local endpoint is active`() {
        assertNull(ProxyConnectionPolicy.select(null))
    }

    @Test
    fun `fetcher opens catalog request through active local proxy`() = runBlocking {
        val opener = RecordingConnectionOpener()
        val fetcher = UrlConnectionHttpFetcher(
            endpointProvider = { LocalProxyEndpoint("127.0.0.1", 2080) },
            connectionOpener = opener,
        )

        fetcher.get("https://example.com/catalog")

        assertEquals(Proxy.Type.SOCKS, opener.proxy?.type())
    }

    @Test
    fun `fetcher opens subscription refresh directly when requested`() = runBlocking {
        val opener = RecordingConnectionOpener()
        val fetcher = UrlConnectionHttpFetcher(
            endpointProvider = { LocalProxyEndpoint("127.0.0.1", 2080) },
            connectionOpener = opener,
        )

        fetcher.getDirect("https://example.com/subscription")

        assertNull(opener.proxy)
    }

    private class RecordingConnectionOpener : HttpConnectionOpener {
        var proxy: Proxy? = Proxy.NO_PROXY

        override fun open(url: URL, proxy: Proxy?): HttpURLConnection {
            this.proxy = proxy
            return FakeHttpURLConnection(url)
        }
    }

    private class FakeHttpURLConnection(url: URL) : HttpURLConnection(url) {
        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int = 204

        override fun getInputStream() = ByteArrayInputStream(ByteArray(0))
    }
}
