package com.aiyifan.app.feature.proxy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class ProxyConnectionStateMachineTest {

    private val node = ProxyNode.forTesting(
        protocol = ProxyProtocol.VLESS,
        host = "edge.example.com",
        port = 443,
        displayName = "Tokyo",
    )

    @Test
    fun `connect is idempotent while connecting and after connected`() {
        val machine = ProxyConnectionStateMachine()

        val first = machine.connect(node)
        val second = machine.connect(node)

        assertEquals(ProxyConnectionState.Connecting(node), first)
        assertSame(first, second)

        val connected = machine.completeConnection(node.id)
        assertEquals(ProxyConnectionState.Connected(node), connected)
        assertSame(connected, machine.connect(node))
    }

    @Test
    fun `disconnect is idempotent from every state`() {
        val machine = ProxyConnectionStateMachine()
        machine.connect(node)

        val first = machine.disconnect()
        val second = machine.disconnect()

        assertEquals(ProxyConnectionState.Disconnected, first)
        assertSame(first, second)
        assertEquals(ProxyConnectionState.Disconnected, machine.completeConnection(node.id))
    }

    @Test
    fun `connection failure can be retried and does not retain node credentials`() {
        val machine = ProxyConnectionStateMachine()
        machine.connect(node)

        val failed = machine.failConnection(node.id)

        assertEquals(ProxyConnectionState.Failed, failed)
        assertEquals(ProxyConnectionState.Connecting(node), machine.connect(node))
        assertEquals("Failed", failed.toString())
    }

    @Test
    fun `node names are omitted from node and connection state string representations`() {
        val untrustedName = "password=top-secret"
        val namedNode = ProxyNode.forTesting(
            protocol = ProxyProtocol.VLESS,
            host = "edge.example.com",
            port = 443,
            displayName = untrustedName,
        )

        assertFalse(namedNode.toString().contains(untrustedName))
        assertFalse(ProxyConnectionState.Connecting(namedNode).toString().contains(untrustedName))
        assertFalse(ProxyConnectionState.Connected(namedNode).toString().contains(untrustedName))
    }

    @Test
    fun `stale node callbacks cannot change the current selection state`() {
        val firstNode = node
        val secondNode = ProxyNode.forTesting(
            protocol = ProxyProtocol.VLESS,
            host = "backup.example.com",
            port = 8443,
            displayName = "Backup",
        )
        val machine = ProxyConnectionStateMachine()
        machine.connect(firstNode)
        machine.connect(secondNode)

        assertEquals(ProxyConnectionState.Connecting(secondNode), machine.completeConnection(firstNode.id))
        assertEquals(ProxyConnectionState.Connecting(secondNode), machine.failConnection(firstNode.id))
        assertEquals(ProxyConnectionState.Connected(secondNode), machine.completeConnection(secondNode.id))
    }
}
