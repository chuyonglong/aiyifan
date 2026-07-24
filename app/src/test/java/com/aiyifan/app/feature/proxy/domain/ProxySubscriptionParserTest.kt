package com.aiyifan.app.feature.proxy.domain

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxySubscriptionParserTest {

    private val parser = ProxySubscriptionParser()

    @Test
    fun `parser imports a standard base64 VLESS node`() {
        val subscription = encode(
            "vless://123e4567-e89b-12d3-a456-426614174000@edge.example.com:443?security=tls#Tokyo",
        )

        val result = parser.parse(subscription)

        val imported = result as SubscriptionImportResult.Imported
        val node = imported.nodes.single()
        assertEquals(ProxyProtocol.VLESS, node.protocol)
        assertEquals("edge.example.com", node.host)
        assertEquals(443, node.port)
        assertEquals("Tokyo", node.displayName)
    }

    @Test
    fun `parser accepts URL-safe base64 without padding`() {
        val source = "vless://123e4567-e89b-12d3-a456-426614174000@edge.example.com:443?security=tls#Tokyo"
        val subscription = Base64.getUrlEncoder().withoutPadding().encodeToString(source.toByteArray())

        val result = parser.parse(subscription)

        assertTrue(result is SubscriptionImportResult.Imported)
    }

    @Test
    fun `parser recognizes supported protocol node lines`() {
        val subscription = encode(
            """
                vless://123e4567-e89b-12d3-a456-426614174000@vless.example.com:443?security=tls#VLESS
                trojan://secret@trojan.example.com:443#Trojan
                ss://YWVzLTI1Ni1nY206cGFzc3dvcmQ=@ss.example.com:8388#Shadowsocks
                vmess://eyJhZGQiOiJ2bWVzcy5leGFtcGxlLmNvbSIsInBvcnQiOiI0NDMifQ==
            """.trimIndent(),
        )

        val result = parser.parse(subscription)

        val imported = result as SubscriptionImportResult.Imported
        assertEquals(
            listOf(ProxyProtocol.VLESS, ProxyProtocol.TROJAN, ProxyProtocol.SHADOWSOCKS, ProxyProtocol.VMESS),
            imported.nodes.map { it.protocol },
        )
    }

    @Test
    fun `parser rejects blank invalid and unsupported subscriptions`() {
        assertEquals(
            SubscriptionImportError.EMPTY_SUBSCRIPTION,
            (parser.parse("   ") as SubscriptionImportResult.Rejected).error,
        )
        assertEquals(
            SubscriptionImportError.INVALID_BASE64,
            (parser.parse("not base64!") as SubscriptionImportResult.Rejected).error,
        )
        assertEquals(
            SubscriptionImportError.NO_SUPPORTED_NODES,
            (parser.parse(encode("https://example.com/config")) as SubscriptionImportResult.Rejected).error,
        )
    }

    @Test
    fun `parser rejects VMess nodes with malformed payloads`() {
        val result = parser.parse(encode("vmess://garbage"))

        assertEquals(
            SubscriptionImportError.NO_SUPPORTED_NODES,
            (result as SubscriptionImportResult.Rejected).error,
        )
    }

    @Test
    fun `parser reads VMess endpoint fields from its payload`() {
        val payload = Base64.getEncoder().encodeToString(
            """{"add":"vmess.example.com","port":"443","ps":"VMess Tokyo"}""".toByteArray(),
        )

        val result = parser.parse(encode("vmess://$payload"))

        val node = (result as SubscriptionImportResult.Imported).nodes.single()
        assertEquals("vmess.example.com", node.host)
        assertEquals(443, node.port)
        assertEquals("VMess Tokyo", node.displayName)
    }

    @Test
    fun `parser does not expose credential material through results`() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000"
        val password = "top-secret-password"
        val result = parser.parse(encode("vless://$uuid@edge.example.com:443?password=$password#Tokyo"))

        assertFalse(result.toString().contains(uuid))
        assertFalse(result.toString().contains(password))
        assertFalse((result as SubscriptionImportResult.Imported).nodes.single().toString().contains(uuid))
    }

    private fun encode(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray())
}
