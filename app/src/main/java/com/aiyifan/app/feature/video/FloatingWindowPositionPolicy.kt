package com.aiyifan.app.feature.video

data class FloatingWindowPosition(
    val x: Int,
    val y: Int,
)

object FloatingWindowPositionPolicy {
    fun snapToNearestHorizontalEdge(
        x: Int,
        y: Int,
        windowWidth: Int,
        windowHeight: Int,
        displayWidth: Int,
        displayHeight: Int,
    ): FloatingWindowPosition {
        val clamped = clampToDisplay(
            x = x,
            y = y,
            windowWidth = windowWidth,
            windowHeight = windowHeight,
            displayWidth = displayWidth,
            displayHeight = displayHeight,
        )
        val rightEdge = (displayWidth - windowWidth).coerceAtLeast(0)
        return clamped.copy(x = if (clamped.x <= rightEdge / 2) 0 else rightEdge)
    }

    fun clampToDisplay(
        x: Int,
        y: Int,
        windowWidth: Int,
        windowHeight: Int,
        displayWidth: Int,
        displayHeight: Int,
    ): FloatingWindowPosition = FloatingWindowPosition(
        x = x.coerceIn(0, (displayWidth - windowWidth).coerceAtLeast(0)),
        y = y.coerceIn(0, (displayHeight - windowHeight).coerceAtLeast(0)),
    )
}
