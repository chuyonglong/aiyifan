package com.aiyifan.app.feature.video

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingWindowPositionPolicyTest {

    @Test
    fun `snap moves a window closer to the left edge onto the left edge`() {
        assertEquals(
            FloatingWindowPosition(x = 0, y = 180),
            FloatingWindowPositionPolicy.snapToNearestHorizontalEdge(
                x = 72,
                y = 180,
                windowWidth = 320,
                windowHeight = 180,
                displayWidth = 1080,
                displayHeight = 2400,
            ),
        )
    }

    @Test
    fun `snap moves a window closer to the right edge onto the right edge`() {
        assertEquals(
            FloatingWindowPosition(x = 760, y = 180),
            FloatingWindowPositionPolicy.snapToNearestHorizontalEdge(
                x = 600,
                y = 180,
                windowWidth = 320,
                windowHeight = 180,
                displayWidth = 1080,
                displayHeight = 2400,
            ),
        )
    }

    @Test
    fun `clamp keeps floating window inside available display bounds`() {
        assertEquals(
            FloatingWindowPosition(x = 0, y = 2220),
            FloatingWindowPositionPolicy.clampToDisplay(
                x = -24,
                y = 2300,
                windowWidth = 320,
                windowHeight = 180,
                displayWidth = 1080,
                displayHeight = 2400,
            ),
        )
    }
}
