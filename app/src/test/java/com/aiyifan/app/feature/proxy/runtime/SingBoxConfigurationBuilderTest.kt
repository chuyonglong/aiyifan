package com.aiyifan.app.feature.proxy.runtime

import com.aiyifan.app.feature.proxy.domain.ProxySubscriptionParser
import com.aiyifan.app.feature.proxy.domain.SubscriptionImportResult
import java.util.Base64
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SingBoxConfigurationBuilderTest {

    @Test
    fun `builds loopback mixed inbound and VLESS outbound`() {
        val node = importedNode(
            "vless://123e4567-e89b-12d3-a456-426614174000@edge.example.com:443" +
                "?encryption=none&security=reality&sni=cdn.example.com&pbk=public-key&sid=abcd&type=tcp#Edge",
        )

        val config = JSONObject(SingBoxConfigurationBuilder().build(node, localPort = 2080))

        val inbound = config.getJSONArray("inbounds").getJSONObject(0)
        assertEquals("mixed", inbound.getString("type"))
        assertEquals("127.0.0.1", inbound.getString("listen"))
        assertEquals(2080, inbound.getInt("listen_port"))

        val outbound = config.getJSONArray("outbounds").getJSONObject(0)
        assertEquals("vless", outbound.getString("type"))
        assertEquals("edge.example.com", outbound.getString("server"))
        assertEquals(443, outbound.getInt("server_port"))
        assertEquals("123e4567-e89b-12d3-a456-426614174000", outbound.getString("uuid"))
        val tls = outbound.getJSONObject("tls")
        assertEquals("cdn.example.com", tls.getString("server_name"))
        assertEquals(true, tls.getJSONObject("reality").getBoolean("enabled"))
        assertEquals("proxy", config.getJSONObject("route").getString("final"))
    }

    private fun importedNode(uri: String) =
        (ProxySubscriptionParser(::decodeWithJvmBase64).parse(encode(uri)) as SubscriptionImportResult.Imported).nodes.single()

    private fun encode(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray())

    private fun decodeWithJvmBase64(encoded: String, urlSafe: Boolean): ByteArray? = runCatching {
        if (urlSafe) {
            Base64.getUrlDecoder().decode(encoded)
        } else {
            Base64.getDecoder().decode(encoded)
        }
    }.getOrNull()
}
