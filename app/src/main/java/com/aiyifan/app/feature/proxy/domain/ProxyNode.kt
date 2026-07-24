package com.aiyifan.app.feature.proxy.domain

import java.security.MessageDigest

enum class ProxyProtocol {
    VLESS,
    TROJAN,
    SHADOWSOCKS,
    VMESS,
}

class ProxyNode internal constructor(
    val protocol: ProxyProtocol,
    val host: String,
    val port: Int,
    val displayName: String,
    internal val serializedNode: String,
) {
    val id: String = serializedNode.sha256Prefix()

    override fun toString(): String = "ProxyNode(protocol=$protocol, host=$host, port=$port)"

    companion object {
        internal fun forTesting(
            protocol: ProxyProtocol,
            host: String,
            port: Int,
            displayName: String,
        ): ProxyNode = ProxyNode(
            protocol = protocol,
            host = host,
            port = port,
            displayName = displayName,
            serializedNode = "$protocol://$host:$port#$displayName",
        )
    }
}

private fun String.sha256Prefix(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
        .take(16)
