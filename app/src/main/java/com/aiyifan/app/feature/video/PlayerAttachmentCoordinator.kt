package com.aiyifan.app.feature.video

interface PlayerHost<T> {
    fun attach(player: T)

    fun detach()
}

class PlayerAttachmentCoordinator<T> {
    private var attachedHost: PlayerHost<T>? = null

    fun attach(player: T, host: PlayerHost<T>) {
        if (attachedHost === host) return

        attachedHost?.detach()
        host.attach(player)
        attachedHost = host
    }

    fun detach() {
        attachedHost?.detach()
        attachedHost = null
    }
}
