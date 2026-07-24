package com.aiyifan.app.feature.proxy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
        val firstAttempt = requireNotNull(machine.activeAttempt())
        val second = machine.connect(node)

        assertEquals(ProxyConnectionState.Connecting(node), first)
        assertSame(first, second)
        assertSame(firstAttempt, machine.activeAttempt())

        val connected = machine.completeConnection(firstAttempt)
        assertEquals(ProxyConnectionState.Connected(node), connected)
        assertSame(connected, machine.connect(node))
    }

    @Test
    fun `disconnect is idempotent from every state`() {
        val machine = ProxyConnectionStateMachine()
        machine.connect(node)
        val attempt = requireNotNull(machine.activeAttempt())

        val first = machine.disconnect()
        val second = machine.disconnect()

        assertEquals(ProxyConnectionState.Disconnected, first)
        assertSame(first, second)
        assertEquals(ProxyConnectionState.Disconnected, machine.completeConnection(attempt))
    }

    @Test
    fun `connection failure can be retried and does not retain node credentials`() {
        val machine = ProxyConnectionStateMachine()
        machine.connect(node)
        val attempt = requireNotNull(machine.activeAttempt())

        val failed = machine.failConnection(attempt)

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
        val firstAttempt = requireNotNull(machine.activeAttempt())
        machine.connect(secondNode)
        val secondAttempt = requireNotNull(machine.activeAttempt())

        assertEquals(ProxyConnectionState.Connecting(secondNode), machine.completeConnection(firstAttempt))
        assertEquals(ProxyConnectionState.Connecting(secondNode), machine.failConnection(firstAttempt))
        assertEquals(ProxyConnectionState.Connected(secondNode), machine.completeConnection(secondAttempt))
    }

    @Test
    fun `stale callbacks cannot change a reconnect attempt for the same node`() {
        val machine = ProxyConnectionStateMachine()
        machine.connect(node)
        val firstAttempt = requireNotNull(machine.activeAttempt())
        machine.disconnect()
        machine.connect(node)
        val secondAttempt = requireNotNull(machine.activeAttempt())

        assertNotEquals(firstAttempt, secondAttempt)
        assertEquals(ProxyConnectionState.Connecting(node), machine.completeConnection(firstAttempt))
        assertEquals(ProxyConnectionState.Connecting(node), machine.failConnection(firstAttempt))
        assertEquals(ProxyConnectionState.Connected(node), machine.completeConnection(secondAttempt))
    }
}
