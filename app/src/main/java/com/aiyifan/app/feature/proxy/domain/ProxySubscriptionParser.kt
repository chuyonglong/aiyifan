package com.aiyifan.app.feature.proxy.domain

import android.util.Base64
import java.net.URI
import java.util.UUID
import org.json.JSONObject

sealed interface SubscriptionImportResult {
    data class Imported(val nodes: List<ProxyNode>) : SubscriptionImportResult
    data class Rejected(val error: SubscriptionImportError) : SubscriptionImportResult
}

enum class SubscriptionImportError {
    EMPTY_SUBSCRIPTION,
    INVALID_BASE64,
    NO_SUPPORTED_NODES,
}

class ProxySubscriptionParser(
    private val decodeBase64Bytes: (encoded: String, urlSafe: Boolean) -> ByteArray? = ::decodeWithAndroidBase64,
) {

    fun parse(subscription: String): SubscriptionImportResult {
        if (subscription.isBlank()) {
            return SubscriptionImportResult.Rejected(SubscriptionImportError.EMPTY_SUBSCRIPTION)
        }

        val decoded = decodeBase64(subscription)
            ?: return SubscriptionImportResult.Rejected(SubscriptionImportError.INVALID_BASE64)
        val nodes = decoded.lineSequence().mapNotNull(::parseNode).toList()

        return if (nodes.isEmpty()) {
            SubscriptionImportResult.Rejected(SubscriptionImportError.NO_SUPPORTED_NODES)
        } else {
            SubscriptionImportResult.Imported(nodes)
        }
    }

    private fun decodeBase64(subscription: String): String? {
        val encoded = subscription.filterNot(Char::isWhitespace)
        val bytes = decodeBase64Bytes(encoded, false)
            ?: decodeBase64Bytes(encoded, true)
            ?: return null
        return runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull()
    }

    private fun parseNode(line: String): ProxyNode? {
        val rawNode = line.trim()
        if (rawNode.isEmpty()) return null

        val uri = runCatching { URI(rawNode) }.getOrNull() ?: return null
        return when (uri.scheme?.lowercase()) {
            "vless" -> parseStandardNode(uri, rawNode, ProxyProtocol.VLESS, requireUuid = true)
            "trojan" -> parseStandardNode(uri, rawNode, ProxyProtocol.TROJAN, requireUuid = false)
            "ss" -> parseStandardNode(uri, rawNode, ProxyProtocol.SHADOWSOCKS, requireUuid = false)
            "vmess" -> parseVmessNode(uri, rawNode)
            else -> null
        }
    }

    private fun parseStandardNode(
        uri: URI,
        rawNode: String,
        protocol: ProxyProtocol,
        requireUuid: Boolean,
    ): ProxyNode? {
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it in 1..65535 } ?: return null
        val credential = uri.userInfo?.takeIf { it.isNotBlank() } ?: return null
        if (requireUuid && runCatching { UUID.fromString(credential) }.isFailure) return null

        return ProxyNode(
            protocol = protocol,
            host = host,
            port = port,
            displayName = uri.fragment?.takeIf { it.isNotBlank() } ?: host,
            serializedNode = rawNode,
        )
    }

    private fun parseVmessNode(uri: URI, rawNode: String): ProxyNode? {
        val payload = uri.rawSchemeSpecificPart.removePrefix("//").takeIf { it.isNotBlank() } ?: return null
        val json = decodeBase64(payload) ?: return null
        val config = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val host = config.optString("add").trim().takeIf { it.isNotEmpty() } ?: return null
        val port = config.optInt("port", -1).takeIf { it in 1..65535 } ?: return null

        return ProxyNode(
            protocol = ProxyProtocol.VMESS,
            host = host,
            port = port,
            displayName = config.optString("ps").trim().takeIf { it.isNotEmpty() } ?: host,
            serializedNode = rawNode,
        )
    }
}

private fun decodeWithAndroidBase64(encoded: String, urlSafe: Boolean): ByteArray? {
    val flags = Base64.NO_WRAP or if (urlSafe) Base64.URL_SAFE else 0
    return runCatching { Base64.decode(encoded, flags) }.getOrNull()
}
