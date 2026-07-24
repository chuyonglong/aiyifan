package com.aiyifan.app.feature.proxy.runtime

import com.aiyifan.app.core.data.remote.LocalProxyEndpoint
import com.aiyifan.app.feature.proxy.domain.ProxyNode

interface SingBoxEngine {
    fun start(config: String)

    fun stop()
}

class SingBoxRuntime(
    private val engine: SingBoxEngine,
    private val configProvider: SingBoxConfigProvider,
    private val localPort: Int = DEFAULT_LOCAL_PORT,
) {
    var activeEndpoint: LocalProxyEndpoint? = null
        private set

    fun connect(node: ProxyNode): LocalProxyEndpoint {
        disconnect()
        try {
            engine.start(configProvider.build(node, localPort))
        } catch (error: Throwable) {
            engine.stop()
            throw error
        }
        return LocalProxyEndpoint(LOOPBACK_ADDRESS, localPort).also { activeEndpoint = it }
    }

    fun disconnect() {
        if (activeEndpoint != null) {
            engine.stop()
            activeEndpoint = null
        }
    }

    private companion object {
        const val LOOPBACK_ADDRESS = "127.0.0.1"
        const val DEFAULT_LOCAL_PORT = 2080
    }
}
