package com.aiyifan.app.feature.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlayerBackBehaviorTest {

    @Test
    fun `full screen back exits full screen`() {
        assertEquals(
            VideoPlayerBackAction.EXIT_FULL_SCREEN,
            VideoPlayerBackBehavior.action(isFullScreen = true, canMinimizeInApp = true),
        )
    }

    @Test
    fun `playing video back minimizes to in app player when needed`() {
        assertEquals(
            VideoPlayerBackAction.MINIMIZE_TO_IN_APP_PLAYER,
            VideoPlayerBackBehavior.action(isFullScreen = false, canMinimizeInApp = true),
        )
    }

    @Test
    fun `back navigates up when player cannot minimize`() {
        assertEquals(
            VideoPlayerBackAction.NAVIGATE_UP,
            VideoPlayerBackBehavior.action(isFullScreen = false, canMinimizeInApp = false),
        )
    }
}
