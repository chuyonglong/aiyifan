package com.aiyifan.app.feature.proxy.domain

sealed interface ProxyConnectionState {
    data object Disconnected : ProxyConnectionState
    data class Connecting(val node: ProxyNode) : ProxyConnectionState
    data class Connected(val node: ProxyNode) : ProxyConnectionState
    data object Failed : ProxyConnectionState
}

class ProxyConnectionAttempt internal constructor() {
    override fun toString(): String = "ProxyConnectionAttempt"
}

class ProxyConnectionStateMachine {
    var state: ProxyConnectionState = ProxyConnectionState.Disconnected
        private set
    private var currentAttempt: ProxyConnectionAttempt? = null

    fun connect(node: ProxyNode): ProxyConnectionState {
        if (state.isForNode(node)) return state
        currentAttempt = ProxyConnectionAttempt()
        state = ProxyConnectionState.Connecting(node)
        return state
    }

    fun activeAttempt(): ProxyConnectionAttempt? =
        currentAttempt?.takeIf { state is ProxyConnectionState.Connecting }

    fun completeConnection(callbackAttempt: ProxyConnectionAttempt): ProxyConnectionState {
        val connecting = state as? ProxyConnectionState.Connecting ?: return state
        if (currentAttempt !== callbackAttempt) return state
        state = ProxyConnectionState.Connected(connecting.node)
        return state
    }

    fun failConnection(callbackAttempt: ProxyConnectionAttempt): ProxyConnectionState {
        val connecting = state as? ProxyConnectionState.Connecting ?: return state
        if (currentAttempt !== callbackAttempt) return state
        state = ProxyConnectionState.Failed
        return state
    }

    fun disconnect(): ProxyConnectionState {
        currentAttempt = null
        if (state !is ProxyConnectionState.Disconnected) {
            state = ProxyConnectionState.Disconnected
        }
        return state
    }

    private fun ProxyConnectionState.isForNode(node: ProxyNode): Boolean = when (this) {
        is ProxyConnectionState.Connecting -> this.node.id == node.id
        is ProxyConnectionState.Connected -> this.node.id == node.id
        else -> false
    }
}
