package com.aiyifan.app.feature.video

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerAttachmentCoordinatorTest {

    @Test
    fun `switching host detaches old host first`() {
        val events = mutableListOf<String>()
        val page = RecordingHost("page", events)
        val overlay = RecordingHost("overlay", events)
        val coordinator = PlayerAttachmentCoordinator<String>()

        coordinator.attach("player", page)
        coordinator.attach("player", overlay)

        assertEquals(listOf("page:attach", "page:detach", "overlay:attach"), events)
    }

    @Test
    fun `detaching removes the current host`() {
        val events = mutableListOf<String>()
        val page = RecordingHost("page", events)
        val coordinator = PlayerAttachmentCoordinator<String>()

        coordinator.attach("player", page)
        coordinator.detach()

        assertEquals(listOf("page:attach", "page:detach"), events)
    }

    private class RecordingHost(
        private val name: String,
        private val events: MutableList<String>,
    ) : PlayerHost<String> {
        override fun attach(player: String) {
            events += "$name:attach"
        }

        override fun detach() {
            events += "$name:detach"
        }
    }
}
