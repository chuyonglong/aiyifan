package com.aiyifan.app.feature.proxy.runtime

import com.aiyifan.app.feature.proxy.domain.ProxyNode
import com.aiyifan.app.feature.proxy.domain.ProxyProtocol
import java.net.URI
import java.net.URLDecoder
import org.json.JSONArray
import org.json.JSONObject

interface SingBoxConfigProvider {
    fun build(node: ProxyNode, localPort: Int): String
}

class SingBoxConfigurationBuilder : SingBoxConfigProvider {

    override fun build(node: ProxyNode, localPort: Int): String {
        require(localPort in 1..65535) { "Invalid local proxy port" }
        require(node.protocol == ProxyProtocol.VLESS) { "This node protocol is not connectable yet" }

        val uri = URI(node.serializedNode)
        val query = uri.queryParameters()
        val outbound = JSONObject()
            .put("type", "vless")
            .put("tag", PROXY_TAG)
            .put("server", node.host)
            .put("server_port", node.port)
            .put("uuid", uri.userInfo)
            .put("encryption", query["encryption"] ?: "none")

        query["flow"]?.takeIf(String::isNotBlank)?.let { outbound.put("flow", it) }
        addTransport(outbound, query)
        addTls(outbound, node.host, query)

        return JSONObject()
            .put("log", JSONObject().put("level", "warn"))
            .put(
                "inbounds",
                JSONArray().put(
                    JSONObject()
                        .put("type", "mixed")
                        .put("tag", "local")
                        .put("listen", LOOPBACK_ADDRESS)
                        .put("listen_port", localPort),
                ),
            )
            .put(
                "outbounds",
                JSONArray()
                    .put(outbound)
                    .put(JSONObject().put("type", "direct").put("tag", "direct")),
            )
            .put("route", JSONObject().put("final", PROXY_TAG))
            .toString()
    }

    private fun addTransport(outbound: JSONObject, query: Map<String, String>) {
        when (query["type"]?.lowercase()) {
            null,
            "",
            "tcp",
            -> Unit

            "ws" -> {
                val transport = JSONObject().put("type", "ws")
                query["path"]?.let { transport.put("path", it) }
                query["host"]?.let { host ->
                    transport.put("headers", JSONObject().put("Host", host))
                }
                outbound.put("transport", transport)
            }

            "grpc" -> {
                val transport = JSONObject().put("type", "grpc")
                query["serviceName"]?.let { transport.put("service_name", it) }
                outbound.put("transport", transport)
            }

            else -> throw IllegalArgumentException("Unsupported VLESS transport")
        }
    }

    private fun addTls(outbound: JSONObject, host: String, query: Map<String, String>) {
        val security = query["security"]?.lowercase()
        if (security.isNullOrBlank() || security == "none") return

        val tls = JSONObject()
            .put("enabled", true)
            .put("server_name", query["sni"]?.takeIf(String::isNotBlank) ?: host)
        query["alpn"]?.split(',')?.filter(String::isNotBlank)?.takeIf(List<String>::isNotEmpty)?.let { protocols ->
            tls.put("alpn", JSONArray(protocols))
        }
        if (security == "reality") {
            val reality = JSONObject().put("enabled", true)
            query["pbk"]?.takeIf(String::isNotBlank)?.let { reality.put("public_key", it) }
            query["sid"]?.takeIf(String::isNotBlank)?.let { reality.put("short_id", it) }
            tls.put("reality", reality)
        }
        outbound.put("tls", tls)
    }

    private fun URI.queryParameters(): Map<String, String> =
        rawQuery
            ?.split('&')
            ?.mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator < 0) {
                    null
                } else {
                    decode(part.substring(0, separator)) to decode(part.substring(separator + 1))
                }
            }
            ?.toMap()
            .orEmpty()

    private fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())

    private companion object {
        const val LOOPBACK_ADDRESS = "127.0.0.1"
        const val PROXY_TAG = "proxy"
    }
}
