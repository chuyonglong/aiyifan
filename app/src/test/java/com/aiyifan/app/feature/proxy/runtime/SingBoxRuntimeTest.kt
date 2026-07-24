package com.aiyifan.app.feature.proxy.runtime

import com.aiyifan.app.core.data.remote.LocalProxyEndpoint
import com.aiyifan.app.feature.proxy.domain.ProxyNode
import com.aiyifan.app.feature.proxy.domain.ProxyProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SingBoxRuntimeTest {

    @Test
    fun `starts local endpoint from selected node and stops it on disconnect`() {
        val engine = RecordingEngine()
        val runtime = SingBoxRuntime(engine, FakeConfigurationBuilder())
        val node = ProxyNode.forTesting(ProxyProtocol.VLESS, "edge.example.com", 443, "Edge")

        val endpoint = runtime.connect(node)

        assertEquals(LocalProxyEndpoint("127.0.0.1", 2080), endpoint)
        assertEquals("edge.example.com", engine.startedForHost)
        assertEquals(endpoint, runtime.activeEndpoint)

        runtime.disconnect()

        assertEquals(1, engine.stopCalls)
        assertNull(runtime.activeEndpoint)
    }

    private class RecordingEngine : SingBoxEngine {
        var startedForHost: String? = null
        var stopCalls = 0

        override fun start(config: String) {
            startedForHost = config
        }

        override fun stop() {
            stopCalls += 1
        }
    }

    private class FakeConfigurationBuilder : SingBoxConfigProvider {
        override fun build(node: ProxyNode, localPort: Int): String = node.host
    }
}
